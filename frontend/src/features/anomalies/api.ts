import { apiRequest } from "../../shared/api/http";
import { kpiQueryString, type KpiFilters, type PageResponse } from "../dashboard/api";

export type AnomalyStatus = "NEW" | "ACKNOWLEDGED" | "LINKED_TO_TICKET" | "DISMISSED" | "SUPERSEDED";

export type Anomaly = {
  id: string;
  metricKey: string;
  anomalyType: string;
  severity: string;
  status: AnomalyStatus;
  detectionMethod: string;
  factoryId: string | null;
  lineId: string | null;
  machineId: string | null;
  productId: string | null;
  shiftId: string | null;
  periodStart: string;
  periodEnd: string;
  observedValue: number;
  baselineAverage: number | null;
  baselineStddev: number | null;
  baselineCount: number;
  baselineQuality: string;
  zScore: number | null;
  thresholdRuleId: string | null;
  explanation: string;
  previousAnomalyId: string | null;
  supersededByAnomalyId: string | null;
  createdAt: string;
  updatedAt: string;
};

export type Recommendation = { id: string; templateCode: string; templateVersion: string; messageDe: string; disclaimerDe: string };
export type LinkedTicket = { id: string; title: string; status: string; priority: string };
export type AnomalyDetail = { anomaly: Anomaly; recommendations: Recommendation[] };
export type AnomalyFilters = KpiFilters & { anomalyType?: string; severity?: string; anomalyStatus?: string; includeSuperseded?: boolean };

export function defaultAnomalyFilters(): AnomalyFilters {
  return { from: "2026-07-01T00:00:00Z", to: "2026-07-02T00:00:00Z" };
}

export function fetchAnomalies(filters: AnomalyFilters, page = 0) {
  return apiRequest<PageResponse<Anomaly>>(`/anomalies?${kpiQueryString(filters, { page, size: 20 })}`);
}

export function fetchAnomaly(id: string) {
  return apiRequest<AnomalyDetail>(`/anomalies/${id}`);
}

export function updateAnomalyStatus(id: string, status: AnomalyStatus) {
  return apiRequest<Anomaly>(`/anomalies/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) });
}

export function rerunAnomalies(filters: KpiFilters) {
  return apiRequest<{ created: number; superseded: number; unchanged: number; detected: number }>("/anomalies/rerun", {
    method: "POST",
    body: JSON.stringify(filters),
  });
}

export function createTicketFromAnomaly(id: string, ticket: { title: string; issueCategory?: string; priority?: string; dueDate?: string }) {
  return apiRequest<LinkedTicket>(`/anomalies/${id}/tickets`, { method: "POST", body: JSON.stringify(ticket) });
}

export function fetchLinkedTicket(id: string) {
  return apiRequest<LinkedTicket>(`/anomalies/${id}/ticket`);
}
