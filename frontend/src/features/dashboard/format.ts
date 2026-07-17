import type { KpiValue } from "./api";

const numberFormatter = new Intl.NumberFormat("de-AT", {
  maximumFractionDigits: 3,
});

const integerFormatter = new Intl.NumberFormat("de-AT", {
  maximumFractionDigits: 0,
});

export function formatInteger(value: number) {
  return integerFormatter.format(value);
}

export function formatDecimal(value: number) {
  return numberFormatter.format(value);
}

export function formatDateTime(value: string) {
  return new Date(value).toLocaleString("de-AT", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    month: "2-digit",
  });
}

export function formatKpiValue(kpi: KpiValue) {
  if (!kpi.available || kpi.value === null) {
    return "N/A";
  }
  return formatDecimal(kpi.value);
}

export function kpiReason(kpi: KpiValue) {
  if (kpi.available) return "";
  switch (kpi.reason) {
    case "NO_PRODUCTION_TIME":
      return "Keine Produktionszeit";
    case "NO_UNITS_PRODUCED":
      return "Keine produzierten Einheiten";
    case "NO_PLANNED_MINUTES":
      return "Keine Planzeit";
    default:
      return "Nicht verfügbar";
  }
}

export function unitLabel(unit: string) {
  switch (unit) {
    case "units_per_hour":
      return "Einheiten/Stunde";
    case "kWh_per_unit":
      return "kWh/Einheit";
    case "percent":
      return "%";
    default:
      return unit;
  }
}
