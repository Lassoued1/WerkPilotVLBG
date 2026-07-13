# WerkPilot VLBG

WerkPilot VLBG is a single-tenant industrial decision-support platform for
manufacturing SMEs. The approved contractual source is
`docs/WerkPilot_VLBG_Cahier_des_Charges_v2.1.docx`.

## Development split

- Backend development happens in this IntelliJ workspace under `backend/`.
- Frontend application development happens separately in VS Code under
  `frontend/`.

## Local Docker stack

Copy the sample environment file before running the local stack:

```powershell
Copy-Item .env.example .env
```

Start the local profile with PostgreSQL, backend, nginx/frontend, Mailpit, and
the `report-files` volume:

```powershell
docker compose --profile local up -d --build
```

Useful local URLs:

- Backend health: `http://localhost:8080/actuator/health`
- Frontend/nginx: `http://localhost:8081`
- Mailpit: `http://localhost:8025`

Validate the repository skeleton with:

```powershell
powershell -File scripts\validate-agent-kit.ps1
```
