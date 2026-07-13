import createClient from "openapi-fetch";

export type ApiPaths = Record<string, never>;

export const apiBaseUrl = "/api";

export const apiClient = createClient<ApiPaths>({
  baseUrl: apiBaseUrl,
});
