import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Line,
  LineChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { germanErrorMessage } from "../../shared/api/http";
import {
  defaultKpiFilters,
  fetchDashboardSummary,
  fetchProductionRecords,
  productionEvidenceUrl,
  type KpiFilters,
} from "../dashboard/api";
import { FilterPanel } from "../dashboard/FilterPanel";
import { formatDateTime, formatInteger } from "../dashboard/format";

export function ProductionPage() {
  const { t } = useTranslation();
  const [filters, setFilters] = useState<KpiFilters>(() => defaultKpiFilters());
  const [page, setPage] = useState(0);

  const trendQuery = useQuery({
    queryKey: ["production-trend", filters],
    queryFn: () => fetchDashboardSummary(filters),
  });
  const recordsQuery = useQuery({
    queryKey: ["production-records", filters, page],
    queryFn: () => fetchProductionRecords(filters, page),
  });

  return (
    <section className="page-stack" aria-labelledby="production-title">
      <div className="page-heading">
        <h1 id="production-title">{t("production.title")}</h1>
        <p>{t("production.description")}</p>
      </div>

      <FilterPanel
        filters={filters}
        onApply={(nextFilters) => {
          setPage(0);
          setFilters(nextFilters);
        }}
      />

      {trendQuery.isError || recordsQuery.isError ? (
        <p className="field-error" role="alert">
          {germanErrorMessage(trendQuery.error ?? recordsQuery.error)}
        </p>
      ) : null}

      <section className="panel" aria-labelledby="production-trend-title">
        <div className="section-heading">
          <h2 id="production-trend-title">{t("production.trend")}</h2>
          <p>{t("dashboard.chartFallbackHelp")}</p>
        </div>
        {trendQuery.isPending ? <p role="status">{t("dashboard.loading")}</p> : null}
        {trendQuery.data ? (
          <>
            <div className="chart-frame" aria-hidden="true">
              <ResponsiveContainer height={240} width="100%">
                <LineChart data={trendQuery.data.productionTrend} margin={{ bottom: 8, left: 0, right: 8, top: 8 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="bucketStart" tickFormatter={formatDateTime} />
                  <YAxis />
                  <Tooltip labelFormatter={(value) => formatDateTime(String(value))} />
                  <Legend />
                  <Line dataKey="unitsProduced" name={t("dashboard.producedUnits")} stroke="#2563eb" />
                </LineChart>
              </ResponsiveContainer>
            </div>
            <h3 className="fallback-title">{t("dashboard.tableFallback")}</h3>
            <table className="data-table compact-table">
              <thead>
                <tr>
                  <th>{t("dashboard.table.period")}</th>
                  <th>{t("dashboard.table.units")}</th>
                </tr>
              </thead>
              <tbody>
                {trendQuery.data.productionTrend.map((row) => (
                  <tr key={row.bucketStart}>
                    <th scope="row">{formatDateTime(row.bucketStart)}</th>
                    <td>{formatInteger(row.unitsProduced)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        ) : null}
      </section>

      <section className="panel" aria-labelledby="records-title">
        <div className="section-heading">
          <h2 id="records-title">{t("production.records")}</h2>
          <p>{t("production.traceability")}</p>
        </div>
        <a className="primary-button primary-link" href={productionEvidenceUrl(filters)}>
          {t("production.evidenceCsv")}
        </a>
        {recordsQuery.isPending ? <p role="status">{t("production.loadingRecords")}</p> : null}
        {recordsQuery.data ? (
          <>
            <table className="data-table compact-table traceability-table">
              <thead>
                <tr>
                  <th>{t("production.table.period")}</th>
                  <th>{t("production.table.units")}</th>
                  <th>{t("production.table.machine")}</th>
                  <th>{t("production.table.product")}</th>
                  <th>{t("production.table.batch")}</th>
                  <th>{t("production.table.importJob")}</th>
                </tr>
              </thead>
              <tbody>
                {recordsQuery.data.items.map((record) => (
                  <tr key={record.id}>
                    <th scope="row">
                      {formatDateTime(record.periodStart)} - {formatDateTime(record.periodEnd)}
                    </th>
                    <td>{formatInteger(record.unitsProduced)}</td>
                    <td>{record.machineId ?? "-"}</td>
                    <td>{record.productId ?? "-"}</td>
                    <td>{record.batchCode ?? "-"}</td>
                    <td>
                      <code>{record.importJobId}</code>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {recordsQuery.data.items.length === 0 ? <p role="status">{t("production.empty")}</p> : null}
            <div className="pagination-controls">
              <button
                className="secondary-button"
                disabled={page === 0}
                onClick={() => setPage((value) => Math.max(0, value - 1))}
                type="button"
              >
                {t("imports.history.previous")}
              </button>
              <button
                className="secondary-button"
                disabled={page >= recordsQuery.data.totalPages - 1}
                onClick={() => setPage((value) => value + 1)}
                type="button"
              >
                {t("imports.history.next")}
              </button>
            </div>
          </>
        ) : null}
      </section>
    </section>
  );
}
