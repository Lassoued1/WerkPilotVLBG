import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";

import { germanErrorMessage, readSession } from "../../shared/api/http";
import { formatDateTime, formatDecimal } from "../dashboard/format";
import {
  createTicketFromAnomaly,
  defaultAnomalyFilters,
  fetchAnomalies,
  fetchAnomaly,
  fetchLinkedTicket,
  rerunAnomalies,
  updateAnomalyStatus,
  type Anomaly,
  type AnomalyFilters,
  type AnomalyStatus,
} from "./api";

const managerRoles = ["ADMIN", "PRODUCTION_MANAGER", "ENERGY_MANAGER"];
const statusLabels: Record<AnomalyStatus, string> = {
  NEW: "Neu",
  ACKNOWLEDGED: "Bestätigt",
  LINKED_TO_TICKET: "Mit Ticket verknüpft",
  DISMISSED: "Verworfen",
  SUPERSEDED: "Ersetzt",
};

export function AnomaliesPage() {
  const [filters, setFilters] = useState<AnomalyFilters>(defaultAnomalyFilters);
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const roles = readSession()?.profile.roles ?? [];
  const canManage = roles.some((role) => managerRoles.includes(role));
  const isAdmin = roles.includes("ADMIN");
  const queryClient = useQueryClient();
  const listQuery = useQuery({ queryKey: ["anomalies", filters, page], queryFn: () => fetchAnomalies(filters, page) });
  const detailQuery = useQuery({ queryKey: ["anomaly", selectedId], queryFn: () => fetchAnomaly(selectedId!), enabled: selectedId !== null });
  const linkedTicketQuery = useQuery({ queryKey: ["anomaly-ticket", selectedId], queryFn: () => fetchLinkedTicket(selectedId!), enabled: selectedId !== null });
  const invalidateAnomalies = () => {
    void queryClient.invalidateQueries({ queryKey: ["anomalies"] });
    void queryClient.invalidateQueries({ queryKey: ["anomaly"] });
    void queryClient.invalidateQueries({ queryKey: ["anomaly-ticket"] });
  };
  const statusMutation = useMutation({ mutationFn: ({ id, status }: { id: string; status: AnomalyStatus }) => updateAnomalyStatus(id, status), onSuccess: invalidateAnomalies });
  const rerunMutation = useMutation({ mutationFn: () => rerunAnomalies(filters), onSuccess: invalidateAnomalies });
  const ticketMutation = useMutation({ mutationFn: ({ id, title, issueCategory, priority }: { id: string; title: string; issueCategory?: string; priority?: string }) => createTicketFromAnomaly(id, { title, issueCategory, priority }), onSuccess: invalidateAnomalies });

  return <section className="page-stack" aria-labelledby="anomalies-title">
    <div className="page-heading"><h1 id="anomalies-title">Anomalien</h1><p>Erkannte Abweichungen, nachvollziehbare Ursachen und Empfehlungen aus dem Backend.</p></div>
    <AnomalyFilter filters={filters} onApply={(next) => { setPage(0); setFilters(next); }} />
    {isAdmin ? <button className="secondary-button" disabled={rerunMutation.isPending} onClick={() => rerunMutation.mutate()} type="button">Analyse erneut ausführen</button> : null}
    {rerunMutation.isSuccess ? <p className="success-message" role="status">Analyse abgeschlossen: {rerunMutation.data.detected} erkannt, {rerunMutation.data.created} neu.</p> : null}
    {listQuery.isError ? <p className="field-error" role="alert">{germanErrorMessage(listQuery.error)}</p> : null}
    <section className="panel"><h2>Erkannte Anomalien</h2>{listQuery.isPending ? <p role="status">Anomalien werden geladen...</p> : null}
      {listQuery.data ? <><table className="data-table"><thead><tr><th>Zeitraum</th><th>Typ</th><th>Schweregrad</th><th>Status</th><th>Maschine</th><th>Aktion</th></tr></thead><tbody>{listQuery.data.items.map((anomaly) => <AnomalyRow anomaly={anomaly} canManage={canManage} key={anomaly.id} onDetails={() => setSelectedId(anomaly.id)} onStatus={(status) => statusMutation.mutate({ id: anomaly.id, status })} />)}</tbody></table>{listQuery.data.items.length === 0 ? <p role="status">Keine Anomalien für den gewählten Zeitraum.</p> : null}<div className="pagination-controls"><button className="secondary-button" disabled={page === 0} onClick={() => setPage((value) => value - 1)} type="button">Zurück</button><button className="secondary-button" disabled={page >= listQuery.data.totalPages - 1} onClick={() => setPage((value) => value + 1)} type="button">Weiter</button></div></> : null}
    </section>
    {selectedId ? <AnomalyDetails canManage={canManage} detail={detailQuery.data} error={detailQuery.error} isLoading={detailQuery.isPending} linkedTicket={linkedTicketQuery.data} onCreateTicket={(values) => ticketMutation.mutate({ id: selectedId, ...values })} ticketError={ticketMutation.error} ticketPending={ticketMutation.isPending} /> : null}
  </section>;
}

function AnomalyFilter({ filters, onApply }: { filters: AnomalyFilters; onApply: (filters: AnomalyFilters) => void }) {
  const { handleSubmit, register } = useForm<AnomalyFilters>({ defaultValues: filters, values: filters });
  return <form className="filter-panel" onSubmit={handleSubmit(onApply)}><label className="field compact-field"><span>Von</span><input {...register("from", { required: true })} /></label><label className="field compact-field"><span>Bis</span><input {...register("to", { required: true })} /></label><label className="field compact-field"><span>Schweregrad</span><select {...register("severity")}><option value="">Alle</option><option value="WARNING">Warnung</option><option value="CRITICAL">Kritisch</option></select></label><label className="field compact-field"><span>Status</span><select {...register("anomalyStatus")}><option value="">Alle</option>{Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label><label className="field compact-field"><span>Maschinen-ID</span><input {...register("machineId")} /></label><label className="field compact-field"><span>Anomalietyp</span><input {...register("anomalyType")} /></label><label className="field compact-field"><span><input type="checkbox" {...register("includeSuperseded")} /> Ersetzte anzeigen</span></label><button className="primary-button filter-submit" type="submit">Filter anwenden</button></form>;
}

function AnomalyRow({ anomaly, canManage, onDetails, onStatus }: { anomaly: Anomaly; canManage: boolean; onDetails: () => void; onStatus: (status: AnomalyStatus) => void }) {
  return <tr><th scope="row">{formatDateTime(anomaly.periodStart)}</th><td>{anomaly.anomalyType}</td><td>{anomaly.severity === "CRITICAL" ? "Kritisch" : "Warnung"}</td><td>{statusLabels[anomaly.status]}</td><td>{anomaly.machineId ?? anomaly.lineId ?? "-"}</td><td className="action-cell"><button className="secondary-button" onClick={onDetails} type="button">Details</button>{canManage && anomaly.status !== "SUPERSEDED" && anomaly.status !== "LINKED_TO_TICKET" ? <select aria-label={`Status für ${anomaly.id}`} defaultValue={anomaly.status} onChange={(event) => onStatus(event.target.value as AnomalyStatus)}><option value={anomaly.status}>{statusLabels[anomaly.status]}</option>{(["ACKNOWLEDGED", "DISMISSED"] as AnomalyStatus[]).filter((status) => status !== anomaly.status).map((status) => <option key={status} value={status}>{statusLabels[status]}</option>)}</select> : null}</td></tr>;
}

function AnomalyDetails({ canManage, detail, error, isLoading, linkedTicket, onCreateTicket, ticketError, ticketPending }: { canManage: boolean; detail?: Awaited<ReturnType<typeof fetchAnomaly>>; error: unknown; isLoading: boolean; linkedTicket?: Awaited<ReturnType<typeof fetchLinkedTicket>>; onCreateTicket: (values: { title: string; issueCategory?: string; priority?: string }) => void; ticketError: unknown; ticketPending: boolean }) {
  if (isLoading) return <p role="status">Anomaliedetails werden geladen...</p>;
  if (error) return <p className="field-error" role="alert">{germanErrorMessage(error)}</p>;
  if (!detail) return null;
  const { anomaly, recommendations } = detail;
  return <section className="panel" aria-labelledby="anomaly-detail-title"><h2 id="anomaly-detail-title">Anomaliedetails</h2><p>{anomaly.explanation}</p><dl className="detail-list"><dt>Beobachteter Wert</dt><dd>{formatDecimal(anomaly.observedValue)}</dd><dt>Baseline</dt><dd>{anomaly.baselineAverage === null ? "Nicht verfügbar" : formatDecimal(anomaly.baselineAverage)} ({anomaly.baselineQuality})</dd><dt>Z-Score</dt><dd>{anomaly.zScore === null ? "-" : formatDecimal(anomaly.zScore)}</dd><dt>Erkennung</dt><dd>{anomaly.detectionMethod}</dd></dl><h3>Empfehlungen</h3>{recommendations.map((recommendation) => <article className="recommendation" key={recommendation.id}><p>{recommendation.messageDe}</p><p className="disclaimer" role="note">{recommendation.disclaimerDe}</p></article>)}{recommendations.length === 0 ? <p>Keine Empfehlung vorhanden.</p> : null}{linkedTicket ? <p className="info-panel">Verknüpftes Ticket: {linkedTicket.title} ({linkedTicket.status})</p> : canManage && anomaly.status !== "SUPERSEDED" ? <CreateTicketForm onSubmit={onCreateTicket} pending={ticketPending} error={ticketError} /> : null}</section>;
}

function CreateTicketForm({ onSubmit, pending, error }: { onSubmit: (values: { title: string; issueCategory?: string; priority?: string }) => void; pending: boolean; error: unknown }) {
  const { handleSubmit, register } = useForm<{ title: string; issueCategory: string; priority: string }>({ defaultValues: { title: "Anomalie untersuchen", priority: "HIGH" } });
  return <form className="inline-form" onSubmit={handleSubmit(onSubmit)}><h3>In Wartungsticket umwandeln</h3><label className="field"><span>Titel</span><input {...register("title", { required: true })} /></label><label className="field"><span>Fehlerkategorie</span><input {...register("issueCategory")} /></label><label className="field"><span>Priorität</span><select {...register("priority")}><option value="LOW">Niedrig</option><option value="MEDIUM">Mittel</option><option value="HIGH">Hoch</option><option value="CRITICAL">Kritisch</option></select></label><button className="primary-button" disabled={pending} type="submit">Ticket erstellen</button>{error ? <p className="field-error" role="alert">{germanErrorMessage(error)}</p> : null}</form>;
}
