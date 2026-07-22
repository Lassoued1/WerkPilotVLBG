import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { createQueryClient } from "../../app/queryClient";
import { MaintenancePage } from "./MaintenancePage";

const ticket = {
  id: "ticket-1",
  title: "Hydraulik prüfen",
  description: "Druckverlust prüfen.",
  issueCategory: "HYDRAULICS",
  status: "OPEN",
  priority: "HIGH",
  factoryId: null,
  lineId: null,
  machineId: "machine-1",
  anomalyId: "anomaly-1",
  assigneeUserId: "user-1",
  dueDate: "2026-07-20",
  resolutionNote: null,
  cancellationReason: null,
  createdByUserId: "admin-1",
  createdAt: "2026-07-19T08:00:00Z",
  updatedAt: "2026-07-19T08:00:00Z",
  overdue: true,
};

function renderPage(roles: string[] = ["ADMIN"]) {
  window.sessionStorage.setItem("werkpilot.session", JSON.stringify({
    accessToken: "token",
    csrfToken: "csrf",
    profile: { id: "user-1", email: "user@test", displayName: "User", roles },
  }));
  return render(<QueryClientProvider client={createQueryClient()}><MaintenancePage /></QueryClientProvider>);
}

afterEach(() => {
  vi.restoreAllMocks();
  window.sessionStorage.clear();
});

describe("MaintenancePage", () => {
  it("renders ticket status, computed overdue badge, and detail comments", async () => {
    vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => new Response(JSON.stringify(
      String(input).includes("/maintenance-tickets/ticket-1")
        ? { ticket, comments: [{ id: "comment-1", ticketId: "ticket-1", authorUserId: "user-1", message: "Teil bestellt.", createdAt: "2026-07-19T09:00:00Z" }] }
        : [ticket],
    ), { status: 200 }));

    renderPage();
    expect(await screen.findByText("Überfällig")).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole("button", { name: "Details" }));
    expect(await screen.findByText("Teil bestellt.")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Status ändern" })).toBeInTheDocument();
  });

  it("shows ticket creation only to a manager role", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }));
    renderPage(["VIEWER"]);
    expect(await screen.findByText("Keine Tickets für die gewählten Filter.")).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Neues Ticket" })).not.toBeInTheDocument();
  });
});
