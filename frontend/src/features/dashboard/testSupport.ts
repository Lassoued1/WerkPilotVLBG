import { vi } from "vitest";

export function dashboardSummary(overrides: Record<string, unknown> = {}) {
  return {
    appliedFilters: {
      from: "2026-07-01T00:00:00Z",
      to: "2026-07-02T00:00:00Z",
      factoryId: null,
      lineId: null,
      machineId: null,
      productId: null,
      shiftId: null,
    },
    totalUnitsProduced: 120,
    totalEnergyKwh: 60,
    totalDowntimeMinutes: 30,
    totalScrapCount: 6,
    outputPerHour: { value: 60, unit: "units_per_hour", available: true },
    energyPerUnit: { value: 0.5, unit: "kWh_per_unit", available: true },
    availability: { value: 93.75, unit: "percent", available: true },
    scrapRate: { value: 5, unit: "percent", available: true },
    productionTrend: [
      { bucketStart: "2026-07-01T08:00:00Z", unitsProduced: 50 },
      { bucketStart: "2026-07-01T09:00:00Z", unitsProduced: 70 },
    ],
    downtimePareto: [
      {
        reasonId: "reason-1",
        reasonName: "Wartung",
        downtimeMinutes: 30,
        cumulativePercentage: 100,
      },
    ],
    energyTopConsumers: [{ lineId: "line-1", machineId: "machine-1", energyKwh: 60 }],
    ...overrides,
  };
}

export function stubJsonFetch(body: unknown) {
  return vi.spyOn(globalThis, "fetch").mockResolvedValue(
    new Response(JSON.stringify(body), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    }),
  );
}
