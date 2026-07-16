export type ApiErrorPayload = {
  errorCode?: string;
  message?: string;
  details?: Array<{ row?: number; column?: string; value?: string; message?: string }>;
};

export class ApiRequestError extends Error {
  readonly status: number;
  readonly payload?: ApiErrorPayload;

  constructor(status: number, payload?: ApiErrorPayload) {
    super(payload?.errorCode ?? `HTTP_${status}`);
    this.status = status;
    this.payload = payload;
  }
}

export type SessionState = {
  accessToken: string;
  csrfToken: string;
  profile: UserProfile;
};

export type UserProfile = {
  id: string;
  email: string;
  displayName: string;
  roles: string[];
};

const sessionKey = "werkpilot.session";

export function readSession(): SessionState | null {
  const raw = window.sessionStorage.getItem(sessionKey);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as SessionState;
  } catch {
    window.sessionStorage.removeItem(sessionKey);
    return null;
  }
}

export function writeSession(session: SessionState) {
  window.sessionStorage.setItem(sessionKey, JSON.stringify(session));
}

export function clearSession() {
  window.sessionStorage.removeItem(sessionKey);
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const session = readSession();
  const headers = new Headers(init.headers);

  if (!headers.has("Content-Type") && init.body && !(init.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  if (session?.accessToken) {
    headers.set("Authorization", `Bearer ${session.accessToken}`);
  }
  if (session?.csrfToken && ["POST", "PUT", "PATCH", "DELETE"].includes((init.method ?? "GET").toUpperCase())) {
    headers.set("X-WerkPilot-CSRF", session.csrfToken);
  }

  const response = await fetch(`/api/v1${path}`, {
    credentials: "include",
    ...init,
    headers,
  });

  if (!response.ok) {
    let payload: ApiErrorPayload | undefined;
    try {
      payload = (await response.json()) as ApiErrorPayload;
    } catch {
      payload = undefined;
    }
    throw new ApiRequestError(response.status, payload);
  }

  if (response.status === 204 || response.status === 202) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export function germanErrorMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    switch (error.payload?.errorCode) {
      case "AUTH_INVALID_CREDENTIALS":
        return "E-Mail oder Passwort ist ungültig.";
      case "AUTH_TOKEN_EXPIRED":
        return "Die Sitzung ist abgelaufen. Bitte melden Sie sich erneut an.";
      case "ACCESS_DENIED":
        return "Sie haben keine Berechtigung für diese Aktion.";
      case "VALIDATION_FAILED":
        return "Bitte prüfen Sie die Eingaben.";
      case "RESOURCE_NOT_FOUND":
        return "Der Datensatz wurde nicht gefunden.";
      case "IMPORT_DUPLICATE_FILE":
        return "Diese Datei wurde für diesen Importtyp bereits importiert.";
      case "IMPORT_JOB_NOT_ELIGIBLE":
        return "Nur ein festgeschriebener Import kann korrigiert oder zurückgerollt werden.";
      case "CSV_VALIDATION_FAILED":
        return "Die CSV-Datei konnte nicht validiert werden.";
      default:
        return "Die Anfrage konnte nicht verarbeitet werden.";
    }
  }
  return "Der Backend-Dienst ist nicht erreichbar.";
}
