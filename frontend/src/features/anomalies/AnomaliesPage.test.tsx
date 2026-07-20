import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { createQueryClient } from "../../app/queryClient";
import { AnomaliesPage } from "./AnomaliesPage";

const anomaly = { id: "a-1", metricKey: "ENERGY_KWH", anomalyType: "THRESHOLD_BREACH", severity: "CRITICAL", status: "NEW", detectionMethod: "THRESHOLD", factoryId: null, lineId: "line-1", machineId: "machine-1", productId: null, shiftId: null, periodStart: "2026-07-01T08:00:00Z", periodEnd: "2026-07-01T09:00:00Z", observedValue: 120, baselineAverage: 90, baselineStddev: 5, baselineCount: 10, baselineQuality: "HIGH", zScore: 6, thresholdRuleId: null, explanation: "Grenzwert überschritten.", previousAnomalyId: null, supersededByAnomalyId: null, createdAt: "2026-07-01T09:00:00Z", updatedAt: "2026-07-01T09:00:00Z" };
function renderPage(roles: string[] = []) { window.sessionStorage.setItem("werkpilot.session", JSON.stringify({ accessToken: "token", csrfToken: "csrf", profile: { id: "u-1", email: "user@test", displayName: "User", roles } })); return render(<QueryClientProvider client={createQueryClient()}><AnomaliesPage /></QueryClientProvider>); }
afterEach(() => { vi.restoreAllMocks(); window.sessionStorage.clear(); });
describe("AnomaliesPage", () => {
  it("shows backend anomaly data and recommendation disclaimer", async () => { vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => new Response(JSON.stringify(String(input).includes("/anomalies/a-1") ? { anomaly, recommendations: [{ id: "r-1", templateCode: "ENERGY", templateVersion: "1", messageDe: "Prüfen Sie die Anlage.", disclaimerDe: "Hinweis: Diese Empfehlung ersetzt keine fachliche Prüfung." }] } : { items: [anomaly], page: 0, size: 20, totalElements: 1, totalPages: 1 }), { status: 200 })); const user = userEvent.setup(); renderPage(); await user.click(await screen.findByRole("button", { name: "Details" })); expect(await screen.findByRole("note")).toHaveTextContent("Hinweis:"); });
  it("shows rerun only to ADMIN and sends the POST request", async () => { const fetchSpy = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }), { status: 200 })); renderPage(["ADMIN"]); const button = await screen.findByRole("button", { name: "Analyse erneut ausführen" }); await userEvent.setup().click(button); expect(fetchSpy).toHaveBeenCalledWith("/api/v1/anomalies/rerun", expect.objectContaining({ method: "POST" })); });
});
