import { lazy, Suspense, type ReactNode } from "react";
import { createBrowserRouter, createMemoryRouter } from "react-router";

import { AppLayout } from "./layout/AppLayout";

const DashboardPage = lazy(async () => {
  const module = await import("../features/dashboard/DashboardPage");
  return { default: module.DashboardPage };
});

const MachinesPage = lazy(async () => {
  const module = await import("../features/machines/MachinesPage");
  return { default: module.MachinesPage };
});

const ProductionPage = lazy(async () => {
  const module = await import("../features/production/ProductionPage");
  return { default: module.ProductionPage };
});

const ImportsPage = lazy(async () => {
  const module = await import("../features/imports/ImportsPage");
  return { default: module.ImportsPage };
});

const AuthPage = lazy(async () => {
  const module = await import("../features/auth/AuthPage");
  return { default: module.AuthPage };
});

const AdminPage = lazy(async () => {
  const module = await import("../features/admin/AdminPage");
  return { default: module.AdminPage };
});

const MasterDataPage = lazy(async () => {
  const module = await import("../features/masterdata/MasterDataPage");
  return { default: module.MasterDataPage };
});

const AnomaliesPage = lazy(async () => {
  const module = await import("../features/anomalies/AnomaliesPage");
  return { default: module.AnomaliesPage };
});

const PlaceholderPage = lazy(async () => {
  const module = await import("./pages/PlaceholderPage");
  return { default: module.PlaceholderPage };
});

const NotFoundPage = lazy(async () => {
  const module = await import("./pages/NotFoundPage");
  return { default: module.NotFoundPage };
});

function pageElement(element: ReactNode) {
  return (
    <Suspense
      fallback={
        <p className="route-loading" role="status">
          Bereich wird geladen...
        </p>
      }
    >
      {element}
    </Suspense>
  );
}

export const routes = [
  {
    element: <AppLayout />,
    children: [
      { index: true, element: pageElement(<DashboardPage />) },
      { path: "login", element: pageElement(<AuthPage />) },
      { path: "machines", element: pageElement(<MachinesPage />) },
      { path: "production", element: pageElement(<ProductionPage />) },
      { path: "imports", element: pageElement(<ImportsPage />) },
      { path: "master-data", element: pageElement(<MasterDataPage />) },
      { path: "anomalies", element: pageElement(<AnomaliesPage />) },
      {
        path: "maintenance",
        element: pageElement(
          <PlaceholderPage
            bodyKey="placeholders.maintenance"
            titleKey="nav.maintenance"
          />,
        ),
      },
      {
        path: "reports",
        element: pageElement(
          <PlaceholderPage
            bodyKey="placeholders.reports"
            titleKey="nav.reports"
          />,
        ),
      },
      { path: "administration", element: pageElement(<AdminPage />) },
      { path: "*", element: pageElement(<NotFoundPage />) },
    ],
  },
];

export function createAppRouter() {
  return createBrowserRouter(routes);
}

export function createTestRouter(initialEntries = ["/"]) {
  return createMemoryRouter(routes, { initialEntries });
}
