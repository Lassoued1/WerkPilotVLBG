import { lazy, Suspense, type ReactNode } from "react";
import { createBrowserRouter, createMemoryRouter } from "react-router";

import { AppLayout } from "./layout/AppLayout";

const DashboardPage = lazy(async () => {
  const module = await import("./pages/DashboardPage");
  return { default: module.DashboardPage };
});

const ImportsPage = lazy(async () => {
  const module = await import("./pages/ImportsPage");
  return { default: module.ImportsPage };
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
      { path: "imports", element: pageElement(<ImportsPage />) },
      {
        path: "master-data",
        element: pageElement(
          <PlaceholderPage
            bodyKey="placeholders.masterData"
            titleKey="nav.masterData"
          />,
        ),
      },
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
      {
        path: "administration",
        element: pageElement(
          <PlaceholderPage
            bodyKey="placeholders.administration"
            titleKey="nav.administration"
          />,
        ),
      },
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
