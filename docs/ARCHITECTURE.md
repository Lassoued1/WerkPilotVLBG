# Architecture

## Architectural position

WerkPilot is one single-tenant, single-node web application for industrial
decision support. The backend is one deployable Java modular monolith. Its
business modules communicate in process through explicit application/domain
interfaces; they are not network services.

## System context

```mermaid
flowchart LR
    Admin[Plant administrator]
    Production[Production manager]
    Technician[Maintenance technician]
    Energy[Energy manager]
    Viewer[Viewer / auditor]
    CSV[Versioned UTF-8 CSV files]
    SMTP[Company SMTP relay]
    System[WerkPilot VLBG]

    Admin -->|Users, settings, imports, reports| System
    Production -->|Production imports, KPIs, anomalies, tickets| System
    Technician -->|Assigned tickets and machine history| System
    Energy -->|Energy imports, KPIs, reports, delegated thresholds| System
    Viewer -->|Read-only dashboards and reports| System
    CSV -->|Browser upload| System
    System -->|Password-reset email| SMTP
```

The system does not connect to PLC, SCADA, HMI, ERP, OPC UA, MQTT, Modbus, or
industrial gateways and never actuates a machine.

## Use-case view

```mermaid
flowchart LR
    A[ADMIN]
    P[PRODUCTION_MANAGER]
    M[MAINTENANCE_TECHNICIAN]
    E[ENERGY_MANAGER]
    V[VIEWER]

    subgraph WP[WerkPilot use cases]
      U1((Authenticate and reset password))
      U2((Manage users and roles))
      U3((Manage master data))
      U4((Import and correct CSV data))
      U5((Review KPI dashboard))
      U6((Review anomalies))
      U7((Manage maintenance tickets))
      U8((Generate and download reports))
      U9((Configure thresholds and settings))
      U10((Review audit events))
    end

    A --> U1
    A --> U2
    A --> U3
    A --> U4
    A --> U5
    A --> U6
    A --> U7
    A --> U8
    A --> U9
    A --> U10
    P --> U1
    P --> U4
    P --> U5
    P --> U6
    P --> U7
    P --> U8
    M --> U1
    M --> U5
    M --> U6
    M --> U7
    E --> U1
    E --> U4
    E --> U5
    E --> U6
    E --> U7
    E --> U8
    E -. delegation ON .-> U9
    V --> U1
    V --> U5
    V --> U6
    V --> U8
```

Every connection is constrained by the Section 8 permission matrix. The
backend is authoritative; this view does not grant permissions beyond it.

## Container and deployment view

```mermaid
flowchart TB
    Browser[Browser]

    subgraph Host[Single-node Docker Compose pilot host]
      Frontend[frontend\nnginx + React static build]
      Backend[backend\nJava 25 + Spring Boot 4.1.x]
      Postgres[(postgres\nPostgreSQL volume)]
      Reports[(report-files\nexternal volume)]
      Mailpit[mailpit\nlocal profile only]
    end

    Relay[Company SMTP relay\npilot profile]
    Browser -->|HTTPS, one origin| Frontend
    Frontend -->|/api reverse proxy| Backend
    Backend -->|JPA / JDBC| Postgres
    Backend -->|UUID-derived paths| Reports
    Backend -->|SMTP local| Mailpit
    Backend -->|SMTP pilot| Relay
```

- `postgres`: persistent PostgreSQL data; schema only through Liquibase.
- `backend`: Java 25 modular monolith, port 8080 internally.
- `frontend`: nginx static frontend and same-origin `/api` proxy.
- `mailpit`: local-only password-reset mail catcher.
- `report-files`: externalized report storage outside the web root, backed up
  with PostgreSQL and subject to 24-month operational retention.

Pilot deployment is single-node and business-hours oriented. High availability
and automated disaster recovery are out of scope.

## Backend component view

```mermaid
flowchart LR
    Web[REST controllers]
    Identity[identity]
    Master[masterdata]
    Importing[importing]
    Production[production]
    Energy[energy]
    Quality[quality]
    Downtime[downtime]
    Analytics[analytics]
    Maintenance[maintenance]
    Reporting[reporting]
    Audit[audit]
    Shared[shared API/error/time primitives]
    DB[(PostgreSQL)]
    Files[(report-files)]
    Mail[SMTP]

    Web --> Identity
    Web --> Master
    Web --> Importing
    Web --> Production
    Web --> Energy
    Web --> Quality
    Web --> Downtime
    Web --> Analytics
    Web --> Maintenance
    Web --> Reporting
    Importing --> Production
    Importing --> Energy
    Importing --> Quality
    Importing --> Downtime
    Importing --> Analytics
    Analytics --> Maintenance
    Reporting --> Production
    Reporting --> Energy
    Reporting --> Quality
    Reporting --> Downtime
    Reporting --> Analytics
    Reporting --> Maintenance
    Identity --> Mail
    Reporting --> Files
    Identity --> Audit
    Importing --> Audit
    Analytics --> Audit
    Maintenance --> Audit
    Reporting --> Audit
    Identity --> DB
    Master --> DB
    Importing --> DB
    Production --> DB
    Energy --> DB
    Quality --> DB
    Downtime --> DB
    Analytics --> DB
    Maintenance --> DB
    Reporting --> DB
    Audit --> DB
    Shared -. no feature business logic .- Web
```

The arrows describe allowed orchestration, not direct access to another
module's persistence package.

## Module ownership

| Module | Owns |
| --- | --- |
| `identity` | Authentication, users, fixed roles, refresh/reset tokens, mail orchestration. |
| `masterdata` | Factories, lines, machines, products, shifts, reasons, categories, global settings. |
| `importing` | Async jobs, hashing, strict parsing, validation, errors, correction/rollback lineage. |
| `production` | Production records and output KPIs. |
| `energy` | Energy measurements, granularity/XOR rules, thresholds, energy KPIs. |
| `quality` | Scrap records/categories and quality KPIs. |
| `downtime` | Downtime records/reasons, availability, Pareto output. |
| `analytics` | Detection identity, rerun/supersession, severity, recommendations. |
| `maintenance` | Tickets, assignment, comments, due date, computed overdue, transitions. |
| `reporting` | PDF rendering, CSV evidence, report metadata/files/download. |
| `audit` | Append-only events and ADMIN-only views. |
| `shared` | Error model, pagination, aggregate/filter/job records, validation and clock primitives. |

`shared` is not a dumping ground.

## Package dependency rules

```mermaid
flowchart TB
    API[api] --> APP[application]
    APP --> DOMAIN[domain]
    PERSIST[persistence adapter] --> DOMAIN
    APP --> PORTS[Explicit public module ports]
    PORTS --> OTHER[Other module application/domain]
```

- Controllers call application services, never repositories.
- Application services own use cases, transactions, orchestration, and ports.
- Domain owns entities, enums, policies, and pure calculations.
- Persistence implements repository ports; its types never cross modules/API.
- JPA entities never cross the REST boundary.
- Cross-module cycles and access to another module's persistence are forbidden.
- Architecture rules become executable tests in Sprint 0.

## Critical flows

### Asynchronous CSV import

1. Authorize the import type, enforce size, and compute SHA-256 while streaming.
2. Persist a `PROCESSING` job and return its shared job response.
3. Validate exact headers and all rows with bounded resources; resolve existing
   master-data codes without implicit creation.
4. Persist at most 500 German error details and mark FAILED, or atomically
   commit every row and mark COMMITTED.
5. Run deterministic post-import analytics after commit.
6. For correction/rollback, atomically supersede the target job and rerun
   analytics so anomaly supersession follows Section 23.4.

### Dashboard

1. Parse and authorize one typed filter object.
2. Query only rows belonging to COMMITTED jobs and fully contained in the
   canonical `[from,to)` window.
3. Calculate authoritative KPIs in pure backend services.
4. Return shared aggregate values and applied filters.
5. The frontend only formats and visualizes the values.

### Password reset

1. Return the same `202` regardless of account existence.
2. When appropriate, hash/store a one-time 60-minute token and send a German
   link whose token is in the URL fragment.
3. Confirmation consumes the token, changes the BCrypt password, revokes all
   refresh tokens, and writes the required audit event.

### Monthly report

1. Persist a report run and normalized filters.
2. Obtain projections from module application ports.
3. Render the fixed PDF with the exact disclaimer.
4. Store it under a UUID-derived path in `report-files` and audit generation.
5. Download only through authorization; return `404` if the file is absent and
   never silently regenerate it.

## Cross-cutting rules

- Store timestamps in UTC; display Europe/Vienna and inject `Clock`.
- Use structured JSON pilot logs with trace/user IDs when available.
- Protect Actuator and OpenAPI outside local development.
- Centralize sanitized errors in `@ControllerAdvice`.
- Generate OpenAPI 3.1 and use it to type frontend access.
- Keep all secrets outside source control and frontend/image layers.
