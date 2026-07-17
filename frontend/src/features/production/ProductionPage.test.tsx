import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import "../../app/i18n";
import { createQueryClient } from "../../app/queryClient";
import { dashboardSummary } from "../dashboard/testSupport";
import { ProductionPage } from "./ProductionPage";

function jsonResponse(body: unknown) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}

function stubProductionFetch() {
  return vi.spyOn(globalThis, "fetch").mockImplementation(async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/dashboard/summary")) {
      return jsonResponse(dashboardSummary());
    }
    if (url.includes("/production/records")) {
      return jsonResponse({
        items: [
          {
            id: "record-1",
            periodStart: "2026-07-01T08:00:00Z",
            periodEnd: "2026-07-01T09:00:00Z",
            factoryId: "factory-1",
            lineId: "line-1",
            machineId: "machine-1",
            productId: "product-1",
            shiftId: "shift-1",
            unitsProduced: 50,
            batchCode: "BATCH-S3",
            importJobId: "job-1",
          },
        ],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      });
    }
    return jsonResponse({ errorCode: "RESOURCE_NOT_FOUND" });
  });
}

function renderProduction() {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <ProductionPage />
    </QueryClientProvider>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe("ProductionPage", () => {
  it("renders production trend, record traceability and evidence CSV link", async () => {
    stubProductionFetch();

    renderProduction();

    expect(await screen.findByRole("heading", { level: 1, name: "Produktionsdatensätze" })).toBeInTheDocument();
    expect(await screen.findByText("BATCH-S3")).toBeInTheDocument();
    expect(screen.getByText("job-1")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Evidence CSV herunterladen" })).toHaveAttribute(
      "href",
      expect.stringContaining("/api/v1/production/evidence.csv"),
    );

    const recordsTable = screen.getAllByRole("table").at(-1);
    expect(recordsTable).toBeDefined();
    expect(within(recordsTable!).getByRole("columnheader", { name: "Import-Job" })).toBeInTheDocument();
  });
});
