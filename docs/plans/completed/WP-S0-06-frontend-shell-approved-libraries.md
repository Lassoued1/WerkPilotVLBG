# Execution Plan: WP-S0-06 - Frontend Shell and Approved Libraries

- Status: Completed
- Owner: Codex VSC / Mohamed
- Started: 2026-07-06
- Last updated: 2026-07-07
- Backlog item: WP-S0-06 Frontend shell and approved libraries
- Requirement IDs: NFR-07, OQ-010, OQ-013
- Source sections: 17.4, 25.2

## Goal and observable outcome

Create a German React TypeScript application shell with approved runtime
libraries, stable routing, query/client providers, form wiring, chart fallback
patterns, OpenAPI client access, and focused route smoke tests.

## In scope

- React app composition with router, TanStack Query, i18n, and strict
  TypeScript boundaries.
- Route shell for dashboard, imports, master data, maintenance, reports, and
  administration placeholders.
- Stable unknown-route behavior with German not-found messaging.
- German UI catalog and frontend-owned user messages.
- Shared OpenAPI fetch client and typed API client placeholder.
- Form and chart components that establish approved implementation patterns.
- Vitest and Testing Library smoke coverage for the shell.

## Out of scope

- Business endpoint implementation.
- Authentication, RBAC enforcement, and protected route behavior.
- Generated OpenAPI types for future business endpoints.
- Browser E2E and Docker Compose gates.

## Current-state findings

- `frontend/src/main.tsx` contains only a placeholder component.
- `frontend/package.json` has React, React DOM, Vite, TypeScript, and Vitest.
- OQ-010 fixes approved runtime libraries:
  `react-router`, `@tanstack/react-query`, `react-hook-form`, `recharts`,
  `i18next`, `react-i18next`, `openapi-typescript`, and `openapi-fetch`.
- TEST_STRATEGY requires Testing Library coverage for frontend components.

## Normative decisions and source conflicts

- DOCX v2.1 remains authoritative.
- UI labels and general user-visible text are German per OQ-013.
- Frontend must not recalculate backend KPI formulas; shell charts use display
  values only.
- No source conflict was found.

## Milestones

### Milestone 1 - Dependencies and test setup

- [x] Add approved frontend runtime libraries.
- [x] Add Testing Library and jsdom test setup.
- [x] Verify install updates `package-lock.json`.

### Milestone 2 - Application shell

- [x] Add app providers, route definitions, German catalog, and OpenAPI client.
- [x] Add shell layout, navigation, dashboard placeholder, import form pattern,
  and chart fallback pattern.
- [x] Keep business behavior deferred to later sprint tasks.

### Milestone 3 - Verification

- [x] Add route smoke tests.
- [x] Run `npm run lint`.
- [x] Run `npm test -- --run src/app`.
- [x] Run `npm run build`.

## Test and acceptance matrix

| Behavior | Test level | Test/file | Expected result |
| --- | --- | --- | --- |
| App renders German shell labels | Component | `src/app/App.test.tsx` | Pass |
| Navigation exposes planned route areas | Component | `src/app/App.test.tsx` | Pass |
| Import form validates required file input | Component | `src/app/App.test.tsx` | Pass |
| Chart pattern has table fallback | Component | `src/app/App.test.tsx` | Pass |
| Unknown route renders German not-found page | Component | `src/app/App.test.tsx` | Pass |

## Risks and rollback

- Data/migration risk: none.
- Security risk: authentication is deliberately deferred; do not imply
  authorization enforcement in the shell.
- Compatibility risk: current npm dependency resolution may require network.
- Rollback or safe recovery: revert frontend dependency changes, `src/`
  shell files, test setup, and this plan.

## Progress log

- 2026-07-06 - Started WP-S0-06 after WP-S0-05 handoff.
- 2026-07-06 - Confirmed approved frontend libraries from OQ-010 and German UI
  requirement from OQ-013.
- 2026-07-06 - Added approved runtime libraries plus Testing Library, jsdom, and
  Node types.
- 2026-07-06 - Replaced the placeholder `main.tsx` with app providers,
  German i18n catalog, React Router routes, OpenAPI fetch client, dashboard
  display shell, import form pattern, and route placeholders.
- 2026-07-06 - Added `App.test.tsx` smoke tests for German shell labels,
  planned navigation, chart table fallback, and import form validation.
- 2026-07-06 - `npm test -- --run src/app` passed with 3 tests.
- 2026-07-06 - Full frontend gate after `npm ci` passed: lint, test, and build.
- 2026-07-06 - Started Vite dev server on `http://127.0.0.1:5173/` and verified
  the root page returns HTTP 200.
- 2026-07-07 - Codex VSC resumed ownership of WP-S0-06 after it had been
  started from IntelliJ by mistake; confirmed the task is frontend-only and
  does not affect backend ownership.
- 2026-07-07 - Added per-application `QueryClient` and router isolation,
  route-level lazy loading, an explicit German not-found route, and a navigation
  landmark.
- 2026-07-07 - Re-ran the current frontend gate: `npm run lint`,
  `npm test -- --run src/app`, and `npm run build` all passed.

## Decision log

- 2026-07-06 - Use shell placeholders for future business routes instead of
  inventing endpoint behavior before Sprint 1+ contracts.
- 2026-07-07 - Use route-level lazy loading for page areas so approved runtime
  libraries do not inflate the initial shell chunk.

## Unexpected findings

- `rg` is not available in this PowerShell environment.
- `npm test -- --run src/app` initially exposed missing Testing Library cleanup
  between tests; `src/test/setup.ts` now performs explicit cleanup.
- `lib: ESNext` caused an `Iterator` type conflict in dependency declarations;
  TypeScript now uses `ES2022`, DOM libs, and `ESNext.Disposable`.
- The earlier Vite chunk-size warning was resolved by route-level code
  splitting; `DashboardPage` and `ImportsPage` now build as separate chunks.

## Final verification

- [x] Focused tests: `npm test -- --run src/app` passed with 4 tests, 0
  failures.
- [x] Backend `clean verify` when affected: not affected by WP-S0-06.
- [x] Frontend lint/test/build when affected: `npm run lint`,
  `npm test -- --run src/app`, and `npm run build` passed on 2026-07-07.
  Build emitted no chunk-size warning after route-level code splitting.
- [x] Docker/health/E2E checks when affected: not affected by WP-S0-06.
- [x] Diff reviewed for unrelated changes: current VSC changes were scoped to
  frontend shell hardening and this completed plan.
- [x] Documentation and traceability updated: this execution plan updated.

## Handoff

WP-S0-06 is complete from the VSC/frontend side. The frontend now has the
approved libraries, German application shell, prepared route areas, OpenAPI
client base, form and chart patterns, route-level code splitting, a German
not-found route, jsdom test setup, and route smoke tests. WP-S0-07 remains an
IntelliJ-owned Docker Compose task, with VSC providing frontend/nginx support
only if requested.
