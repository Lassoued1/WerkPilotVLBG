import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";

import "../../app/i18n";
import { createQueryClient } from "../../app/queryClient";
import { AdminPage } from "./AdminPage";

function renderPage() {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <AdminPage />
    </QueryClientProvider>,
  );
}

describe("Sprint 1 admin UI", () => {
  beforeEach(() => {
    window.sessionStorage.clear();
  });

  it("shows the administration page and permission state for non-admin users", async () => {
    renderPage();

    expect(await screen.findByRole("heading", { level: 1, name: "Administration" })).toBeInTheDocument();
    expect(screen.getByRole("note")).toHaveTextContent("Schreibaktionen sind ADMIN vorbehalten");
    expect(screen.getByRole("button", { name: "Benutzer speichern" })).toBeDisabled();
    expect(await screen.findByRole("rowheader", { name: "Demo Admin" })).toBeInTheDocument();
  });

  it("validates the create-user form", async () => {
    window.sessionStorage.setItem("werkpilot.session", JSON.stringify({
      accessToken: "token",
      csrfToken: "csrf",
      profile: { id: "1", email: "admin@werkpilot.local", displayName: "Admin", roles: ["ADMIN"] },
    }));
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole("button", { name: "Benutzer speichern" }));

    expect(await screen.findByText("E-Mail ist erforderlich.")).toBeInTheDocument();
  });
});
