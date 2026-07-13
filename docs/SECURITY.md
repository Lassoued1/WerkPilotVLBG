# Security, Privacy, Audit, and Industrial Safety

## Security posture

WerkPilot handles industrial operational data and user work identities. The
backend enforces security. The MVP is single-tenant, decision-support only,
and isolated from machine-control networks and actions.

## Authentication baseline

- Hash passwords with BCrypt strength 12 or greater.
- Access tokens live for 15 minutes, are returned in login/refresh JSON, and
  are sent only through the `Authorization: Bearer` header.
- Refresh tokens live for 12 hours and are transported exclusively in an
  HttpOnly, Secure, SameSite=Strict cookie. Their server-side identifiers are
  hashed, revocable, and never returned in JSON, URLs, or logs.
- Refresh rotates the cookie; logout revokes it and clears the cookie.
- Apply CSRF protection to cookie-authenticated state changes, at minimum
  `/auth/refresh` and `/auth/logout`, using a validated token header.
- Restrict CORS to the deployed frontend origin. The pilot nginx topology uses
  one origin; any future cross-origin deployment requires a security review.
- Three failed login attempts within ten minutes create a security audit event.
- The local demo admin is seed-only and must be changed before pilot use.

These rules implement OQ-001 as resolved in Sections 24.1 and 27.1.

## Password reset

- Self-service and admin-triggered reset use the same emailed one-time link.
- Tokens contain at least 128 random bits, are stored only as hashes, expire
  after 60 minutes, and are single-use.
- The link carries the token in the URL fragment, not the query string.
- Reset requests always return the same `202` response and are rate-limited by
  address and IP in pilot.
- Requesting reset does not invalidate the password or sessions. Successful
  confirmation changes the password and revokes every refresh token.
- German mail is built from `APP_BASE_URL`; local uses Mailpit and pilot uses
  the configured SMTP relay. Tokens and passwords never appear in logs.

These rules implement OQ-002 and Sections 24.1 and 27.4.

## Authorization baseline

Persist exactly `ADMIN`, `PRODUCTION_MANAGER`, `MAINTENANCE_TECHNICIAN`,
`ENERGY_MANAGER`, and `VIEWER`. Apply request/method authorization in the
backend and verify every protected controller.

Additional checks include:

- technicians update only assigned tickets;
- managers access only permitted import types;
- ADMIN always manages energy thresholds, while ENERGY_MANAGER may do so only
  when `energy_threshold_delegation_enabled` is ON;
- only ADMIN changes that global setting;
- report generation, history, and downloads enforce approved roles;
- audit views are ADMIN-only.

UI visibility is never authorization.

## Input and file security

- Enforce 25 MB and 100,000 rows independently.
- Accept strict UTF-8 comma-separated templates and reject unknown headers.
- Treat filenames as untrusted metadata and never construct server paths from
  them.
- Keep uploads and reports outside the web root.
- Escape formula-leading CSV export cells when spreadsheet interpretation is
  possible.
- Store comments as escaped plain text with a 500-character limit.
- Never include uploaded row content in normal logs.

## API and browser security

- Return only the structured, sanitized error format from Section 17.
- English `errorCode` values are branching keys. The frontend renders German
  catalog text; only dynamic CSV detail messages are generated in German by
  the backend.
- Use secure headers and HTTPS in pilot.
- Protect OpenAPI and Actuator outside local development.
- Do not expose secrets in frontend bundles, image layers, reports, or logs.

## Audit requirements

Application services append but never update/delete these events:

- `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `USER_ROLE_CHANGED`
- `CSV_IMPORT_STARTED`, `CSV_IMPORT_FAILED`, `CSV_IMPORT_COMMITTED`
- `CSV_IMPORT_CORRECTED`, `CSV_IMPORT_ROLLED_BACK`
- `ANOMALY_STATUS_CHANGED`, `TICKET_STATUS_CHANGED`
- `REPORT_GENERATED`
- `PASSWORD_RESET_REQUESTED`, `PASSWORD_RESET_COMPLETED`
- `THRESHOLD_DELEGATION_CHANGED`, `THRESHOLD_CHANGED`
- `RETENTION_PURGE_EXECUTED`

Store the fields required by Section 27.2 plus a trace ID where available.

## Privacy, retention, and reports

- Store only work name, email, roles, and status; do not collect health,
  biometric, or private employee-performance data.
- Do not require operator names in production imports.
- Retain operational records and report files for 24 months and audit events
  for 36 months unless the sponsor rebaselines the policy.
- No automatic purge exists in the MVP. The administrator runs the documented
  operation, whose safeguards refuse younger data and whose result creates
  `RETENTION_PURGE_EXECUTED`.
- Store reports in the external `report-files` volume and serve them only via
  the authorized download endpoint. A missing file returns `404`; it is never
  silently regenerated.

## Industrial safety boundary

- No machine actuation, shutdown, or parameter changes.
- No PLC, SCADA, HMI, OPC UA, MQTT, Modbus, or gateway integration.
- Recommendations do not replace certified maintenance or safety procedures.
- Use the exact German disclaimer from Section 23.5 on anomaly details, ticket
  views that show linked recommendations, and monthly PDFs.

## Security verification gate

Verify token/cookie/CSRF/CORS behavior, password-reset enumeration resistance,
role and ownership restrictions, threshold delegation, import rejection,
sanitized errors, all audit events, protected reports/OpenAPI/Actuator, manual
retention safeguards, and absence of secrets in logs. Finish with:

```powershell
cd backend
.\mvnw.cmd clean verify
```
