import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import "../../app/i18n";
import { createQueryClient } from "../../app/queryClient";
import { DashboardPage } from "./DashboardPage";
import { dashboardSummary, stubJsonFetch } from "./testSupport";

function renderDashboard() {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <DashboardPage />
    </QueryClientProvider>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe("DashboardPage", () => {
  it("renders German backend KPI cards without recalculating formulas in the UI", async () => {
    stubJsonFetch(dashboardSummary());

    renderDashboard();

    expect(await screen.findByText("Ausbringung pro Stunde")).toBeInTheDocument();
    expect(screen.getAllByText("60").length).toBeGreaterThan(0);
    expect(screen.getByText("0,5")).toBeInTheDocument();
    expect(screen.getAllByText("Vom Backend berechnet").length).toBeGreaterThan(1);
    expect(screen.getByRole("heading", { level: 2, name: "Produktionstrend" })).toBeInTheDocument();
  });

  it("keeps an accessible table fallback for chart data and applies filters to the request", async () => {
    const fetchSpy = stubJsonFetch(dashboardSummary());
    const user = userEvent.setup();

    renderDashboard();

    await screen.findByText("Stillstands-Pareto");
    const table = screen.getAllByRole("table")[0];
    expect(within(table).getByRole("columnheader", { name: "Zeitraum" })).toBeInTheDocument();
    expect(within(table).getByRole("columnheader", { name: "Einheiten" })).toBeInTheDocument();

    await user.clear(screen.getByLabelText("Maschinen-ID"));
    await user.type(screen.getByLabelText("Maschinen-ID"), "11111111-1111-1111-1111-111111111111");
    await user.click(screen.getByRole("button", { name: "Filter anwenden" }));

    expect(fetchSpy.mock.calls.at(-1)?.[0]?.toString()).toContain("machineId=11111111-1111-1111-1111-111111111111");
  });
});
