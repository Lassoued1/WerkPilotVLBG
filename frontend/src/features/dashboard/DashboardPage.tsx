import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { germanErrorMessage } from "../../shared/api/http";
import { defaultKpiFilters, fetchDashboardSummary, type DashboardSummaryResponse, type KpiFilters } from "./api";
import { FilterPanel } from "./FilterPanel";
import { formatDateTime, formatDecimal, formatInteger, formatKpiValue, kpiReason, unitLabel } from "./format";

export function DashboardPage() {
  const { t } = useTranslation();
  const [filters, setFilters] = useState<KpiFilters>(() => defaultKpiFilters());
  const summaryQuery = useQuery({
    queryKey: ["dashboard-summary", filters],
    queryFn: () => fetchDashboardSummary(filters),
  });

  return (
    <section className="page-stack" aria-labelledby="dashboard-title">
      <div className="page-heading">
        <h1 id="dashboard-title">{t("dashboard.title")}</h1>
        <p>{t("dashboard.description")}</p>
      </div>

      <FilterPanel filters={filters} onApply={setFilters} />

      {summaryQuery.isPending ? <p role="status">{t("dashboard.loading")}</p> : null}
      {summaryQuery.isError ? (
        <p className="field-error" role="alert">
          {germanErrorMessage(summaryQuery.error)}
        </p>
      ) : null}
      {summaryQuery.data ? <DashboardSummary summary={summaryQuery.data} /> : null}
    </section>
  );
}

function DashboardSummary({ summary }: { summary: DashboardSummaryResponse }) {
  const { t } = useTranslation();
  const metricCards = [
    {
      label: t("dashboard.producedUnits"),
      value: formatInteger(summary.totalUnitsProduced),
      unit: t("dashboard.units.pieces"),
      help: t("dashboard.backendOnly"),
    },
    {
      label: t("dashboard.outputPerHour"),
      value: formatKpiValue(summary.outputPerHour),
      unit: unitLabel(summary.outputPerHour.unit),
      help: kpiReason(summary.outputPerHour) || t("dashboard.backendOnly"),
    },
    {
      label: t("dashboard.energyPerUnit"),
      value: formatKpiValue(summary.energyPerUnit),
      unit: unitLabel(summary.energyPerUnit.unit),
      help: kpiReason(summary.energyPerUnit) || t("dashboard.backendOnly"),
    },
    {
      label: t("dashboard.scrapRate"),
      value: formatKpiValue(summary.scrapRate),
      unit: unitLabel(summary.scrapRate.unit),
      help: kpiReason(summary.scrapRate) || t("dashboard.backendOnly"),
    },
    {
      label: t("dashboard.availability"),
      value: formatKpiValue(summary.availability),
      unit: unitLabel(summary.availability.unit),
      help: kpiReason(summary.availability) || t("dashboard.backendOnly"),
    },
    {
      label: t("dashboard.downtime"),
      value: formatInteger(summary.totalDowntimeMinutes),
      unit: t("dashboard.units.minutes"),
      help: t("dashboard.backendOnly"),
    },
    {
      label: t("dashboard.energyTotal"),
      value: formatDecimal(summary.totalEnergyKwh),
      unit: "kWh",
      help: t("dashboard.backendOnly"),
    },
    {
      label: t("dashboard.scrapTotal"),
      value: formatInteger(summary.totalScrapCount),
      unit: t("dashboard.units.pieces"),
      help: t("dashboard.backendOnly"),
    },
  ];

  return (
    <>
      <div className="metric-grid dashboard-metric-grid">
        {metricCards.map((metric) => (
          <article className="metric-card" key={metric.label}>
            <span className="metric-label">{metric.label}</span>
            <strong>{metric.value}</strong>
            <span className="metric-unit">{metric.unit}</span>
            <small className="metric-help">{metric.help}</small>
          </article>
        ))}
      </div>

      <section className="panel" aria-labelledby="trend-title">
        <div className="section-heading">
          <h2 id="trend-title">{t("dashboard.productionTrend")}</h2>
          <p>{t("dashboard.chartFallbackHelp")}</p>
        </div>
        {summary.productionTrend.length > 0 ? (
          <div className="chart-frame" aria-hidden="true">
            <ResponsiveContainer height={260} width="100%">
              <BarChart data={summary.productionTrend} margin={{ bottom: 8, left: 0, right: 8, top: 8 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="bucketStart" tickFormatter={formatDateTime} />
                <YAxis />
                <Tooltip labelFormatter={(value) => formatDateTime(String(value))} />
                <Legend />
                <Bar dataKey="unitsProduced" fill="#2563eb" name={t("dashboard.producedUnits")} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        ) : (
          <p role="status">{t("dashboard.emptyTrend")}</p>
        )}

        <h3 className="fallback-title">{t("dashboard.tableFallback")}</h3>
        <table className="data-table">
          <thead>
            <tr>
              <th scope="col">{t("dashboard.table.period")}</th>
              <th scope="col">{t("dashboard.table.units")}</th>
            </tr>
          </thead>
          <tbody>
            {summary.productionTrend.map((row) => (
              <tr key={row.bucketStart}>
                <th scope="row">{formatDateTime(row.bucketStart)}</th>
                <td>{formatInteger(row.unitsProduced)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <div className="two-column-grid">
        <section className="panel" aria-labelledby="pareto-title">
          <h2 id="pareto-title">{t("dashboard.downtimePareto")}</h2>
          <table className="data-table compact-table">
            <thead>
              <tr>
                <th>{t("dashboard.table.reason")}</th>
                <th>{t("dashboard.table.minutes")}</th>
                <th>{t("dashboard.table.cumulative")}</th>
              </tr>
            </thead>
            <tbody>
              {summary.downtimePareto.map((row) => (
                <tr key={row.reasonId}>
                  <th scope="row">{row.reasonName}</th>
                  <td>{formatInteger(row.downtimeMinutes)}</td>
                  <td>{formatDecimal(row.cumulativePercentage)} %</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section className="panel" aria-labelledby="energy-title">
          <h2 id="energy-title">{t("dashboard.energyTopConsumers")}</h2>
          <table className="data-table compact-table">
            <thead>
              <tr>
                <th>{t("dashboard.table.asset")}</th>
                <th>{t("dashboard.table.energy")}</th>
              </tr>
            </thead>
            <tbody>
              {summary.energyTopConsumers.map((row) => (
                <tr key={`${row.lineId}-${row.machineId ?? "line"}`}>
                  <th scope="row">{row.machineId ?? row.lineId}</th>
                  <td>{formatDecimal(row.energyKwh)} kWh</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>
    </>
  );
}
