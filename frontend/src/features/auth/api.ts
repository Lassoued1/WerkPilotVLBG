import { apiRequest, clearSession, writeSession, type SessionState, type UserProfile } from "../../shared/api/http";

export type LoginPayload = {
  email: string;
  password: string;
};

export type LoginResponse = {
  accessToken: string;
  csrfToken: string;
  profile: UserProfile;
};

export async function login(payload: LoginPayload): Promise<SessionState> {
  const response = await apiRequest<LoginResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  const session = {
    accessToken: response.accessToken,
    csrfToken: response.csrfToken,
    profile: response.profile,
  };
  writeSession(session);
  return session;
}

export async function requestPasswordReset(email: string) {
  await apiRequest<void>("/auth/password-reset-request", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

export async function confirmPasswordReset(token: string, newPassword: string) {
  await apiRequest<void>("/auth/password-reset-confirm", {
    method: "POST",
    body: JSON.stringify({ token, newPassword }),
  });
}

export async function logout() {
  try {
    await apiRequest<void>("/auth/logout", { method: "POST" });
  } finally {
    clearSession();
  }
}
