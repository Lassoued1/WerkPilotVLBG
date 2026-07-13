# 📋 PLAN D'ACTION - WerkPilot VLBG

**Date** : 2026-07-06  
**Projet** : WerkPilot VLBG - Plateforme de Support Décisionnel  
**Autorité Contractuelle** : `WerkPilot_VLBG_Cahier_des_Charges_v2.1.docx`

---

## 🎯 STRATÉGIE GLOBALE

### Vision
WerkPilot VLBG est une plateforme **mono-tenant, mono-nœud** de support décisionnel pour PME manufacturières. Aucune capacité d'actuation machine, pas d'intégration PLC/SCADA.

### Portée Fixe
- **12 modules métier** (S-01 à S-12)
- **5 rôles RBAC** (ADMIN, PRODUCTION_MANAGER, MAINTENANCE_TECHNICIAN, ENERGY_MANAGER, VIEWER)
- **106 IDs explicites** (BO, S, OOS, VAL, MD, PRD, ENE, EC, DTS, MNT, AN, REP, NFR, TEST, ACC)
- **Toutes OQ résolues** : OQ-001 à OQ-013 intégrées dans DOCX v2.1 Section 36.3

### Principes Non-Négociables
1. ✅ Le DOCX v2.1 est l'**autorité absolue**
2. ✅ Aucune OQ rouverte - toutes sont résolues
3. ✅ Change control : Sections 36.1-36.2 du DOCX
4. ✅ Arrêt approval **Mohamed à chaque sprint exit**
5. ✅ Preuves de livraison : tests, gates, acceptance criteria
6. ✅ Zero business-logic outside the DOCX

---

## 🏗️ ARCHITECTURE TECHNIQUE

### Stack Approuvé
| Layer | Technology | Version |
|-------|-----------|---------|
| **Backend** | Java | 25 |
| | Spring Boot | 4.1.x |
| | Build | Maven Wrapper |
| **Frontend** | React | 18+ |
| | TypeScript | Latest |
| | Bundler | Vite |
| **Database** | PostgreSQL | Latest |
| | Migrations | Liquibase |
| **Deployment** | Docker Compose | v2+ |
| **Email** | Mailpit (local) | Latest |
| | SMTP Relay (pilot) | Company SMTP |

### Modules Métier (12)
1. **S-01 Identity** → Login, refresh, 5 rôles, reset email
2. **S-02 Master Data** → Factories, lines, machines, products, shifts
3. **S-03 CSV Import** → Upload strict, validation atomique, correction, rollback
4. **S-04 Production** → KPI, traçabilité, tendances
5. **S-05 Energy** → kWh, énergie/unité, anomalies
6. **S-06 Scrap/Downtime** → Taux, Pareto, patterns
7. **S-07 Analytics** → Z-scores, explications, recommandations
8. **S-08 Maintenance** → Tickets, transitions, due dates
9. **S-09 Dashboard** → Cartes, filtres, KPI (perf: 500k rows en 5s)
10. **S-10 Reports** → PDF/CSV mensuels, disclaimer allemand
11. **S-11 Audit** → Append-only, 36 mois rétention
12. **S-12 Administration** → Users, seuils, calendrier

### Rôles RBAC (Backend Authoritative)
- **ADMIN** → Tous droits
- **PRODUCTION_MANAGER** → Imports prod, KPI, tickets
- **MAINTENANCE_TECHNICIAN** → Tickets assignés
- **ENERGY_MANAGER** → Imports énergie, KPI, délégation optionnelle
- **VIEWER** → Read-only

---

## 🛠️ STRATÉGIE IDE

### Frontend
**IDE** : VS Code  
**Agent** : GitHub Copilot (Codex)  
**Avantages** :
- Léger, ultra-rapide
- Vite HMR instantané
- Excellent TypeScript support
- Playwright E2E fluide

### Backend
**IDE** : IntelliJ IDEA Community (gratuit)  
**Agent** : GitHub Copilot (Codex)  
**Avantages** :
- Spring Boot magic
- Debugging avancé
- JPA/Hibernate insights
- Refactorings de qualité

### Communication
```
Frontend (VS Code :5173)
        ↓
  /api reverse proxy
        ↓
Backend (IntelliJ :8080)
        ↓
PostgreSQL (Compose)
```

---

## 📊 FORMULES KPI CRITIQUES

| KPI | Formule | Cas Limites |
|-----|---------|------------|
| **Total énergie** | `SUM(energy_kwh)` | 1 décimale |
| **Unités produites** | `SUM(units_produced)` | Entier |
| **Énergie/unité** | `SUM(kwh) / NULLIF(SUM(units),0)` | N/A si units=0 ; 2 décimales |
| **Taux rebut** | `SUM(scrap) / NULLIF(SUM(units+scrap),0)*100` | 2 décimales |
| **Downtime** | `SUM(downtime_min)` | Minutes entières |
| **Disponibilité** | `(planned_min - downtime_min) / planned_min * 100` | Nécessite shift plan |
| **Anomalies** | Compte non-dismissed | Regrouper par sévérité |
| **Backlog maint.** | OPEN + IN_PROGRESS | **OVERDUE = query-time bool, PAS un statut** |

---

## 🔐 CONTRATS CLÉS RÉSOLUS

### 🔑 Authentification (OQ-001 ✓)
- **Refresh token** : HttpOnly, Secure, SameSite=Strict (12h)
- **Access token** : JSON + Bearer header (15 min)
- **CSRF** : Requis refresh & logout
- **Reset** : Fragment-URL token, 128-bit, 60 min, single-use, allemand

### 📤 Import CSV (OQ-009 ✓)
- UTF-8, virgule, point décimal, header obligatoire
- Max 25 MB, 100k lignes
- **Validation atomique** : PROCESSING → COMMITTED/FAILED
- **Erreurs** : Max 500 + overflow metadata (allemand)
- **Correction** : Job COMMITTED → remplace + lineage + rerun analytics
- **Rollback** : Raison + pas de remplacement

### ⏱️ Temps & Granularité (OQ-005 ✓)
- Intervals : Half-open `[start, end)`, UTC stockage, Vienna affichage
- **Energy** : Machine **XOR** Line, jamais mix granularités période chevauchée
- **KPI** : Agrégation entièrement contenue, **pas de proration**

### 🔍 Analytics Déterministe (OQ-007 ✓)
- **Baseline** : 30 dernières périodes (min 10 pour z-score)
- **Sévérité** : MEDIUM ≥2.0, HIGH ≥3.0, CRITICAL ≥4.0
- **Rerun** : Clé identité fixe → no-op/changed/disappeared → SUPERSEDED (terminal)
- **Recommandations** : Versionnées, déterministes

### 📄 Rapports & Rétention (OQ-008 ✓)
- **Stockage** : Volume externe UUID-driven
- **Missing file** : 404 (jamais silent regen)
- **Rétention** : 24 mois opérationnel, 36 mois audit
- **PAS de scheduler** → purge manuelle safeguarded + audit
- **Disclaimer allemand** : Anomalies, tickets, PDF

---

## 🔌 ENDPOINTS API PRINCIPAUX

### Authentification
```
POST   /auth/login
POST   /auth/refresh               (CSRF header)
POST   /auth/logout                (CSRF header)
GET    /auth/me
POST   /auth/password-reset-request
POST   /auth/password-reset-confirm
```

### CSV Import (4 types)
```
POST   /import-jobs/production-records      (ADMIN, PROD_MANAGER)
POST   /import-jobs/energy-measurements    (ADMIN, ENERGY_MANAGER)
POST   /import-jobs/downtime-records       (ADMIN, PROD_MANAGER)
POST   /import-jobs/scrap-records          (ADMIN, PROD_MANAGER)
GET    /import-jobs/{id}/errors            (max 500 + overflow)
POST   /import-jobs/{id}/correction        (COMMITTED target)
POST   /import-jobs/{id}/rollback          (reason required)
```

### KPI & Analytics
```
GET    /production/kpis
GET    /energy/kpis
GET    /dashboard/summary                   (perf: 500k en 5s)
GET    /downtime/pareto
GET    /quality/scrap-rate
GET    /anomalies, PATCH /anomalies/{id}/status
```

### Maintenance & Rapports
```
GET, POST, PATCH   /maintenance-tickets
POST               /maintenance-tickets/{id}/comments
POST               /reports/monthly
GET                /reports/{id}/download
```

---

## 🗓️ BACKLOG D'IMPLÉMENTATION (6 SPRINTS)

### **SPRINT 0 - Architecture Baseline** (Ready/Planned)
**Objectif** : Skeleton exécutable, rules architecturales testables, B/F/D gates passent

| Task | Source | Deliverable | Key Proof |
|------|--------|-------------|-----------|
| WP-S0-01 | Repo + toolchain | Git, Maven Wrapper, TypeScript Vite | `validate-agent-kit.ps1` |
| WP-S0-02 | Architecture views | Context, use-case, container, component, deployment | 106-ID traceabilité |
| WP-S0-03 | PostgreSQL + Liquibase | Testcontainer, root changelog, empty-db migration test | `LiquibaseMigrationIT` |
| WP-S0-04 | Modular packages | `com.werkpilot` modules + dependency rules | `ArchitectureRulesTest` |
| WP-S0-05 | Shared API/OpenAPI | Error, pagination, aggregate, filter, job shapes | `SharedApiContractIT` |
| WP-S0-06 | Frontend shell | Router, React Query, forms, charts, i18n, libs | `npm test -- --run src/app` |
| WP-S0-07 | Docker Compose | local/test/pilot profiles, volumes, health checks | Gate D |
| WP-S0-08 | CI + observability | JSON logs, protected Actuator, seed, fixtures | B + F gates |

**Exit Criteria** : B, F, D pass ; migrations apply ; architecture rules executable ; approval Mohamed

---

### **SPRINT 1 - Identity & Master Data** (Planned)
**Objectif** : Auth complet, RBAC 5 rôles, master-data CRUD

| Task | Source | Deliverable | Key Proof |
|------|--------|-------------|-----------|
| WP-S1-01 | OQ-001 | BCrypt, tokens, refresh rotate, logout, CSRF, CORS | `AuthenticationFlowIT` |
| WP-S1-02 | §8, TEST-03 | 5 rôles, backend authorization, user admin | `UserAdministrationIT` |
| WP-S1-03 | OQ-002, §27.4 | Enumeration-safe, Mailpit/SMTP, 60-min token, reset confirm | `PasswordResetFlowIT` |
| WP-S1-04 | MD-01..08 | Factories, lines, machines, products, shifts, reasons | `MasterDataCrudIT` |
| WP-S1-05 | OQ-003, §27.2 | `system_settings`, delegation toggle, authorization audit | `EnergyThresholdDelegationIT` |
| WP-S1-06 | S-11, §27.2 | Append-only audit, ADMIN query, required events | `AuditPersistenceIT` |
| WP-S1-07 | §25, ACC-02 | German login/reset/admin/master-data screens | `npm test -- --run src/features/auth` |

**Exit Criteria** : Auth/refresh/logout/reset pass ; RBAC enforcement ; master-data ops complete ; B/F gates pass ; approval Mohamed

---

### **SPRINT 2 - Asynchronous CSV Import & Correction** (Planned)
**Objectif** : 4 templates CSV, validation atomique, correction, rollback, 100k benchmark

| Task | Source | Deliverable | Key Proof |
|------|--------|-------------|-----------|
| WP-S2-01 | OQ-009 | PROCESSING/COMMITTED/FAILED/SUPERSEDED, hash, 500-error cap | `AsyncImportJobIT` |
| WP-S2-02 | VAL-01..04 | UTF-8, comma, dot, headers, UTC, German row details | `CsvTemplateValidationTest` |
| WP-S2-03 | PRD-01 | Production template, atomic commit/reject, traceability | `ProductionCsvImportIT` |
| WP-S2-04 | OQ-005, ENE-01 | Energy template, machine/line XOR, granularity constraints | `EnergyCsvImportIT` |
| WP-S2-05 | DTS-01, DTS-04 | Downtime/scrap templates, duration/reason/category | `DowntimeCsvImportIT` |
| WP-S2-06 | OQ-006, §24.3 | Correction atomicity, rollback reason, lineage, active-row filtering | `CorrectionImportIT` |
| WP-S2-07 | S-03, §25 | Upload UI, React Query polling, history, German errors | `npm test -- --run src/features/imports` |
| WP-S2-08 | §34, NFR-03 | 4 fixture families (valid/invalid), 100k benchmark | `LargeCsvImportBenchmarkIT` |

**Exit Criteria** : All 4 templates pass atomic flows ; correction/rollback preserve lineage ; 100k evidence recorded ; B/F gates pass ; approval Mohamed

---

### **SPRINT 3 - KPI APIs & Dashboard** (Planned)
**Objectif** : KPI calculations, time/filter policies, dashboard 500k perf target

| Task | Source | Deliverable | Key Proof |
|------|--------|-------------|-----------|
| WP-S3-01 | §4.2, PRD-02..05 | Pure production, energy/unit, scrap, downtime, availability KPI services | `KpiCalculationServiceTest` |
| WP-S3-02 | OQ-005, EC-01..04 | Canonical `[from,to)` windows, fully contained rows, no proration | `TimeWindowAggregationIT` |
| WP-S3-03 | PRD-02..06, ENE-02..04 | Production/energy records, trends, totals, top 10, CSV export | `ProductionApiIT` |
| WP-S3-04 | DTS-02..05 | Downtime/scrap totals, Pareto, scrap rate, filters | `DowntimeApiIT` |
| WP-S3-05 | S-09, NFR-02 | Dashboard summary, shared filters, aggregates, indexes | `DashboardPerformanceIT` |
| WP-S3-06 | §25.1, ACC-05 | German cards/filters/charts/tables, machine/record views | `npm test -- --run src/features/dashboard` |

**Exit Criteria** : Fixture KPIs exact match ; 500k dashboard target passes ; B/F gates pass ; approval Mohamed

---

### **SPRINT 4 - Analytics & Maintenance** (Planned)
**Objectif** : Anomalies déterministes, recommandations, maintenance tickets, anomaly→ticket workflow

| Task | Source | Deliverable | Key Proof |
|------|--------|-------------|-----------|
| WP-S4-01 | S-12, OQ-003 | Threshold CRUD, delegation authorization, audit | `ThresholdAdministrationIT` |
| WP-S4-02 | AN-01, AN-03 | Baselines, threshold fallback, z-scores, severity, import trigger | `AnomalyDetectionServiceTest` |
| WP-S4-03 | OQ-007, §23.4 | Rerun identity, no-op/changed/disappeared behavior, SUPERSEDED | `AnalyticsRerunIT` |
| WP-S4-04 | AN-05, CR-001 | Versioned templates, exact German disclaimer | `RecommendationServiceTest` |
| WP-S4-05 | AN-03, AN-04 | Anomaly filters/detail/status/audit, German display, disclaimer | `npm test -- --run src/features/anomalies` |
| WP-S4-06 | MNT-01..07, OQ-012 | Lifecycle, assignment, comments, due-date, computed overdue | `MaintenanceTicketLifecycleIT` |
| WP-S4-07 | MNT-02, AN-04 | Anomaly→ticket linked creation, machine history, pattern detection | `AnomalyToTicketIT` |
| WP-S4-08 | §25, ACC-07 | German assigned-first list/detail, transitions, overdue badge | `npm test -- --run src/features/maintenance` |

**Exit Criteria** : Abnormal fixtures create deterministic anomalies ; reruns preserve history ; ticket workflow complete ; B/F gates pass ; approval Mohamed

---

### **SPRINT 5 - Reporting, Operations, Hardening, Acceptance** (Planned)
**Objectif** : PDF/CSV rapports, backup/restore/purge, security complete, E2E acceptance

| Task | Source | Deliverable | Key Proof |
|------|--------|-------------|-----------|
| WP-S5-01 | REP-01..03 | PDFBox fixed sections, exact disclaimer, matching CSV evidence | `MonthlyReportServiceIT` |
| WP-S5-02 | OQ-008, §26.3 | UUID-derived external storage, authorized download, 404 missing-file | `ReportStorageIT` |
| WP-S5-03 | S-10, S-11 | Report generation/history/download, audit screens | `npm test -- --run src/features/reports` |
| WP-S5-04 | §28.1, OQ-011 | PostgreSQL + report-files backup/restore, safeguarded SQL purge | `test-backup-restore-purge.ps1` |
| WP-S5-05 | §27, TEST-03 | Complete authorization matrix, sanitized errors, protected Actuator/OpenAPI | `SecurityHardeningIT` |
| WP-S5-06 | NFR-02..04 | 500k dashboard, 100k import, 25 users, no 5xx | `k6 run performance-load.js` |
| WP-S5-07 | NFR-07 | Playwright E2E Chrome/Edge/Firefox, full acceptance journey | Gate E |
| WP-S5-08 | §37 | README, .env.example, OpenAPI, CSV templates, runbooks, evidence | B, F, D, E gates |

**Exit Criteria** : All 10 acceptance criteria with reproducible evidence ; pilot installable/operable ; final approval Mohamed

---

## ✅ GATES DE QUALITÉ

### Backend Gate (B)
```powershell
cd backend
./mvnw clean verify
```
✓ Compile Java 25  
✓ Liquibase on PostgreSQL Testcontainers  
✓ Unit/integration/security/architecture tests  

### Frontend Gate (F)
```powershell
cd frontend
npm ci
npm run lint
npm test -- --run
npm run build
```
✓ Lint zero errors  
✓ Unit/component tests  
✓ Build successful  

### Deployment Gate (D)
```powershell
docker compose config
docker compose up -d --build
curl -f http://localhost:8080/actuator/health
```
✓ All services healthy  
✓ Volumes mounted  
✓ Profiles correct  

### E2E Gate (E)
```powershell
npx playwright test
```
✓ Chrome, Edge, Firefox smoke  
✓ Login/import/dashboard/anomaly/ticket/report journey  
✓ No visual regressions  

---

## 📋 EXCUSIONS EXPLICITES (OOS-01..OOS-15)

❌ PLC, SCADA, HMI, OPC UA, MQTT, Modbus, gateway  
❌ Shutdown, parameter changes, actuation  
❌ Python analytics, external LLM, generative AI  
❌ Full MES, ERP write-back, digital twin, mobile native  
❌ Certified predictive maintenance, legal compliance  
❌ High-availability clustering, data lake, ABAC  
❌ Multi-tenant SaaS

---

## 🎯 PLAYBOOK AGENT CODEX

### Starter Goal
```text
Implement backlog item WP-S<X>-<Y> from docs/IMPLEMENTATION_BACKLOG.md.
Read AGENTS.md + nested instructions. Keep Java25/SB4.1/React TypeScript.
Create execution plan from docs/plans/TEMPLATE.md. Done = documented commands pass,
no unrelated changes, plan records exact verification.
```

### Task Loop
1. Sélectionner next item dependency-ready
2. Donner goal : task ID, constraints, proof
3. Mode Plan pour travail ambigu, execution-plan pour substantiel
4. Inspection code avant edit
5. Tests + gates affectées complètes
6. Review diff + regression/security pass
7. Update plan + traceability
8. Stop approval Mohamed sprint boundary

### Handoff Checklist
- Task + requirement IDs
- Files and behavior changed
- Migrations/API changes
- Tests added
- Exact commands run + results
- Remaining risks
- Next smallest safe task

---

## 🚀 POINTS DE DÉMARRAGE

### Avant Sprint 0
- [ ] Valider structure repo : `backend/`, `frontend/`, `docs/`, `docker-compose.yml`
- [ ] Documenter setup local : `.env.example`, README backend/frontend
- [ ] Valider toolchain : Java 25, Maven, Node 18+, Docker
- [ ] Setup IDE : IntelliJ + VS Code + Codex extensions

### Sprint 0 Démarrage
- [ ] WP-S0-01 : Repository skeleton
- [ ] WP-S0-02 : Architecture diagrams
- [ ] WP-S0-03 : PostgreSQL Testcontainer
- [ ] WP-S0-04 : Package architecture
- [ ] ... (continuer tasks dans ordre)

### Approval Gate
**Stop après chaque sprint exit** pour approval Mohamed avant starting next sprint.

---

## 📌 RÉSUMÉ DÉCISIONNEL

| Aspect | Décision | Raison |
|--------|----------|--------|
| **IDE Frontend** | VS Code | Léger, Vite HMR, Playwright |
| **IDE Backend** | IntelliJ Community | Spring Boot, JPA, debugging |
| **Agent** | GitHub Copilot (Codex) | Code generation, tests, gates |
| **Architecture** | Modular monolith | Scalable, maintainable, testable |
| **Database** | PostgreSQL | ACID, constraints, migrations |
| **Frontend Framework** | React 18+ TypeScript | Modern, typed, ecosystem fort |
| **Backend Framework** | Spring Boot 4.1.x | Java 25, latest features |
| **Testing** | JUnit5 + Vitest + Playwright | Pyramid quality gates |
| **Approval** | Mohamed at sprint exit | Change control, risk mitigation |

---

## 📞 CONTACTS & ESCALATION

**Sponsor** : Mohamed  
**Architecture Lead** : [À confirmer]  
**Frontend Lead** : [À confirmer]  
**Backend Lead** : [À confirmer]  

**Approval Gates** : Sprint 0, 1, 2, 3, 4, 5 exits  
**Risk Mitigation** : Weekly checkpoint, risk register

---

## 📄 RÉFÉRENCES DOCUMENTAIRES

| Document | Localisation | Autorité |
|----------|-------------|----------|
| **Cahier des Charges** | `WerkPilot_VLBG_Cahier_des_Charges_v2.1.docx` | 🔴 **ABSOLUE** |
| **Analyse Projet** | `docs/PROJECT_ANALYSIS.md` | Readiness assessment |
| **Baseline Produit** | `docs/PRODUCT_BASELINE.md` | Fixed scope, KPI, safety |
| **Architecture** | `docs/ARCHITECTURE.md` | Context, components, deployment |
| **Modèle Domaine** | `docs/DOMAIN_MODEL.md` | Entities, invariants, persistence |
| **Contrat API** | `docs/API_CONTRACT.md` | REST, endpoints, errors |
| **Sécurité** | `docs/SECURITY.md` | Auth, RBAC, audit, privacy |
| **Stratégie Test** | `docs/TEST_STRATEGY.md` | Suites, levels, acceptance |
| **Backlog** | `docs/IMPLEMENTATION_BACKLOG.md` | Tasks ordonnées, dependencies |
| **Requirements** | `docs/requirements/REQUIREMENTS.csv` | 106 IDs machine-readable |
| **Agents** | `docs/AGENTS.md` | Codex workflow + instructions |
| **Playbook** | `docs/AGENT_PLAYBOOK.md` | Agent loop + checklists |
| **Décisions** | `docs/DECISIONS_AND_OPEN_QUESTIONS.md` | OQ resolutions, CR log |

---

## ✨ VERSION CONTRÔLÉE

**Version** : 1.0  
**Date** : 2026-07-06  
**Auteur** : GitHub Copilot  
**Approval** : [À confirmer par Mohamed]  
**Statut** : Ready for Sprint 0 kickoff  

---

**Fin du Plan d'Action**

*Pour conversion en .docx : Ouvrir ce fichier dans Word ou utiliser Pandoc :*
```powershell
pandoc PLAN_ACTION.md -o PLAN_ACTION.docx
```
