# Execution Plan: WP-S1-02 - Fixed RBAC and User Administration

- Status: Completed
- Owner: Codex / Mohamed
- Started: 2026-07-07
- Last updated: 2026-07-07
- Backlog item: WP-S1-02 Fixed RBAC and user administration
- Requirement IDs: S-01, S-11, TEST-03, ACC-02, ACC-09
- Source sections: 8, 8.1, 27.2, 30.2, 31.1

## Goal and observable outcome

Implement backend-only fixed-role authorization and ADMIN user administration.
ADMIN can list, create, read, update roles/status/display name, and soft-disable
users. Non-admin roles cannot access the user administration endpoints. Role
and status changes create audit rows for later WP-S1-06 query work.

## In scope

- Five fixed roles already defined by WP-S1-01.
- ADMIN-only authorization for `/users/**`.
- User list/read/create/update/status/soft-disable endpoints.
- Email normalization and uniqueness.
- BCrypt hashing for admin-created temporary passwords.
- Guard against removing or disabling the last active ADMIN.
- Audit persistence for user role/status/admin-create events.
- Integration tests for admin CRUD and role authorization.

## Out of scope

- Frontend user administration UI, owned by WP-S1-07 in VS Code.
- Emailed password reset and admin reset trigger, owned by WP-S1-03.
- Audit query API/UI, owned by WP-S1-06 and WP-S5-03.
- Fine-grained ABAC, explicitly out of scope.

## Endpoint contract

| Method | Path | Access | Behavior |
| --- | --- | --- | --- |
| GET | `/users` | ADMIN | Paginated user list. |
| POST | `/users` | ADMIN | Create active user with fixed roles and temporary password. |
| GET | `/users/{id}` | ADMIN | Read user. |
| PUT | `/users/{id}` | ADMIN | Update display name, roles, and active status. |
| PATCH | `/users/{id}/status` | ADMIN | Set active/inactive status. |
| DELETE | `/users/{id}` | ADMIN | Soft-disable user. |

## Milestones

### Milestone 1 - Persistence and audit

- [x] Add audit event table and persistence port/adapter.
- [x] Extend user persistence for list/update/count active admins.

### Milestone 2 - Application/API

- [x] Add user administration service.
- [x] Add ADMIN-only user controller DTOs and validation.
- [x] Add last-active-admin guard.

### Milestone 3 - Authorization and tests

- [x] Add `/users/**` ADMIN authorization.
- [x] Add `UserAdministrationIT`.
- [x] Add `SecurityAuthorizationIT`.

### Milestone 4 - Verification

- [x] Run focused tests.
- [x] Run backend gate.
- [x] Update this plan with exact results.

## Test and acceptance matrix

| Behavior | Test level | Test/file | Expected result |
| --- | --- | --- | --- |
| ADMIN creates/lists/updates/disables users | Integration | `UserAdministrationIT` | Pass |
| Duplicate email is rejected | Integration | `UserAdministrationIT` | Pass |
| Last active ADMIN cannot be demoted/disabled | Integration | `UserAdministrationIT` | Pass |
| Role/status changes create audit rows | Integration | `UserAdministrationIT` | Pass |
| Non-admin and anonymous users cannot access `/users` | Integration | `SecurityAuthorizationIT` | Pass |

## Progress log

- 2026-07-07 - Started WP-S1-02 after WP-S1-01 backend gate passed.
- 2026-07-07 - Added `/users` ADMIN-only backend API, user persistence
  updates, audit event persistence, last-active-admin guard, and focused
  authorization/user-admin integration tests.
- 2026-07-07 - Full backend gate initially exposed architecture violations:
  the controller depended on the domain role enum and the application returned
  `shared.api.PageResponse`. Fixed by accepting role names at the API boundary,
  converting them in the application service, and building `PageResponse` only
  in the controller.

## Final verification

- [x] Focused tests: `mvn "-Dtest=UserAdministrationIT,SecurityAuthorizationIT" test` - 7 tests, 0 failures, build success.
- [x] Architecture/focused rerun: `mvn "-Dtest=ArchitectureRulesTest,UserAdministrationIT,SecurityAuthorizationIT" test` - 15 tests, 0 failures, build success.
- [x] Backend `clean verify` when affected: `mvn clean verify` - 34 tests, 0 failures, build success.
- [x] Frontend lint/test/build when affected: not run; WP-S1-02 is backend/IntelliJ scope and no frontend source was edited.
- [ ] Docker/health/E2E checks when affected: not run; backend Maven gate and integration tests passed.
- [x] Diff reviewed for unrelated changes: repository is entirely untracked in this workspace, so `git status` cannot isolate a useful tracked diff.
- [x] Documentation and traceability updated: this plan updated with implementation and verification results.

## Handoff

WP-S1-02 is complete on the IntelliJ/backend side. Frontend user administration
screens remain out of scope for this work package and belong to WP-S1-07 in VS
Code.
