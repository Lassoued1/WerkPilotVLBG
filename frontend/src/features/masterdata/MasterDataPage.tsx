import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";

import { apiRequest, germanErrorMessage, readSession } from "../../shared/api/http";

type MasterDataKind = "factories" | "production-lines" | "machines" | "products" | "shifts" | "downtime-reasons" | "scrap-categories";

type MasterDataRecord = {
  id: string;
  code: string;
  name: string;
  active: boolean;
  family?: string | null;
  startTime?: string;
  endTime?: string;
  plannedMinutes?: number;
};

type PageResponse<T> = { items: T[]; page: number; size: number; totalElements: number; totalPages: number };

type SimpleForm = { code: string; name: string; family?: string; startTime?: string; endTime?: string; plannedMinutes?: number };

const resources: Array<{ kind: MasterDataKind; label: string; helper: string }> = [
  { kind: "factories", label: "Werke", helper: "Factory-Stammdaten" },
  { kind: "production-lines", label: "Linien", helper: "Produktionslinien" },
  { kind: "machines", label: "Maschinen", helper: "Maschinen je Linie" },
  { kind: "products", label: "Produkte", helper: "Produkte und Familien" },
  { kind: "shifts", label: "Schichten", helper: "Schichtzeiten und Planminuten" },
  { kind: "downtime-reasons", label: "Stillstandsgründe", helper: "Gründe für Downtime" },
  { kind: "scrap-categories", label: "Ausschusskategorien", helper: "Qualitätskategorien" },
];

const fallbackRecords: MasterDataRecord[] = [
  { id: "demo-factory", code: "VLBG", name: "Werk Vorarlberg", active: true },
];

function canWrite() {
  return readSession()?.profile.roles.includes("ADMIN") ?? false;
}

async function fetchRecords(kind: MasterDataKind) {
  try {
    return await apiRequest<PageResponse<MasterDataRecord>>(`/${kind}?page=0&size=20&includeInactive=true`);
  } catch {
    return { items: fallbackRecords, page: 0, size: 20, totalElements: fallbackRecords.length, totalPages: 1 } satisfies PageResponse<MasterDataRecord>;
  }
}

function payloadFor(kind: MasterDataKind, values: SimpleForm) {
  if (kind === "products") return { code: values.code, name: values.name, family: values.family ?? null };
  if (kind === "shifts") return { code: values.code, name: values.name, startTime: values.startTime ?? "06:00", endTime: values.endTime ?? "14:00", plannedMinutes: Number(values.plannedMinutes ?? 480) };
  return { code: values.code, name: values.name };
}

export function MasterDataPage() {
  const queryClient = useQueryClient();
  const writeAllowed = canWrite();

  return (
    <section className="page-stack" aria-labelledby="masterdata-title">
      <div className="page-heading">
        <h1 id="masterdata-title">Stammdaten</h1>
        <p>CRUD-Masken für Sprint 1. Backend-Regeln, Soft-Delete und Eindeutigkeit bleiben serverseitig maßgeblich.</p>
      </div>
      {!writeAllowed ? <div className="permission-banner" role="note">Lesemodus: Nur ADMIN darf Stammdaten anlegen oder ändern.</div> : null}
      <div className="resource-grid">
        {resources.map((resource) => (
          <MasterDataCard key={resource.kind} queryClient={queryClient} resource={resource} writeAllowed={writeAllowed} />
        ))}
      </div>
    </section>
  );
}

function MasterDataCard({ queryClient, resource, writeAllowed }: { queryClient: ReturnType<typeof useQueryClient>; resource: typeof resources[number]; writeAllowed: boolean }) {
  const query = useQuery({ queryKey: ["master-data", resource.kind], queryFn: () => fetchRecords(resource.kind) });
  const { formState, handleSubmit, register, reset } = useForm<SimpleForm>();
  const mutation = useMutation({
    mutationFn: (values: SimpleForm) => apiRequest<MasterDataRecord>(`/${resource.kind}`, { method: "POST", body: JSON.stringify(payloadFor(resource.kind, values)) }),
    onSuccess: () => {
      reset();
      void queryClient.invalidateQueries({ queryKey: ["master-data", resource.kind] });
    },
  });

  return (
    <article className="panel resource-card">
      <div className="section-heading">
        <h2>{resource.label}</h2>
        <p>{resource.helper}</p>
      </div>
      <form className="inline-form" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
        <label className="field compact-field">Code
          <input {...register("code", { required: "Code ist erforderlich." })} />
        </label>
        <label className="field compact-field">Name
          <input {...register("name", { required: "Name ist erforderlich." })} />
        </label>
        {resource.kind === "products" ? <label className="field compact-field">Familie<input {...register("family")} /></label> : null}
        {resource.kind === "shifts" ? (
          <>
            <label className="field compact-field">Start<input type="time" {...register("startTime")} /></label>
            <label className="field compact-field">Ende<input type="time" {...register("endTime")} /></label>
            <label className="field compact-field">Planminuten<input type="number" {...register("plannedMinutes", { valueAsNumber: true })} /></label>
          </>
        ) : null}
        {(formState.errors.code || formState.errors.name) ? <p className="field-error" role="alert">Bitte Code und Name ausfüllen.</p> : null}
        {mutation.isError ? <p className="field-error" role="alert">{germanErrorMessage(mutation.error)}</p> : null}
        {mutation.isSuccess ? <p className="success-message">Datensatz wurde angelegt.</p> : null}
        <button className="primary-button" disabled={!writeAllowed || mutation.isPending} type="submit">Anlegen</button>
      </form>
      <table className="data-table compact-table">
        <thead><tr><th>Code</th><th>Name</th><th>Status</th></tr></thead>
        <tbody>
          {(query.data?.items ?? []).map((record) => (
            <tr key={record.id}><th scope="row">{record.code}</th><td>{record.name}</td><td>{record.active ? "Aktiv" : "Inaktiv"}</td></tr>
          ))}
        </tbody>
      </table>
    </article>
  );
}
