import { useMemo, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";

import { germanErrorMessage, readSession } from "../../shared/api/http";
import { confirmPasswordReset, login, requestPasswordReset } from "./api";

type LoginForm = { email: string; password: string };
type ResetRequestForm = { email: string };
type ResetConfirmForm = { token: string; newPassword: string };

export function AuthPage() {
  const [mode, setMode] = useState<"login" | "request" | "confirm">("login");
  const session = readSession();

  return (
    <section className="page-stack" aria-labelledby="auth-title">
      <div className="page-heading">
        <h1 id="auth-title">Anmeldung und Passwort</h1>
        <p>Login, Passwort-Reset und Sitzungsdaten für die Sprint-1-Identity-Strecke.</p>
      </div>

      <div className="tab-row" role="tablist" aria-label="Identity Aktionen">
        <button className={mode === "login" ? "tab-button tab-active" : "tab-button"} onClick={() => setMode("login")} type="button">Anmelden</button>
        <button className={mode === "request" ? "tab-button tab-active" : "tab-button"} onClick={() => setMode("request")} type="button">Reset anfordern</button>
        <button className={mode === "confirm" ? "tab-button tab-active" : "tab-button"} onClick={() => setMode("confirm")} type="button">Reset bestätigen</button>
      </div>

      {session ? (
        <div className="info-panel" role="status">
          Angemeldet als {session.profile.displayName} ({session.profile.email}) mit Rollen: {session.profile.roles.join(", ")}
        </div>
      ) : null}

      {mode === "login" ? <LoginPanel /> : null}
      {mode === "request" ? <PasswordResetRequestPanel /> : null}
      {mode === "confirm" ? <PasswordResetConfirmPanel /> : null}
    </section>
  );
}

function LoginPanel() {
  const { formState, handleSubmit, register } = useForm<LoginForm>();
  const [message, setMessage] = useState<string | null>(null);
  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (session) => setMessage(`Anmeldung erfolgreich: ${session.profile.displayName}`),
  });
  const errorMessage = useMemo(() => mutation.error ? germanErrorMessage(mutation.error) : null, [mutation.error]);

  return (
    <form className="form-panel" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
      <h2>Anmelden</h2>
      <label className="field">E-Mail
        <input autoComplete="email" type="email" {...register("email", { required: "Bitte geben Sie eine E-Mail ein." })} />
      </label>
      {formState.errors.email ? <p className="field-error" role="alert">{formState.errors.email.message}</p> : null}
      <label className="field">Passwort
        <input autoComplete="current-password" type="password" {...register("password", { required: "Bitte geben Sie ein Passwort ein." })} />
      </label>
      {formState.errors.password ? <p className="field-error" role="alert">{formState.errors.password.message}</p> : null}
      {errorMessage ? <p className="field-error" role="alert">{errorMessage}</p> : null}
      {message ? <p className="success-message">{message}</p> : null}
      <button className="primary-button" disabled={mutation.isPending} type="submit">Anmelden</button>
    </form>
  );
}

function PasswordResetRequestPanel() {
  const { formState, handleSubmit, register } = useForm<ResetRequestForm>();
  const mutation = useMutation({ mutationFn: (values: ResetRequestForm) => requestPasswordReset(values.email) });

  return (
    <form className="form-panel" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
      <h2>Passwort-Reset anfordern</h2>
      <p className="form-help">Die Antwort bleibt absichtlich neutral, damit keine Konten erraten werden können.</p>
      <label className="field">E-Mail
        <input type="email" {...register("email", { required: "Bitte geben Sie eine E-Mail ein." })} />
      </label>
      {formState.errors.email ? <p className="field-error" role="alert">{formState.errors.email.message}</p> : null}
      {mutation.isError ? <p className="field-error" role="alert">{germanErrorMessage(mutation.error)}</p> : null}
      {mutation.isSuccess ? <p className="success-message">Wenn ein aktives Konto existiert, wurde eine E-Mail versendet.</p> : null}
      <button className="primary-button" disabled={mutation.isPending} type="submit">Reset-E-Mail senden</button>
    </form>
  );
}

function PasswordResetConfirmPanel() {
  const { formState, handleSubmit, register } = useForm<ResetConfirmForm>();
  const mutation = useMutation({ mutationFn: (values: ResetConfirmForm) => confirmPasswordReset(values.token, values.newPassword) });

  return (
    <form className="form-panel" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
      <h2>Neues Passwort setzen</h2>
      <label className="field">Reset-Token
        <input {...register("token", { required: "Bitte geben Sie den Reset-Token ein." })} />
      </label>
      {formState.errors.token ? <p className="field-error" role="alert">{formState.errors.token.message}</p> : null}
      <label className="field">Neues Passwort
        <input type="password" {...register("newPassword", { minLength: { value: 12, message: "Das Passwort muss mindestens 12 Zeichen haben." }, required: "Bitte geben Sie ein neues Passwort ein." })} />
      </label>
      {formState.errors.newPassword ? <p className="field-error" role="alert">{formState.errors.newPassword.message}</p> : null}
      {mutation.isError ? <p className="field-error" role="alert">{germanErrorMessage(mutation.error)}</p> : null}
      {mutation.isSuccess ? <p className="success-message">Das Passwort wurde geändert. Bitte melden Sie sich neu an.</p> : null}
      <button className="primary-button" disabled={mutation.isPending} type="submit">Passwort ändern</button>
    </form>
  );
}
