# Push Checklist — WerkPilot VLBG

Cette checklist sert a preparer un push propre du depot WerkPilot VLBG.

## 1. Verifier le perimetre du commit

Depuis la racine :

```powershell
git status --short
```

A verifier :

- les fichiers ajoutes sont intentionnels ;
- aucun secret local n'est inclus ;
- aucun artefact de build n'est inclus ;
- aucun dossier `node_modules`, `target`, `dist` n'est suivi ;
- les changements backend/frontend sont coherents avec le sprint en cours.

## 2. Fichiers a ne jamais pousser

Ne jamais pousser :

```text
.env
.env.*
backend/target/
frontend/node_modules/
frontend/dist/
*.log
.idea/
.vscode/
```

Exception autorisee :

```text
.env.example
```

## 3. Validation squelette projet

```powershell
powershell -File scripts\validate-agent-kit.ps1
```

Resultat attendu :

```text
WerkPilot agent kit validation passed.
```

## 4. Validation backend — IntelliJ + Codex

```powershell
cd backend
.\mvnw.cmd clean verify
```

Resultat attendu :

```text
BUILD SUCCESS
```

Points couverts :

- compilation Java 25 ;
- migrations Liquibase ;
- Spring context ;
- securite ;
- audit ;
- architecture modulaire ;
- contrats API partages ;
- tests d'integration Testcontainers.

## 5. Validation frontend — VS Code + Codex

```powershell
cd frontend
npm ci
npm run lint
npm test -- --run
npm run build
```

Resultat attendu :

- aucune erreur TypeScript ;
- tests Vitest OK ;
- build Vite OK.

## 6. Validation Docker Compose

Depuis la racine :

```powershell
docker compose --profile local config
```

Puis, si necessaire :

```powershell
docker compose --profile local up -d --build
curl.exe -f http://localhost:8080/actuator/health
```

Resultat attendu :

```json
{"status":"UP"}
```

URLs utiles :

| Service | URL |
| --- | --- |
| Frontend | http://localhost:8081 |
| Backend health | http://localhost:8080/actuator/health |
| Mailpit | http://localhost:8025 |

## 7. Verifier la documentation

Avant push, mettre a jour si necessaire :

| Fichier | Quand le modifier |
| --- | --- |
| `README.md` | changement setup, stack, commandes, workflow |
| `docs/IMPLEMENTATION_BACKLOG.md` | changement d'etat de sprint/tache |
| `docs/API_CONTRACT.md` | changement endpoint, payload, erreur, pagination |
| `docs/SECURITY.md` | changement auth, RBAC, audit, hardening |
| `docs/TEST_STRATEGY.md` | changement de gate, test ou acceptance |
| `docs/DECISIONS_AND_OPEN_QUESTIONS.md` | decision structurante ou change request |

## 8. Convention de commit recommandee

Format simple :

```text
<scope>: <description courte>
```

Exemples :

```text
docs: prepare project README and push checklist
backend: fix test mail health configuration
frontend: add sprint 1 auth shell
ci: add backend and frontend gates
```

Scopes recommandes :

- `backend`
- `frontend`
- `docs`
- `docker`
- `ci`
- `test`
- `security`

## 9. Avant de fermer Sprint 0

Sprint 0 peut etre marque `Done` uniquement si :

- `validate-agent-kit.ps1` passe ;
- backend gate passe ;
- frontend gate passe ;
- Docker Compose config passe ;
- health backend Docker passe ;
- les preuves sont notees ;
- Mohamed approuve le sprint exit.

## 10. Commande de push

Apres validation :

```powershell
git add .
git status --short
git commit -m "docs: prepare project documentation for push"
git push
```

Avant `git add .`, controler visuellement que `.gitignore` exclut bien les artefacts.
