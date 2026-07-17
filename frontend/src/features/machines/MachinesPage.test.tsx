import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import "../../app/i18n";
import { createQueryClient } from "../../app/queryClient";
import { dashboardSummary, stubJsonFetch } from "../dashboard/testSupport";
import { MachinesPage } from "./MachinesPage";

function renderMachines() {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <MachinesPage />
    </QueryClientProvider>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe("MachinesPage", () => {
  it("renders machine monitoring cards and top consumers from backend summary data", async () => {
    stubJsonFetch(dashboardSummary());

    renderMachines();

    expect(await screen.findByRole("heading", { level: 1, name: "Maschinenmonitoring" })).toBeInTheDocument();
    expect(await screen.findByText("Energieverbrauch nach Maschine/Linie")).toBeInTheDocument();
    expect(screen.getByText("machine-1")).toBeInTheDocument();
    expect(screen.getByText("60 kWh")).toBeInTheDocument();
  });
});
