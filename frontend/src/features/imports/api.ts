import { apiRequest } from "../../shared/api/http";

export type ImportTypeKey = "production" | "energy" | "downtime" | "scrap";

export type JobStatus = "PROCESSING" | "COMMITTED" | "FAILED" | "SUPERSEDED";

export type JobResponse = {
  jobId: string;
  status: JobStatus;
  createdAt: string;
  completedAt: string | null;
};

export type ImportJobListItem = {
  id: string;
  importType: string;
  status: JobStatus;
  originalFilename: string;
  safeFilename: string;
  fileHashSha256: string;
  fileSizeBytes: number;
  totalRows: number;
  validRows: number;
  errorCount: number;
  errorOverflow: boolean;
  correctsImportJobId: string | null;
  createdByUserId: string;
  createdAt: string;
  completedAt: string | null;
  failureReason: string | null;
};

export type ImportJobError = {
  id: string;
  importJobId: string;
  rowNumber: number;
  columnName: string;
  rejectedValue: string | null;
  message: string;
  createdAt: string;
};

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export const importEndpoints: Record<ImportTypeKey, string> = {
  production: "/import-jobs/production-records",
  energy: "/import-jobs/energy-measurements",
  downtime: "/import-jobs/downtime-records",
  scrap: "/import-jobs/scrap-records",
};

export const importTypesForRoles = (roles: string[]): ImportTypeKey[] => {
  if (roles.includes("ADMIN")) return ["production", "energy", "downtime", "scrap"];
  const allowed: ImportTypeKey[] = [];
  if (roles.includes("PRODUCTION_MANAGER")) allowed.push("production", "downtime", "scrap");
  if (roles.includes("ENERGY_MANAGER")) allowed.push("energy");
  return allowed;
};

function csvFormData(file: File): FormData {
  const formData = new FormData();
  formData.append("file", file);
  return formData;
}

export function startImport(type: ImportTypeKey, file: File) {
  return apiRequest<JobResponse>(importEndpoints[type], { method: "POST", body: csvFormData(file) });
}

export function fetchJobs(page: number, size = 20) {
  return apiRequest<PageResponse<ImportJobListItem>>(`/import-jobs?page=${page}&size=${size}`);
}

export function fetchErrors(jobId: string, page = 0, size = 50) {
  return apiRequest<PageResponse<ImportJobError>>(`/import-jobs/${jobId}/errors?page=${page}&size=${size}`);
}

export function correctJob(jobId: string, file: File) {
  return apiRequest<JobResponse>(`/import-jobs/${jobId}/correction`, { method: "POST", body: csvFormData(file) });
}

export function rollbackJob(jobId: string, reason: string) {
  return apiRequest<JobResponse>(`/import-jobs/${jobId}/rollback`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}
