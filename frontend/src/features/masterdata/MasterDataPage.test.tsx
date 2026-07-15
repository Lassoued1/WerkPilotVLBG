import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";

import "../../app/i18n";
import { createQueryClient } from "../../app/queryClient";
import { MasterDataPage } from "./MasterDataPage";

function renderPage() {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <MasterDataPage />
    </QueryClientProvider>,
  );
}

describe("Sprint 1 master-data UI", () => {
  beforeEach(() => {
    window.sessionStorage.clear();
  });

  it("renders all required master-data resources and read-only state", async () => {
    renderPage();

    expect(await screen.findByRole("heading", { level: 1, name: "Stammdaten" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 2, name: "Werke" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 2, name: "Linien" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 2, name: "Maschinen" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 2, name: "Produkte" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 2, name: "Schichten" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 2, name: "Stillstandsgründe" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 2, name: "Ausschusskategorien" })).toBeInTheDocument();
    expect(screen.getByRole("note")).toHaveTextContent("Nur ADMIN darf Stammdaten");
  });

  it("validates code and name before creating a record", async () => {
    window.sessionStorage.setItem("werkpilot.session", JSON.stringify({
      accessToken: "token",
      csrfToken: "csrf",
      profile: { id: "1", email: "admin@werkpilot.local", displayName: "Admin", roles: ["ADMIN"] },
    }));
    const user = userEvent.setup();
    renderPage();

    const buttons = await screen.findAllByRole("button", { name: "Anlegen" });
    await user.click(buttons[0]);

    expect(await screen.findByText("Bitte Code und Name ausfüllen.")).toBeInTheDocument();
  });
});
