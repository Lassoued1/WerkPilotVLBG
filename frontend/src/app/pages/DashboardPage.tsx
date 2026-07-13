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
import { useTranslation } from "react-i18next";

const metrics = [
  { labelKey: "dashboard.producedUnits", value: "128.400", unit: "Stk." },
  { labelKey: "dashboard.energyPerUnit", value: "1,84", unit: "kWh/Stk." },
  { labelKey: "dashboard.scrapRate", value: "2,10", unit: "%" },
  { labelKey: "dashboard.downtime", value: "312", unit: "Min." },
];

const trendRows = [
  { period: "Mo", output: 21400, downtime: 42 },
  { period: "Di", output: 23800, downtime: 35 },
  { period: "Mi", output: 22900, downtime: 58 },
  { period: "Do", output: 24700, downtime: 31 },
  { period: "Fr", output: 25600, downtime: 46 },
];

export function DashboardPage() {
  const { t } = useTranslation();

  return (
    <section className="page-stack" aria-labelledby="dashboard-title">
      <div className="page-heading">
        <h1 id="dashboard-title">{t("dashboard.title")}</h1>
        <p>{t("dashboard.description")}</p>
      </div>

      <div className="metric-grid">
        {metrics.map((metric) => (
          <article className="metric-card" key={metric.labelKey}>
            <span className="metric-label">{t(metric.labelKey)}</span>
            <strong>{metric.value}</strong>
            <span className="metric-unit">{metric.unit}</span>
          </article>
        ))}
      </div>

      <section className="panel" aria-labelledby="trend-title">
        <div className="section-heading">
          <h2 id="trend-title">{t("dashboard.sampleTrend")}</h2>
        </div>
        <div className="chart-frame" aria-hidden="true">
          <ResponsiveContainer height={260} width="100%">
            <BarChart
              data={trendRows}
              margin={{ bottom: 8, left: 0, right: 8, top: 8 }}
            >
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="period" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="output" fill="#2563eb" name="Einheiten" />
              <Bar dataKey="downtime" fill="#f97316" name="Stillstand" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <h3 className="fallback-title">{t("dashboard.tableFallback")}</h3>
        <table className="data-table">
          <thead>
            <tr>
              <th scope="col">Zeitraum</th>
              <th scope="col">Einheiten</th>
              <th scope="col">Stillstand</th>
            </tr>
          </thead>
          <tbody>
            {trendRows.map((row) => (
              <tr key={row.period}>
                <th scope="row">{row.period}</th>
                <td>{row.output.toLocaleString("de-AT")}</td>
                <td>{row.downtime} Min.</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </section>
  );
}
