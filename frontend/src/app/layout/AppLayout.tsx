import { NavLink, Outlet } from "react-router";
import { useTranslation } from "react-i18next";

import { apiBaseUrl } from "../../shared/api/client";

const navigationItems = [
  { to: "/", labelKey: "nav.dashboard" },
  { to: "/imports", labelKey: "nav.imports" },
  { to: "/master-data", labelKey: "nav.masterData" },
  { to: "/maintenance", labelKey: "nav.maintenance" },
  { to: "/reports", labelKey: "nav.reports" },
  { to: "/administration", labelKey: "nav.administration" },
];

export function AppLayout() {
  const { t } = useTranslation();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <span className="brand-mark" aria-hidden="true">
            WP
          </span>
          <div>
            <div className="brand-name">{t("appName")}</div>
            <div className="brand-subtitle">{t("appSubtitle")}</div>
          </div>
        </div>

        <nav className="nav-list" aria-label="Hauptnavigation">
          {navigationItems.map((item) => (
            <NavLink
              className={({ isActive }) =>
                isActive ? "nav-link nav-link-active" : "nav-link"
              }
              end={item.to === "/"}
              key={item.to}
              to={item.to}
            >
              {t(item.labelKey)}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="content-shell">
        <header className="topbar">
          <div className="status-line">
            <span>{t("shell.routeReady")}</span>
            <span>{t("shell.apiStatus", { baseUrl: apiBaseUrl })}</span>
          </div>
        </header>
        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
