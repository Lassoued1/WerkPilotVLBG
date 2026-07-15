import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";

import { apiRequest, germanErrorMessage, readSession } from "../../shared/api/http";

type Role = "ADMIN" | "PRODUCTION_MANAGER" | "MAINTENANCE_TECHNICIAN" | "ENERGY_MANAGER" | "VIEWER";

type UserRow = {
  id: string;
  email: string;
  displayName: string;
  active: boolean;
  roles: Role[];
};

type PageResponse<T> = { items: T[]; page: number; size: number; totalElements: number; totalPages: number };

type CreateUserForm = {
  email: string;
  displayName: string;
  temporaryPassword: string;
  role: Role;
};

const roles: Role[] = ["ADMIN", "PRODUCTION_MANAGER", "MAINTENANCE_TECHNICIAN", "ENERGY_MANAGER", "VIEWER"];

const fallbackUsers: UserRow[] = [
  { id: "demo-admin", email: "admin@werkpilot.local", displayName: "Demo Admin", active: true, roles: ["ADMIN"] },
];

async function fetchUsers() {
  try {
    return await apiRequest<PageResponse<UserRow>>("/users?page=0&size=20");
  } catch {
    return { items: fallbackUsers, page: 0, size: 20, totalElements: fallbackUsers.length, totalPages: 1 } satisfies PageResponse<UserRow>;
  }
}

async function createUser(values: CreateUserForm) {
  return apiRequest<UserRow>("/users", {
    method: "POST",
    body: JSON.stringify({
      email: values.email,
      displayName: values.displayName,
      temporaryPassword: values.temporaryPassword,
      roles: [values.role],
    }),
  });
}

async function triggerPasswordReset(id: string) {
  await apiRequest<void>(`/users/${id}/password-reset`, { method: "POST" });
}

async function updateDelegation(enabled: boolean) {
  return apiRequest<{ energyThresholdDelegationEnabled: boolean }>("/settings/global/energy-threshold-delegation", {
    method: "PUT",
    body: JSON.stringify({ enabled }),
  });
}

function isAdmin() {
  return readSession()?.profile.roles.includes("ADMIN") ?? false;
}

export function AdminPage() {
  const queryClient = useQueryClient();
  const admin = isAdmin();
  const usersQuery = useQuery({ queryKey: ["users"], queryFn: fetchUsers });
  const [delegationEnabled, setDelegationEnabled] = useState(false);
  const { formState, handleSubmit, register, reset } = useForm<CreateUserForm>({ defaultValues: { role: "VIEWER" } });
  const createMutation = useMutation({
    mutationFn: createUser,
    onSuccess: () => {
      reset({ email: "", displayName: "", temporaryPassword: "", role: "VIEWER" });
      void queryClient.invalidateQueries({ queryKey: ["users"] });
    },
  });
  const resetMutation = useMutation({ mutationFn: triggerPasswordReset });
  const settingsMutation = useMutation({
    mutationFn: updateDelegation,
    onSuccess: (settings) => setDelegationEnabled(settings.energyThresholdDelegationEnabled),
  });
  const createError = useMemo(() => createMutation.error ? germanErrorMessage(createMutation.error) : null, [createMutation.error]);

  return (
    <section className="page-stack" aria-labelledby="admin-title">
      <div className="page-heading">
        <h1 id="admin-title">Administration</h1>
        <p>Benutzerverwaltung, Rollen und globale Energie-Schwellenwert-Delegation für Sprint 1.</p>
      </div>

      {!admin ? (
        <div className="permission-banner" role="note">Schreibaktionen sind ADMIN vorbehalten. Die Oberfläche zeigt Berechtigungszustände explizit an.</div>
      ) : null}

      <div className="two-column-grid">
        <form className="form-panel" onSubmit={handleSubmit((values) => createMutation.mutate(values))}>
          <h2>Benutzer anlegen</h2>
          <label className="field">E-Mail
            <input type="email" {...register("email", { required: "E-Mail ist erforderlich." })} />
          </label>
          {formState.errors.email ? <p className="field-error" role="alert">{formState.errors.email.message}</p> : null}
          <label className="field">Anzeigename
            <input {...register("displayName", { required: "Anzeigename ist erforderlich." })} />
          </label>
          <label className="field">Temporäres Passwort
            <input type="password" {...register("temporaryPassword", { minLength: { value: 12, message: "Mindestens 12 Zeichen." }, required: "Temporäres Passwort ist erforderlich." })} />
          </label>
          {formState.errors.temporaryPassword ? <p className="field-error" role="alert">{formState.errors.temporaryPassword.message}</p> : null}
          <label className="field">Rolle
            <select {...register("role")}>{roles.map((role) => <option key={role} value={role}>{role}</option>)}</select>
          </label>
          {createError ? <p className="field-error" role="alert">{createError}</p> : null}
          {createMutation.isSuccess ? <p className="success-message">Benutzer wurde angelegt.</p> : null}
          <button className="primary-button" disabled={!admin || createMutation.isPending} type="submit">Benutzer speichern</button>
        </form>

        <div className="form-panel">
          <h2>Globale Einstellungen</h2>
          <p className="form-help">ENERGY_MANAGER darf Schwellenwerte nur bearbeiten, wenn diese Delegation aktiv ist.</p>
          <label className="checkbox-field">
            <input checked={delegationEnabled} onChange={(event) => {
              setDelegationEnabled(event.target.checked);
              settingsMutation.mutate(event.target.checked);
            }} disabled={!admin || settingsMutation.isPending} type="checkbox" />
            Energie-Schwellenwert-Delegation aktivieren
          </label>
          {settingsMutation.isError ? <p className="field-error" role="alert">{germanErrorMessage(settingsMutation.error)}</p> : null}
        </div>
      </div>

      <section className="panel" aria-labelledby="users-title">
        <div className="section-heading"><h2 id="users-title">Benutzer</h2></div>
        <table className="data-table">
          <thead><tr><th>Name</th><th>E-Mail</th><th>Rollen</th><th>Status</th><th>Aktionen</th></tr></thead>
          <tbody>
            {(usersQuery.data?.items ?? []).map((user) => (
              <tr key={user.id}>
                <th scope="row">{user.displayName}</th>
                <td>{user.email}</td>
                <td>{user.roles.join(", ")}</td>
                <td>{user.active ? "Aktiv" : "Inaktiv"}</td>
                <td><button className="secondary-button" disabled={!admin || resetMutation.isPending} onClick={() => resetMutation.mutate(user.id)} type="button">Reset senden</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </section>
  );
}
