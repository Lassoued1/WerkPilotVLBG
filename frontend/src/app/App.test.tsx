import { render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { App } from "./App";
import { createTestRouter } from "./routes";
import { dashboardSummary } from "../features/dashboard/testSupport";

function renderApp(initialEntries = ["/"]) {
  return render(<App router={createTestRouter(initialEntries)} />);
}

function stubDashboard() {
  return vi.spyOn(globalThis, "fetch").mockResolvedValue(
    new Response(JSON.stringify(dashboardSummary()), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    }),
  );
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe("WerkPilot frontend shell", () => {
  it("renders the German dashboard shell and planned navigation areas", async () => {
    stubDashboard();

    renderApp();

    expect(
      await screen.findByRole(
        "heading",
        { level: 1, name: "Dashboard" },
        { timeout: 3_000 },
      ),
    ).toBeInTheDocument();
    expect(screen.getByText("WerkPilot VLBG")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "CSV-Import" })).toHaveAttribute(
      "href",
      "/imports",
    );
    expect(screen.getByRole("link", { name: "Maschinen" })).toHaveAttribute(
      "href",
      "/machines",
    );
    expect(screen.getByRole("link", { name: "Produktion" })).toHaveAttribute(
      "href",
      "/production",
    );
    expect(screen.getByRole("link", { name: "Stammdaten" })).toHaveAttribute(
      "href",
      "/master-data",
    );
    expect(
      screen.getByText(
        "Backend-Werte werden hier nur visualisiert; KPI-Berechnungen bleiben serverseitig.",
      ),
    ).toBeInTheDocument();
  });

  it("renders a table fallback for chart data", async () => {
    stubDashboard();

    renderApp();

    const tables = await screen.findAllByRole("table");
    const table = tables[0];

    expect(
      screen.getByRole("heading", {
        level: 3,
        name: "Tabellarische Alternative",
      }),
    ).toBeInTheDocument();
    expect(
      within(table).getByRole("columnheader", { name: "Zeitraum" }),
    ).toBeInTheDocument();
    expect(within(table).getByRole("columnheader", { name: "Einheiten" }))
      .toBeInTheDocument();
  });

  it("shows the read-only import banner without an upload-capable session", async () => {
    renderApp(["/imports"]);

    expect(
      await screen.findByRole("heading", { level: 1, name: "CSV-Import" }),
    ).toBeInTheDocument();
    expect(await screen.findByRole("note")).toHaveTextContent(
      "Lesemodus: Ihre Rolle erlaubt keine CSV-Uploads.",
    );
    expect(
      screen.getByRole("button", { name: "Import starten" }),
    ).toBeDisabled();
  });

  it("renders a German not-found page for unknown routes", async () => {
    renderApp(["/nicht-vorhanden"]);

    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "Seite nicht gefunden",
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Zurück zum Dashboard" }),
    ).toHaveAttribute("href", "/");
  });
});
