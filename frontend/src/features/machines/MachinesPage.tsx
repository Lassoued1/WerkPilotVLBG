import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import { germanErrorMessage } from "../../shared/api/http";
import { defaultKpiFilters, fetchDashboardSummary, type KpiFilters } from "../dashboard/api";
import { FilterPanel } from "../dashboard/FilterPanel";
import { formatDecimal, formatInteger, formatKpiValue, unitLabel } from "../dashboard/format";

export function MachinesPage() {
  const { t } = useTranslation();
  const [filters, setFilters] = useState<KpiFilters>(() => defaultKpiFilters());
  const summaryQuery = useQuery({
    queryKey: ["machines-summary", filters],
    queryFn: () => fetchDashboardSummary(filters),
  });

  return (
    <section className="page-stack" aria-labelledby="machines-title">
      <div className="page-heading">
        <h1 id="machines-title">{t("machines.title")}</h1>
        <p>{t("machines.description")}</p>
      </div>

      <FilterPanel filters={filters} onApply={setFilters} />

      {summaryQuery.isPending ? <p role="status">{t("dashboard.loading")}</p> : null}
      {summaryQuery.isError ? (
        <p className="field-error" role="alert">
          {germanErrorMessage(summaryQuery.error)}
        </p>
      ) : null}
      {summaryQuery.data ? (
        <>
          <div className="metric-grid">
            <article className="metric-card">
              <span className="metric-label">{t("machines.units")}</span>
              <strong>{formatInteger(summaryQuery.data.totalUnitsProduced)}</strong>
              <span className="metric-unit">{t("dashboard.units.pieces")}</span>
            </article>
            <article className="metric-card">
              <span className="metric-label">{t("machines.availability")}</span>
              <strong>{formatKpiValue(summaryQuery.data.availability)}</strong>
              <span className="metric-unit">{unitLabel(summaryQuery.data.availability.unit)}</span>
            </article>
            <article className="metric-card">
              <span className="metric-label">{t("machines.energy")}</span>
              <strong>{formatDecimal(summaryQuery.data.totalEnergyKwh)}</strong>
              <span className="metric-unit">kWh</span>
            </article>
            <article className="metric-card">
              <span className="metric-label">{t("machines.downtime")}</span>
              <strong>{formatInteger(summaryQuery.data.totalDowntimeMinutes)}</strong>
              <span className="metric-unit">{t("dashboard.units.minutes")}</span>
            </article>
          </div>

          <section className="panel" aria-labelledby="machine-consumers-title">
            <h2 id="machine-consumers-title">{t("machines.topConsumers")}</h2>
            <table className="data-table compact-table">
              <thead>
                <tr>
                  <th>{t("dashboard.table.asset")}</th>
                  <th>{t("dashboard.table.energy")}</th>
                </tr>
              </thead>
              <tbody>
                {summaryQuery.data.energyTopConsumers.map((consumer) => (
                  <tr key={`${consumer.lineId}-${consumer.machineId ?? "line"}`}>
                    <th scope="row">{consumer.machineId ?? consumer.lineId}</th>
                    <td>{formatDecimal(consumer.energyKwh)} kWh</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {summaryQuery.data.energyTopConsumers.length === 0 ? <p role="status">{t("machines.empty")}</p> : null}
          </section>
        </>
      ) : null}
    </section>
  );
}
