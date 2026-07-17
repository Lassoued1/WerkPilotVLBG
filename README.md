# WerkPilot VLBG

WerkPilot VLBG est une plateforme mono-tenant de support decisionnel pour PME manufacturieres. Le projet est organise en deux flux de developpement coordonnes :

- Backend : Spring Boot, developpe et suivi dans IntelliJ IDEA avec le plugin ChatGPT Codex.
- Frontend : React TypeScript, developpe et suivi dans VS Code avec le plugin ChatGPT Codex.

La source contractuelle prioritaire est :

```text
docs/WerkPilot_VLBG_Cahier_des_Charges_v2.1.docx
```

Si une documentation derivee contredit ce document, le cahier des charges v2.1 prevaut.

## Statut projet

Le projet suit un backlog en 6 sprints :

| Sprint | Objectif principal | Statut actuel |
| --- | --- | --- |
| Sprint 0 | Baseline architecture, toolchain, Docker, gates | Termine / stabilise pour les sprints suivants |
| Sprint 1 | Identity, RBAC, users, audit, master data | Termine |
| Sprint 2 | Imports CSV asynchrones, correction, rollback | Termine |
| Sprint 3 | APIs KPI, agregations, dashboard | Termine jusqu'a WP-S3-06 |
| Sprint 4 | Analytics, anomalies, maintenance | Planifie |
| Sprint 5 | Reporting, operations, hardening, acceptance | Planifie |

Le backlog detaille est maintenu dans :

```text
docs/IMPLEMENTATION_BACKLOG.md
```

## Architecture fonctionnelle

WerkPilot couvre 12 modules metier :

1. Identity
2. Master Data
3. CSV Import
4. Production
5. Energy
6. Scrap / Downtime
7. Analytics
8. Maintenance
9. Dashboard
10. Reports
11. Audit
12. Administration

Roles RBAC fixes :

- `ADMIN`
- `PRODUCTION_MANAGER`
- `MAINTENANCE_TECHNICIAN`
- `ENERGY_MANAGER`
- `VIEWER`

Le backend reste l'autorite metier. Le frontend ne doit pas recalculer les KPI, les droits, les statuts ou les regles de validation ; il consomme les API backend et affiche les resultats.

## Etat Sprint 3

Sprint 3 couvre les KPI backend, les APIs d'agregation et le dashboard frontend.

| Tache | Statut | Zone | Preuve principale |
| --- | --- | --- | --- |
| WP-S3-01 KPI calculation services | Done | Backend | `.\mvnw.cmd "-Dtest=KpiCalculationServiceTest" test` |
| WP-S3-02 Time/filter/active-data query policy | Done | Backend | `.\mvnw.cmd "-Dtest=TimeWindowAggregationIT,KpiFilteringIT" verify` |
| WP-S3-03 Production and energy APIs | Done | Backend | `.\mvnw.cmd "-Dtest=ProductionApiIT,EnergyApiIT" verify` |
| WP-S3-04 Downtime and scrap APIs | Done | Backend | `.\mvnw.cmd "-Dtest=DowntimeApiIT,ScrapApiIT" verify` |
| WP-S3-05 Dashboard summary and performance | Done | Backend | `.\mvnw.cmd "-Dtest=DashboardApiIT,DashboardPerformanceIT" verify` |
| WP-S3-06 Dashboard and monitoring UI | Done | Frontend | `npm test -- --run src/features/dashboard src/features/machines src/features/production` |

Preuves recentes :

```powershell
cd backend
.\mvnw.cmd clean verify
# 100 tests, 0 failure, 0 error

cd ..\frontend
npm test -- --run
# 8 files, 19 tests

npm run lint
npm run build
```

Runbook associe :

```text
docs/runbooks/sprint3-dashboard-performance.md
```

Ce runbook decrit comment produire une evidence performance durable pour le dashboard, notamment le benchmark cible de 500 000 lignes.

## APIs KPI et dashboard disponibles

Les endpoints Sprint 3 consommes par le frontend sont :

| Endpoint | Usage |
| --- | --- |
| `GET /dashboard/summary` | Cartes KPI, tendance production, Pareto downtime, top consommateurs energie |
| `GET /production/kpis` | KPI production |
| `GET /production/records` | Liste paginee des enregistrements production |
| `GET /production/trends` | Tendance production |
| `GET /production/evidence.csv` | Evidence CSV production |
| `GET /energy/kpis` | KPI energie |
| `GET /energy/top-consumers` | Top consommateurs energie |
| `GET /downtime/pareto` | Pareto downtime et disponibilite |
| `GET /quality/scrap-rate` | Taux d'ausschuss / scrap rate |

Toutes les requetes KPI utilisent des intervalles `[from,to)` et uniquement les donnees issues de jobs `COMMITTED`. Les lignes `SUPERSEDED` restent disponibles pour la tracabilite mais sont exclues des KPI.

## Stack technique

| Zone | Technologie |
| --- | --- |
| Backend | Java 25, Spring Boot 4.1, Maven Wrapper |
| Backend persistence | PostgreSQL 16, Liquibase, JPA |
| Backend tests | JUnit, Spring Boot Test, Testcontainers, ArchUnit |
| Frontend | React 19, TypeScript 5.9, Vite 7 |
| Frontend tests | Vitest, Testing Library |
| Local stack | Docker Compose, PostgreSQL, backend, nginx/frontend, Mailpit |
| CI | GitHub Actions |

## Structure du depot

```text
WerkPilotVLBG/
├── backend/                 # Application Spring Boot, ouverte dans IntelliJ
├── frontend/                # Application React/Vite, ouverte dans VS Code
├── docker/                  # Dockerfiles et configuration nginx
├── docs/                    # Cahier des charges, architecture, backlog, securite, tests
├── scripts/                 # Scripts de validation projet
├── .github/workflows/       # CI GitHub Actions
├── docker-compose.yml       # Stack locale
├── .env.example             # Exemple de configuration locale
└── PLAN_ACTION.md           # Plan d'action global
```

## Prerequis locaux

- Java 25
- Maven via `backend/mvnw.cmd`
- Node.js 24 recommande
- npm 11+
- Docker Desktop
- Git
- IntelliJ IDEA pour le backend
- VS Code pour le frontend

Validation rapide de l'environnement :

```powershell
powershell -File scripts\validate-agent-kit.ps1
```

## Installation locale

Depuis la racine du projet :

```powershell
Copy-Item .env.example .env
```

Puis lancer la stack locale :

```powershell
docker compose --profile local up -d --build
```

URLs utiles :

| Service | URL |
| --- | --- |
| Frontend nginx | http://localhost:8081 |
| Backend health | http://localhost:8080/actuator/health |
| Mailpit | http://localhost:8025 |
| PostgreSQL | localhost:5432 |

## Developpement backend dans IntelliJ

Ouvrir le dossier :

```text
backend/
```

Commandes principales :

```powershell
cd backend
.\mvnw.cmd clean verify
```

Test cible :

```powershell
cd backend
.\mvnw.cmd "-Dtest=ApplicationContextIT" test
```

Le fichier de configuration de test est :

```text
backend/src/test/resources/application.yml
```

Il desactive le health check SMTP pendant les tests Maven, tout en gardant le bean `JavaMailSender` disponible pour les services de reset password.

## Developpement frontend dans VS Code

Ouvrir le dossier :

```text
D:\IntellijProjects\WerkPilotVLBG
```

Installer les dependances :

```powershell
cd frontend
npm ci
```

Lancer le serveur Vite :

```powershell
npm run dev
```

Valider le frontend :

```powershell
npm run lint
npm test -- --run
npm run build
```

Test cible Sprint 3 UI :

```powershell
npm test -- --run src/features/dashboard src/features/machines src/features/production
```

## Gates qualite

Avant de pousser ou de demander une revue, executer les gates applicables.

### Gate backend

```powershell
cd backend
.\mvnw.cmd clean verify
```

### Gate frontend

```powershell
cd frontend
npm ci
npm run lint
npm test -- --run
npm run build
```

### Gate Docker

```powershell
docker compose --profile local config
docker compose --profile local up -d --build
curl.exe -f http://localhost:8080/actuator/health
```

### Gate squelette projet

```powershell
powershell -File scripts\validate-agent-kit.ps1
```

## Workflow Codex recommande

Le projet doit etre suivi avec deux agents Codex separes :

| Flux | Outil | Responsabilite |
| --- | --- | --- |
| Backend | IntelliJ + ChatGPT Codex plugin | Java, Spring Boot, securite, persistence, tests backend |
| Frontend | VS Code + ChatGPT Codex plugin | React, TypeScript, UI, tests frontend |
| Coordination | Backlog + OpenAPI + docs | Contrats API, sprint gates, evidence |

Regle de coordination :

1. Le backend expose ou modifie le contrat API.
2. Le frontend consomme le contrat sans dupliquer la logique metier.
3. Les tests backend et frontend sont relances.
4. Le sprint n'est ferme qu'apres evidence et approbation Mohamed.

Directive de collaboration :

- Les actions git doivent utiliser l'identite `Lassoued1 <mohamed.lassoued@gmail.com>`.
- Ne pas committer ou pousser avec une identite `Codex`.
- Les changements backend sont geres dans IntelliJ/Codex.
- Les changements frontend sont geres dans VS Code/Codex.
- Les sorties IA doivent etre verifiees par des tests reproductibles : "trust then verify".
- Les runbooks et harnesses de tests doivent etre reutilisables lorsque possible.

## Documentation projet

| Document | Role |
| --- | --- |
| `docs/README.md` | Carte de la documentation |
| `docs/PROJECT_ANALYSIS.md` | Analyse de preparation projet |
| `docs/PRODUCT_BASELINE.md` | Scope produit, roles, KPI, limites |
| `docs/ARCHITECTURE.md` | Vues architecture et regles modulaires |
| `docs/API_CONTRACT.md` | Contrats REST, erreurs, pagination |
| `docs/DOMAIN_MODEL.md` | Modele domaine et invariants |
| `docs/SECURITY.md` | Auth, RBAC, audit, securite |
| `docs/TEST_STRATEGY.md` | Strategie de tests et acceptance |
| `docs/IMPLEMENTATION_BACKLOG.md` | Backlog sprint par sprint |
| `docs/PUSH_CHECKLIST.md` | Checklist avant push |
| `docs/runbooks/sprint3-dashboard-performance.md` | Runbook performance dashboard Sprint 3 |

## Regles de push

Ne pas pousser :

- `.env`
- secrets locaux
- `backend/target/`
- `frontend/node_modules/`
- `frontend/dist/`
- fichiers IDE locaux
- logs

Ces exclusions sont deja couvertes par `.gitignore`.

Avant push :

```powershell
git status --short
powershell -File scripts\validate-agent-kit.ps1
cd backend
.\mvnw.cmd clean verify
cd ..\frontend
npm ci
npm run lint
npm test -- --run
npm run build
cd ..
docker compose --profile local config
```

Voir aussi :

```text
docs/PUSH_CHECKLIST.md
```

## Comptes et donnees locales

Le backend peut creer un compte admin de demonstration en environnement local/test selon la configuration :

```text
admin@werkpilot.local
WerkPilot-Admin-Change-Me-2026
```

Ne pas utiliser ces valeurs en pilot/production.

## Profils Docker

| Profil | Usage |
| --- | --- |
| `local` | Developpement local avec Mailpit |
| `test` | Execution outillee avec Mailpit |
| `pilot` | Deploiement pilote sans Mailpit, avec relais SMTP entreprise |

Exemple local :

```powershell
docker compose --profile local up -d --build
```

Exemple arret :

```powershell
docker compose down
```

Avec suppression des volumes locaux :

```powershell
docker compose down -v
```

## Statut d'acceptation Sprint 0

Sprint 0 peut etre ferme uniquement si les elements suivants sont verts :

- `scripts/validate-agent-kit.ps1`
- backend `mvnw clean verify`
- frontend lint/test/build
- `docker compose --profile local config`
- health backend Docker
- approbation Mohamed

Tant que ces preuves ne sont pas attachees, le sprint reste "quasi termine" mais pas officiellement "Done".

## Statut d'acceptation Sprint 3

Sprint 3 peut etre considere termine lorsque les preuves suivantes sont vertes :

- backend `.\mvnw.cmd clean verify`
- frontend `npm test -- --run`
- frontend `npm run lint`
- frontend `npm run build`
- revue que le frontend affiche les KPI backend sans recalcul navigateur
- evidence performance dashboard selon le runbook Sprint 3 pour l'environnement pilote/CI

Etat actuel local : les gates backend et frontend ont ete executes avec succes apres WP-S3-06.
