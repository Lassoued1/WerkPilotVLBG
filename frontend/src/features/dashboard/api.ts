import { apiRequest } from "../../shared/api/http";

export type KpiValue = {
  value: number | null;
  unit: string;
  available: boolean;
  reason?: string;
};

export type AppliedKpiFilters = {
  from: string;
  to: string;
  factoryId: string | null;
  lineId: string | null;
  machineId: string | null;
  productId: string | null;
  shiftId: string | null;
};

export type KpiFilters = {
  from: string;
  to: string;
  factoryId?: string;
  lineId?: string;
  machineId?: string;
  productId?: string;
  shiftId?: string;
};

export type ProductionTrendPoint = {
  bucketStart: string;
  unitsProduced: number;
};

export type DowntimeParetoPoint = {
  reasonId: string;
  reasonName: string;
  downtimeMinutes: number;
  cumulativePercentage: number;
};

export type EnergyTopConsumer = {
  lineId: string;
  machineId: string | null;
  energyKwh: number;
};

export type DashboardSummaryResponse = {
  appliedFilters: AppliedKpiFilters;
  totalUnitsProduced: number;
  totalEnergyKwh: number;
  totalDowntimeMinutes: number;
  totalScrapCount: number;
  outputPerHour: KpiValue;
  energyPerUnit: KpiValue;
  availability: KpiValue;
  scrapRate: KpiValue;
  productionTrend: ProductionTrendPoint[];
  downtimePareto: DowntimeParetoPoint[];
  energyTopConsumers: EnergyTopConsumer[];
};

export type ProductionRecordView = {
  id: string;
  periodStart: string;
  periodEnd: string;
  factoryId: string;
  lineId: string;
  machineId: string | null;
  productId: string | null;
  shiftId: string;
  unitsProduced: number;
  batchCode: string | null;
  importJobId: string;
};

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export function defaultKpiFilters(): KpiFilters {
  return {
    from: "2026-07-01T00:00:00Z",
    to: "2026-07-02T00:00:00Z",
  };
}

export function kpiQueryString(filters: KpiFilters, extra: Record<string, string | number> = {}) {
  const parameters = new URLSearchParams();
  for (const [key, value] of Object.entries({ ...filters, ...extra })) {
    if (value !== undefined && value !== "") {
      parameters.set(key, String(value));
    }
  }
  return parameters.toString();
}

export function fetchDashboardSummary(filters: KpiFilters) {
  return apiRequest<DashboardSummaryResponse>(`/dashboard/summary?${kpiQueryString(filters)}`);
}

export function fetchProductionRecords(filters: KpiFilters, page = 0, size = 10) {
  return apiRequest<PageResponse<ProductionRecordView>>(
    `/production/records?${kpiQueryString(filters, { page, size })}`,
  );
}

export function productionEvidenceUrl(filters: KpiFilters) {
  return `/api/v1/production/evidence.csv?${kpiQueryString(filters)}`;
}
