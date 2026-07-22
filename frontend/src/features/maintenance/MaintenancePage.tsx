import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";

import { germanErrorMessage, readSession } from "../../shared/api/http";
import { formatDateTime } from "../dashboard/format";
import {
  addTicketComment,
  createTicket,
  fetchTicket,
  fetchTickets,
  updateTicketStatus,
  type MaintenanceTicket,
  type TicketDraft,
  type TicketFilters,
  type TicketPriority,
  type TicketStatus,
} from "./api";

const managerRoles = ["ADMIN", "PRODUCTION_MANAGER", "ENERGY_MANAGER"];
const statusLabels: Record<TicketStatus, string> = {
  OPEN: "Offen",
  IN_PROGRESS: "In Bearbeitung",
  RESOLVED: "Gelöst",
  CANCELLED: "Storniert",
};

const priorityLabels: Record<TicketPriority, string> = {
  LOW: "Niedrig",
  MEDIUM: "Mittel",
  HIGH: "Hoch",
  CRITICAL: "Kritisch",
};

export function MaintenancePage() {
  const queryClient = useQueryClient();
  const roles = readSession()?.profile.roles ?? [];
  const canCreate = roles.some((role) => managerRoles.includes(role));
  const [filters, setFilters] = useState<TicketFilters>({});
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const ticketsQuery = useQuery({ queryKey: ["maintenance-tickets", filters], queryFn: () => fetchTickets(filters) });
  const detailQuery = useQuery({ queryKey: ["maintenance-ticket", selectedId], queryFn: () => fetchTicket(selectedId!), enabled: selectedId !== null });
  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ["maintenance-tickets"] });
    void queryClient.invalidateQueries({ queryKey: ["maintenance-ticket"] });
  };

  return (
    <section className="page-stack" aria-labelledby="maintenance-title">
      <div className="page-heading">
        <h1 id="maintenance-title">Instandhaltung</h1>
        <p>Eigene zugewiesene offene Tickets erscheinen zuerst. Überfällig ist eine berechnete Kennzeichnung, kein Ticketstatus.</p>
      </div>
      <TicketFilter filters={filters} onApply={setFilters} />
      {canCreate ? <TicketCreateForm onCreated={invalidate} /> : null}
      {ticketsQuery.isError ? <p className="field-error" role="alert">{germanErrorMessage(ticketsQuery.error)}</p> : null}
      <section className="panel">
        <h2>Tickets</h2>
        {ticketsQuery.isPending ? <p role="status">Tickets werden geladen...</p> : null}
        {ticketsQuery.data ? (
          <>
            <table className="data-table">
              <thead><tr><th>Titel</th><th>Status</th><th>Priorität</th><th>Fällig</th><th>Maschine</th><th>Aktion</th></tr></thead>
              <tbody>
                {ticketsQuery.data.map((ticket) => (
                  <tr key={ticket.id}>
                    <th scope="row">{ticket.title}</th>
                    <td>{statusLabels[ticket.status]}</td>
                    <td>{priorityLabels[ticket.priority]}</td>
                    <td>{ticket.dueDate ?? "-"} {ticket.overdue ? <strong className="overdue-badge">Überfällig</strong> : null}</td>
                    <td>{ticket.machineId ?? "-"}</td>
                    <td><button className="secondary-button" onClick={() => setSelectedId(ticket.id)} type="button">Details</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
            {ticketsQuery.data.length === 0 ? <p role="status">Keine Tickets für die gewählten Filter.</p> : null}
          </>
        ) : null}
      </section>
      {selectedId ? <TicketDetailPanel detail={detailQuery.data} error={detailQuery.error} loading={detailQuery.isPending} onChanged={invalidate} /> : null}
    </section>
  );
}

function TicketFilter({ filters, onApply }: { filters: TicketFilters; onApply: (filters: TicketFilters) => void }) {
  const { handleSubmit, register } = useForm<TicketFilters>({ defaultValues: filters, values: filters });
  return <form className="filter-panel" onSubmit={handleSubmit(onApply)}>
    <label className="field compact-field"><span>Status</span><select {...register("ticketStatus")}><option value="">Alle</option>{Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
    <label className="field compact-field"><span>Priorität</span><select {...register("priority")}><option value="">Alle</option>{Object.entries(priorityLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
    <label className="field compact-field"><span>Maschinen-ID</span><input {...register("machineId")} /></label>
    <label className="field compact-field"><span>Zugewiesen an (ID)</span><input {...register("assigneeId")} /></label>
    <button className="primary-button filter-submit" type="submit">Filter anwenden</button>
  </form>;
}

function TicketCreateForm({ onCreated }: { onCreated: () => void }) {
  const { handleSubmit, register, reset } = useForm<TicketDraft>({ defaultValues: { priority: "MEDIUM" } });
  const mutation = useMutation({ mutationFn: createTicket, onSuccess: () => { reset({ priority: "MEDIUM" }); onCreated(); } });
  return <section className="panel"><h2>Neues Ticket</h2><form className="inline-form" onSubmit={handleSubmit((draft) => mutation.mutate(draft))}>
    <label className="field"><span>Titel</span><input {...register("title", { required: true })} /></label>
    <label className="field"><span>Beschreibung</span><textarea {...register("description")} /></label>
    <label className="field"><span>Fehlerkategorie</span><input {...register("issueCategory")} /></label>
    <label className="field"><span>Priorität</span><select {...register("priority")}><option value="LOW">Niedrig</option><option value="MEDIUM">Mittel</option><option value="HIGH">Hoch</option><option value="CRITICAL">Kritisch</option></select></label>
    <label className="field"><span>Maschinen-ID</span><input {...register("machineId")} /></label>
    <label className="field"><span>Zugewiesen an (ID)</span><input {...register("assigneeUserId")} /></label>
    <label className="field"><span>Fällig am</span><input type="date" {...register("dueDate")} /></label>
    <button className="primary-button" disabled={mutation.isPending} type="submit">Ticket erstellen</button>
    {mutation.isError ? <p className="field-error" role="alert">{germanErrorMessage(mutation.error)}</p> : null}
  </form></section>;
}

function TicketDetailPanel({ detail, error, loading, onChanged }: { detail?: Awaited<ReturnType<typeof fetchTicket>>; error: unknown; loading: boolean; onChanged: () => void }) {
  if (loading) return <p role="status">Ticketdetails werden geladen...</p>;
  if (error) return <p className="field-error" role="alert">{germanErrorMessage(error)}</p>;
  if (!detail) return null;
  return <section className="panel" aria-labelledby="ticket-detail-title">
    <h2 id="ticket-detail-title">{detail.ticket.title}</h2>
    <dl className="detail-list"><dt>Status</dt><dd>{statusLabels[detail.ticket.status]}</dd><dt>Priorität</dt><dd>{priorityLabels[detail.ticket.priority]}</dd><dt>Fehlerkategorie</dt><dd>{detail.ticket.issueCategory ?? "-"}</dd><dt>Anomalie</dt><dd>{detail.ticket.anomalyId ?? "-"}</dd><dt>Aktualisiert</dt><dd>{formatDateTime(detail.ticket.updatedAt)}</dd></dl>
    {detail.ticket.description ? <p>{detail.ticket.description}</p> : null}
    <TicketStatusForm ticket={detail.ticket} onChanged={onChanged} />
    <CommentForm ticketId={detail.ticket.id} onChanged={onChanged} />
    <h3>Kommentare</h3>
    {detail.comments.length === 0 ? <p>Noch keine Kommentare.</p> : <ul className="comment-list">{detail.comments.map((comment) => <li key={comment.id}><strong>{formatDateTime(comment.createdAt)}</strong><span>{comment.message}</span></li>)}</ul>}
  </section>;
}

function TicketStatusForm({ ticket, onChanged }: { ticket: MaintenanceTicket; onChanged: () => void }) {
  const allowed: Record<TicketStatus, TicketStatus[]> = { OPEN: ["IN_PROGRESS", "CANCELLED"], IN_PROGRESS: ["RESOLVED", "CANCELLED"], RESOLVED: [], CANCELLED: [] };
  const { handleSubmit, register, watch } = useForm<{ status: TicketStatus; note: string }>({ defaultValues: { status: allowed[ticket.status][0], note: "" } });
  const mutation = useMutation({ mutationFn: (values: { status: TicketStatus; note: string }) => updateTicketStatus(ticket.id, values.status, values.note), onSuccess: onChanged });
  if (allowed[ticket.status].length === 0) return <p className="info-panel">Dieser Ticketstatus ist abgeschlossen und nicht mehr veränderbar.</p>;
  const nextStatus = watch("status");
  return <form className="inline-form" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
    <h3>Status ändern</h3>
    <label className="field"><span>Neuer Status</span><select {...register("status")}>{allowed[ticket.status].map((status) => <option key={status} value={status}>{statusLabels[status]}</option>)}</select></label>
    <label className="field"><span>{nextStatus === "RESOLVED" ? "Lösungsnotiz" : nextStatus === "CANCELLED" ? "Stornierungsgrund" : "Notiz (optional)"}</span><textarea {...register("note", { required: nextStatus === "RESOLVED" || nextStatus === "CANCELLED" })} /></label>
    <button className="primary-button" disabled={mutation.isPending} type="submit">Status speichern</button>
    {mutation.isError ? <p className="field-error" role="alert">{germanErrorMessage(mutation.error)}</p> : null}
  </form>;
}

function CommentForm({ ticketId, onChanged }: { ticketId: string; onChanged: () => void }) {
  const { handleSubmit, register, reset } = useForm<{ message: string }>();
  const mutation = useMutation({ mutationFn: ({ message }: { message: string }) => addTicketComment(ticketId, message), onSuccess: () => { reset(); onChanged(); } });
  return <form className="inline-form" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
    <h3>Kommentar hinzufügen</h3>
    <label className="field"><span>Kommentar</span><textarea {...register("message", { required: true })} /></label>
    <button className="secondary-button" disabled={mutation.isPending} type="submit">Kommentar speichern</button>
    {mutation.isError ? <p className="field-error" role="alert">{germanErrorMessage(mutation.error)}</p> : null}
  </form>;
}
