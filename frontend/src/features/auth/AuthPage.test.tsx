import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import "../../app/i18n";
import { createQueryClient } from "../../app/queryClient";
import { AuthPage } from "./AuthPage";

function renderPage() {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <AuthPage />
    </QueryClientProvider>,
  );
}

describe("Sprint 1 auth UI", () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it("renders login, reset request, and reset confirm actions in German", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByRole("heading", { level: 1, name: "Anmeldung und Passwort" })).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "Anmelden" })).toHaveLength(2);

    await user.click(screen.getByRole("button", { name: "Reset anfordern" }));
    expect(screen.getByRole("heading", { level: 2, name: "Passwort-Reset anfordern" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Reset bestätigen" }));
    expect(screen.getByRole("heading", { level: 2, name: "Neues Passwort setzen" })).toBeInTheDocument();
  });

  it("validates required login inputs before calling the backend", async () => {
    const user = userEvent.setup();
    const fetchSpy = vi.spyOn(globalThis, "fetch");
    renderPage();

    const loginForm = (await screen.findByRole("heading", { level: 2, name: "Anmelden" })).closest("form");
    expect(loginForm).not.toBeNull();
    await user.click(within(loginForm as HTMLElement).getByRole("button", { name: "Anmelden" }));

    expect(await screen.findByText("Bitte geben Sie eine E-Mail ein.")).toBeInTheDocument();
    expect(screen.getByText("Bitte geben Sie ein Passwort ein.")).toBeInTheDocument();
    expect(fetchSpy).not.toHaveBeenCalled();
  });
});

