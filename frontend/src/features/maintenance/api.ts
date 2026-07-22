import { apiRequest } from "../../shared/api/http";

export type TicketStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CANCELLED";
export type TicketPriority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type MaintenanceTicket = {
  id: string;
  title: string;
  description: string | null;
  issueCategory: string | null;
  status: TicketStatus;
  priority: TicketPriority;
  factoryId: string | null;
  lineId: string | null;
  machineId: string | null;
  anomalyId: string | null;
  assigneeUserId: string | null;
  dueDate: string | null;
  resolutionNote: string | null;
  cancellationReason: string | null;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
  overdue: boolean;
};

export type TicketComment = {
  id: string;
  ticketId: string;
  authorUserId: string;
  message: string;
  createdAt: string;
};

export type TicketDetail = { ticket: MaintenanceTicket; comments: TicketComment[] };
export type TicketFilters = { ticketStatus?: string; priority?: string; assigneeId?: string; machineId?: string };
export type TicketDraft = {
  title: string;
  description?: string;
  issueCategory?: string;
  priority: TicketPriority;
  factoryId?: string;
  lineId?: string;
  machineId?: string;
  assigneeUserId?: string;
  dueDate?: string;
};

function queryString(filters: TicketFilters) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value) params.set(key, value);
  });
  return params.toString();
}

export function fetchTickets(filters: TicketFilters = {}) {
  const query = queryString(filters);
  return apiRequest<MaintenanceTicket[]>(`/maintenance-tickets${query ? `?${query}` : ""}`);
}

export function fetchTicket(id: string) {
  return apiRequest<TicketDetail>(`/maintenance-tickets/${id}`);
}

export function createTicket(draft: TicketDraft) {
  return apiRequest<MaintenanceTicket>("/maintenance-tickets", { method: "POST", body: JSON.stringify(draft) });
}

export function updateTicketStatus(id: string, status: TicketStatus, note?: string) {
  return apiRequest<MaintenanceTicket>(`/maintenance-tickets/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status, note: note || null }),
  });
}

export function addTicketComment(id: string, message: string) {
  return apiRequest<TicketComment>(`/maintenance-tickets/${id}/comments`, {
    method: "POST",
    body: JSON.stringify({ message }),
  });
}
