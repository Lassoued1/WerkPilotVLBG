# Execution Plan: WP-S0-07 - Docker Compose, Profiles and Volumes

- Status: In review
- Owner: Codex / Mohamed
- Started: 2026-07-07
- Last updated: 2026-07-07
- Backlog item: WP-S0-07 Docker Compose, profiles and volumes
- Requirement IDs: NFR-01, OQ-008
- Source sections: 13, 26.3, 29

## Goal and observable outcome

Provide a single-node Docker Compose baseline that starts PostgreSQL, the
Spring Boot backend, an nginx-served static frontend, local Mailpit for local
mail capture, persistent PostgreSQL storage, external `report-files` storage,
and health checks suitable for the Sprint 0 deployment gate.

## In scope

- Root Compose file with local/test/pilot profile support.
- Persistent `postgres-data` and `report-files` volumes.
- Backend container build and runtime database wiring.
- Frontend nginx container build and `/api` reverse proxy wiring, without
  changing frontend application source.
- Local Mailpit service.
- Health checks for PostgreSQL, backend, frontend, and Mailpit.
- Environment sample for local and pilot-oriented configuration.

## Out of scope

- Frontend feature development; frontend app source remains owned by VS Code.
- Production TLS termination and certificate management.
- SMTP relay implementation beyond environment wiring.
- Backup/restore scripts, which are planned for WP-S5-04.

## Current-state findings

- No Docker or Compose files exist yet.
- Backend has PostgreSQL/Liquibase baseline but no Actuator dependency.
- Gate D expects `docker compose config`, stack startup, and
  `curl http://localhost:8080/actuator/health`.
- Architecture requires one-origin nginx frontend with `/api` reverse proxy,
  PostgreSQL, backend, local Mailpit, and external `report-files`.

## Normative decisions and source conflicts

- DOCX v2.1 remains authoritative.
- Keep Mailpit local-only by profile and environment intent; pilot uses company
  SMTP variables instead of Mailpit.
- `report-files` is mounted outside the web root and into the backend only.
- No source conflict was found.

## Milestones

### Milestone 1 - Compose and image build files

- [x] Add Dockerfiles for backend and nginx/static frontend deployment.
- [x] Add nginx config with SPA fallback and `/api` proxy.
- [x] Add root `.dockerignore`.

### Milestone 2 - Runtime configuration

- [x] Add `docker-compose.yml` with PostgreSQL, backend, frontend, Mailpit,
  volumes, health checks, and profiles.
- [x] Add `.env.example` documenting local/test/pilot environment values.
- [x] Add backend Actuator health exposure required by Gate D.

### Milestone 3 - Verification

- [x] Run `docker compose config`.
- [x] Run backend verification if backend config changed.
- [x] Run Gate D startup when feasible.
- [x] Update this plan with exact results.

## Test and acceptance matrix

| Behavior | Test level | Test/file | Expected result |
| --- | --- | --- | --- |
| Compose parses and resolves services | Deployment | `docker compose config` | Pass |
| Backend health endpoint exists | Integration | `mvn clean verify` | Pass |
| PostgreSQL volume and report volume declared | Deployment | Compose config | Pass |
| nginx proxies `/api` to backend | Config review | nginx config | Pass |

## Risks and rollback

- Data/migration risk: low; Compose introduces persistent local volumes but no
  business schema beyond existing Liquibase baseline.
- Security risk: local defaults are sample-only; `.env` remains ignored and
  secrets must not be committed.
- Compatibility risk: Docker image builds may need network access to pull base
  images and download dependencies.
- Rollback or safe recovery: revert Compose/Docker/env files, Actuator
  dependency/config, and this execution plan.

## Progress log

- 2026-07-07 - Started WP-S0-07 after user confirmed WP-S0-06 is handled in VS
  Code.
- 2026-07-07 - Confirmed this task is IntelliJ/repo deployment work and frontend
  application source is out of scope.
- 2026-07-07 - Added `.dockerignore`, backend Dockerfile, frontend nginx
  Dockerfile, nginx reverse-proxy config, `docker-compose.yml`, `.env.example`,
  README local stack instructions, and backend Actuator health exposure.
- 2026-07-07 - `mvn clean verify` passed with 14 tests, 0 failures, 0 errors;
  Actuator exposed one endpoint under `/actuator`.
- 2026-07-07 - `docker compose config` is unavailable in this environment
  because the Docker CLI plugin command is not recognized; `docker-compose
  config` passed.
- 2026-07-07 - `docker-compose --profile local config` passed and showed
  Mailpit in the local/test profiles.
- 2026-07-07 - Initial `docker-compose --profile local up -d --build` timed out
  during image build after the backend image was created. The suspended
  `docker-compose` process was stopped, `docker-compose build frontend` passed,
  then `docker-compose --profile local up -d` passed.
- 2026-07-07 - Gate D health checks passed: PostgreSQL, backend, frontend, and
  Mailpit containers were all `healthy`; backend returned
  `{"groups":["liveness","readiness"],"status":"UP"}`, frontend `/healthz`
  returned `ok`, and Mailpit `/livez` returned HTTP 200.
- 2026-07-07 - nginx frontend root `http://localhost:8081/` returned HTTP 200.

## Decision log

- 2026-07-07 - Use deployment Dockerfiles and nginx config outside frontend app
  source so WP-S0-07 does not modify VS Code-owned frontend feature code.

## Unexpected findings

- This Windows Docker installation exposes Compose as `docker-compose.exe`
  v5.1.4; `docker compose` returned `unknown command`.
- `docker-compose --profile local up -d --build` exceeded the first 5-minute
  timeout while pulling/building images, but separate frontend build plus stack
  startup completed successfully.

## Final verification

- [x] Focused tests: `docker-compose config` and
  `docker-compose --profile local config` passed.
- [x] Backend `clean verify` when affected: `mvn clean verify` passed with 14
  tests, 0 failures, 0 errors.
- [x] Frontend lint/test/build when affected: frontend application source was
  not changed in WP-S0-07; Docker frontend image build ran `npm run build`
  successfully.
- [x] Docker/health/E2E checks when affected:
  `docker-compose --profile local up -d` passed after images were built;
  `docker-compose --profile local ps` showed all four services healthy;
  `curl.exe -fsS http://localhost:8080/actuator/health` returned `UP`;
  frontend `/healthz` returned `ok`; frontend `/` returned HTTP 200; Mailpit
  `/livez` returned HTTP 200.
- [x] Diff reviewed for unrelated changes: repository remains pre-initial
  commit; WP-S0-07 changes are scoped to backend health config, deployment
  files, README, and this plan. Frontend application source was not edited.
- [x] Documentation and traceability updated: README and this execution plan
  updated.

## Handoff

WP-S0-07 is implemented and ready for Mohamed review. The local Docker stack is
currently running with PostgreSQL on 5432, backend on 8080, nginx/frontend on
8081, Mailpit SMTP on 1025, Mailpit UI on 8025, and named volumes
`postgres-data` and `report-files`. The next Sprint 0 task is WP-S0-08 CI,
observability and seed contract.
