# Execution Plan: WP-S1-01 - Authentication, Refresh, Logout and CSRF

- Status: Completed
- Owner: Codex / Mohamed
- Started: 2026-07-07
- Last updated: 2026-07-07
- Backlog item: WP-S1-01 Authentication, refresh, logout and CSRF
- Requirement IDs: S-01, OQ-001
- Source sections: 24.1, 27.1

## Goal and observable outcome

Implement backend authentication endpoints for login, refresh, logout and
current profile. Login returns a 15-minute access token in JSON and sets a
12-hour HttpOnly/Secure/SameSite=Strict refresh cookie. Refresh validates a
CSRF header, rotates the refresh cookie, and returns a new access token. Logout
validates CSRF, revokes the current refresh token, and clears the cookie.

## In scope

- Identity persistence for users and refresh sessions.
- BCrypt password hashing with strength 12.
- Access token issuing and Bearer-token authentication.
- Refresh-token hashing, storage, rotation, revocation, and cookie transport.
- CSRF header validation for refresh and logout.
- CORS origin configuration for deployed frontend origin.
- Integration tests for login, refresh rotation, logout, CSRF, invalid login,
  and `/auth/me`.

## Out of scope

- Password reset flow, which belongs to WP-S1-03.
- Full user administration/RBAC management, which belongs to WP-S1-02.
- Audit persistence, which belongs to WP-S1-06; security event hooks can be
  added later.
- Frontend auth UI, which belongs to WP-S1-07 in VS Code.

## Current-state findings

- Identity packages currently contain only package markers.
- No security dependency, identity tables, auth endpoints, or auth tests exist.
- Actuator health and the OpenAPI shared endpoint already exist.

## Normative decisions and source conflicts

- DOCX v2.1 remains authoritative.
- OQ-001 is resolved: refresh token only in HttpOnly Secure SameSite=Strict
  cookie; access token only in JSON/Bearer header; CSRF applies to refresh and
  logout; CORS is restricted.
- No source conflict was found.

## Milestones

### Milestone 1 - Persistence and domain

- [x] Add identity/refresh Liquibase migration.
- [x] Add role enum, user entity, refresh session entity, repositories, and seed
  demo admin for test/local bootstrap.

### Milestone 2 - Auth application and API

- [x] Add token generation/hashing services.
- [x] Add login, refresh, logout, and `/auth/me` endpoints.
- [x] Add refresh cookie and CSRF behavior.

### Milestone 3 - Security configuration

- [x] Add Spring Security dependencies and bearer authentication filter.
- [x] Add CORS configuration.
- [x] Keep public endpoints limited to auth login/reset placeholders, health,
  and local OpenAPI.

### Milestone 4 - Verification

- [x] Add focused auth integration tests.
- [x] Run focused auth tests.
- [x] Run backend gate.
- [x] Update this plan with exact results.

## Test and acceptance matrix

| Behavior | Test level | Test/file | Expected result |
| --- | --- | --- | --- |
| Login returns access token and secure refresh cookie | Integration | `AuthenticationFlowIT` | Pass |
| Refresh requires CSRF and rotates cookie | Integration | `RefreshTokenRotationIT` | Pass |
| Logout requires CSRF and clears/revokes cookie | Integration | `AuthenticationFlowIT` | Pass |
| Bearer token authenticates `/auth/me` | Integration | `AuthenticationFlowIT` | Pass |
| CORS allows configured frontend origin | Integration | `CsrfProtectionIT` | Pass |

## Risks and rollback

- Data/migration risk: identity tables are new; rollback drops only WP-S1-01
  tables/indexes.
- Security risk: seed admin is local/test bootstrap and must be changed before
  pilot use.
- Compatibility risk: Spring Boot 4/Spring Security package names may require
  adjustment during compilation.
- Rollback or safe recovery: revert migration, security dependencies/config,
  identity classes/tests, and this plan.

## Progress log

- 2026-07-07 - Started WP-S1-01 after Sprint 0 approval was recorded.
- 2026-07-07 - Implemented backend authentication endpoints, security filter,
  identity persistence, refresh rotation, logout revocation, CSRF validation,
  and CORS configuration.
- 2026-07-07 - Fixed verification issues found during focused tests:
  `/auth/refresh` is public at Spring Security level but validates refresh
  cookie and CSRF in the application service; logout tests include Bearer auth;
  DB integration tests dirties the Spring context after each Testcontainers
  class; access tokens include a signed nonce so refresh returns a new token
  even within the same second.

## Decision log

- 2026-07-07 - Use opaque HMAC-signed access tokens instead of JWT library
  dependency for Sprint 1 baseline; backend remains the only token issuer and
  verifier.
- 2026-07-07 - Include a signed random nonce in each access token payload to
  prevent identical tokens when two sessions are issued in the same second.

## Unexpected findings

- Spring Security correctly denied logout without Bearer authentication; tests
  were adjusted because the contract says logout is authenticated plus CSRF.
- Testcontainers stopped per-class PostgreSQL containers while Spring reused a
  cached context; the DB support now marks contexts dirty after each class.

## Final verification

- [x] Focused tests: `mvn "-Dtest=AuthenticationFlowIT,RefreshTokenRotationIT,CsrfProtectionIT" test` - 6 tests, 0 failures, build success.
- [x] Backend `clean verify` when affected: `mvn clean verify` - 27 tests, 0 failures, build success.
- [x] Frontend lint/test/build when affected: not run; WP-S1-01 is backend/IntelliJ scope and no frontend source was edited.
- [ ] Docker/health/E2E checks when affected: not run; backend Maven gate and integration tests passed.
- [x] Diff reviewed for unrelated changes: repository is entirely untracked in this workspace, so `git status` cannot isolate a useful tracked diff.
- [x] Documentation and traceability updated: this plan updated with implementation and verification results.

## Handoff

WP-S1-01 is complete on the IntelliJ/backend side. Frontend auth integration
remains out of scope for this work package and belongs to the VS Code-owned
frontend work.
