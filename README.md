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
| Sprint 0 | Baseline architecture, toolchain, Docker, gates | Quasi termine, validations finales a confirmer |
| Sprint 1 | Identity, RBAC, users, audit, master data | Backend largement avance, frontend a completer |
| Sprint 2 | Imports CSV asynchrones, correction, rollback | Planifie |
| Sprint 3 | APIs KPI, agregations, dashboard | Planifie |
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
frontend/
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
