import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { createQueryClient } from "../../app/queryClient";
import "../../app/i18n";
import { ImportsPage } from "./ImportsPage";

type FetchRoute = {
  method: string;
  path: string;
  status?: number;
  body: unknown;
};

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function stubFetch(routes: FetchRoute[]) {
  return vi
    .spyOn(globalThis, "fetch")
    .mockImplementation(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === "string" ? input : input instanceof URL ? input.toString() : input.url;
      const method = (init?.method ?? "GET").toUpperCase();
      const route = routes.find((candidate) => candidate.method === method && url.includes(candidate.path));
      if (!route) {
        return jsonResponse({ errorCode: "RESOURCE_NOT_FOUND" }, 404);
      }
      return jsonResponse(route.body, route.status ?? 200);
    });
}

function writeTestSession(roles: string[]) {
  window.sessionStorage.setItem(
    "werkpilot.session",
    JSON.stringify({
      accessToken: "token",
      csrfToken: "csrf",
      profile: { id: "1", email: "user@werkpilot.local", displayName: "Test User", roles },
    }),
  );
}

function emptyPage() {
  return { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
}

function committedJob(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: "11111111-1111-1111-1111-111111111111",
    importType: "PRODUCTION_RECORDS",
    status: "COMMITTED",
    originalFilename: "produktion.csv",
    safeFilename: "produktion.csv",
    fileHashSha256: "abc",
    fileSizeBytes: 120,
    totalRows: 2,
    validRows: 2,
    errorCount: 0,
    errorOverflow: false,
    correctsImportJobId: null,
    createdByUserId: "1",
    createdAt: "2026-07-01T08:00:00Z",
    completedAt: "2026-07-01T08:00:05Z",
    failureReason: null,
    ...overrides,
  };
}

function failedJob() {
  return committedJob({
    id: "22222222-2222-2222-2222-222222222222",
    status: "FAILED",
    originalFilename: "fehlerhaft.csv",
    validRows: 0,
    errorCount: 2,
  });
}

function renderPage() {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <ImportsPage />
    </QueryClientProvider>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
  window.sessionStorage.clear();
});

describe("ImportsPage", () => {
  it("lets an ADMIN choose all four import types and starts a multipart upload", async () => {
    writeTestSession(["ADMIN"]);
    const fetchSpy = stubFetch([
      { method: "GET", path: "/import-jobs?", body: emptyPage() },
      {
        method: "POST",
        path: "/import-jobs/production-records",
        body: { jobId: "job-1", status: "PROCESSING", createdAt: "2026-07-01T08:00:00Z", completedAt: null },
      },
    ]);
    const user = userEvent.setup();

    renderPage();

    const select = await screen.findByLabelText("Importtyp");
    const options = within(select).getAllByRole("option");
    expect(options.map((option) => option.textContent)).toEqual([
      "Produktion",
      "Energie",
      "Stillstand",
      "Ausschuss",
    ]);

    const file = new File(["period_start\n"], "produktion.csv", { type: "text/csv" });
    await user.upload(screen.getByLabelText("CSV-Datei"), file);
    await user.click(screen.getByRole("button", { name: "Import starten" }));

    expect(
      await screen.findByText("Der Import wurde gestartet und wird verarbeitet."),
    ).toBeInTheDocument();

    const uploadCall = fetchSpy.mock.calls.find(
      ([, init]) => (init?.method ?? "GET").toUpperCase() === "POST",
    );
    expect(uploadCall).toBeDefined();
    const [uploadUrl, uploadInit] = uploadCall!;
    expect(String(uploadUrl)).toContain("/api/v1/import-jobs/production-records");
    expect(uploadInit?.body).toBeInstanceOf(FormData);
    expect(new Headers(uploadInit?.headers).get("Content-Type")).toBeNull();
    expect(new Headers(uploadInit?.headers).get("X-WerkPilot-CSRF")).toBe("csrf");
  });

  it("shows the read-only banner and hides admin actions for a VIEWER", async () => {
    writeTestSession(["VIEWER"]);
    stubFetch([{ method: "GET", path: "/import-jobs?", body: { ...emptyPage(), items: [committedJob()], totalElements: 1, totalPages: 1 } }]);

    renderPage();

    expect(await screen.findByRole("note")).toHaveTextContent("Lesemodus");
    expect(screen.getByRole("button", { name: "Import starten" })).toBeDisabled();
    expect(await screen.findByText("Festgeschrieben")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Korrigieren" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Zurückrollen" })).not.toBeInTheDocument();
  });

  it("offers production, downtime and scrap but not energy to a PRODUCTION_MANAGER", async () => {
    writeTestSession(["PRODUCTION_MANAGER"]);
    stubFetch([{ method: "GET", path: "/import-jobs?", body: { ...emptyPage(), items: [committedJob()], totalElements: 1, totalPages: 1 } }]);

    renderPage();

    const select = await screen.findByLabelText("Importtyp");
    const options = within(select).getAllByRole("option");
    expect(options.map((option) => option.textContent)).toEqual([
      "Produktion",
      "Stillstand",
      "Ausschuss",
    ]);
    expect(screen.queryByRole("button", { name: "Korrigieren" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Zurückrollen" })).not.toBeInTheDocument();
  });

  it("renders German job statuses and shows error details for a failed job", async () => {
    writeTestSession(["ADMIN"]);
    stubFetch([
      {
        method: "GET",
        path: "/import-jobs?",
        body: { ...emptyPage(), items: [committedJob(), failedJob()], totalElements: 2, totalPages: 1 },
      },
      {
        method: "GET",
        path: `/import-jobs/${failedJob().id}/errors`,
        body: {
          ...emptyPage(),
          items: [
            {
              id: "err-1",
              importJobId: failedJob().id,
              rowNumber: 2,
              columnName: "units_produced",
              rejectedValue: "-1",
              message: "Der Wert muss größer oder gleich null sein.",
              createdAt: "2026-07-01T08:00:00Z",
            },
          ],
          totalElements: 1,
          totalPages: 1,
        },
      },
    ]);
    const user = userEvent.setup();

    renderPage();

    expect(await screen.findByText("Festgeschrieben")).toBeInTheDocument();
    expect(screen.getByText("Fehlgeschlagen")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Fehler anzeigen" }));

    expect(await screen.findByText("Fehlerdetails")).toBeInTheDocument();
    expect(screen.getByText("units_produced")).toBeInTheDocument();
    expect(
      screen.getByText("Der Wert muss größer oder gleich null sein."),
    ).toBeInTheDocument();
  });

  it("requires a rollback reason and posts it as JSON for an ADMIN", async () => {
    writeTestSession(["ADMIN"]);
    const job = committedJob();
    const fetchSpy = stubFetch([
      {
        method: "GET",
        path: "/import-jobs?",
        body: { ...emptyPage(), items: [job], totalElements: 1, totalPages: 1 },
      },
      {
        method: "POST",
        path: `/import-jobs/${job.id}/rollback`,
        body: { jobId: job.id, status: "SUPERSEDED", createdAt: job.createdAt, completedAt: job.completedAt },
      },
    ]);
    const user = userEvent.setup();

    renderPage();

    await user.click(await screen.findByRole("button", { name: "Zurückrollen" }));
    await user.click(screen.getByRole("button", { name: "Rollback ausführen" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Bitte geben Sie eine Begründung an",
    );

    await user.type(screen.getByLabelText("Begründung"), "Falsche Schicht importiert.");
    await user.click(screen.getByRole("button", { name: "Rollback ausführen" }));

    const rollbackCall = fetchSpy.mock.calls.find(([url, init]) =>
      String(url).includes("/rollback") && (init?.method ?? "GET").toUpperCase() === "POST",
    );
    expect(rollbackCall).toBeDefined();
    const [, rollbackInit] = rollbackCall!;
    expect(new Headers(rollbackInit?.headers).get("Content-Type")).toBe("application/json");
    expect(JSON.parse(String(rollbackInit?.body))).toEqual({ reason: "Falsche Schicht importiert." });
  });
});
