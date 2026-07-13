# Execution Plan: WP-S1-03 - Emailed Password Reset

- Status: Completed
- Owner: Codex / Mohamed
- Started: 2026-07-07
- Last updated: 2026-07-07
- Backlog item: WP-S1-03 Emailed password reset
- Requirement IDs: S-01, S-11
- Source sections: 24.1, 27.4, 29.2
- Open question trace: OQ-002 resolved

## Goal and observable outcome

Implement backend password reset by emailed one-time link. Public reset request
is enumeration-safe and returns `202`. Active accounts receive a 60-minute,
single-use fragment token link. Confirmation changes the password and revokes
all refresh sessions. ADMIN can trigger the same email for a user without seeing
the token or password.

## In scope

- Password reset token persistence with hashes only.
- Self-service reset request endpoint.
- ADMIN reset trigger endpoint.
- Reset confirmation endpoint.
- Password update with BCrypt strength 12.
- Refresh-session revocation after successful confirmation.
- Audit events for requested and completed reset.
- SMTP/Mailpit mail adapter with German plain-text mail.
- Integration tests for enumeration safety, admin trigger, confirm, reuse
  rejection, and session revocation.

## Out of scope

- Frontend reset UI, owned by WP-S1-07 in VS Code.
- Pilot rate limiting beyond the stable endpoint behavior.
- Audit query API/UI, owned by WP-S1-06 and WP-S5-03.

## Endpoint contract

| Method | Path | Access | Behavior |
| --- | --- | --- | --- |
| POST | `/auth/password-reset-request` | Public | Always `202`; sends mail only for active account. |
| POST | `/auth/password-reset-confirm` | Public | Valid token + new password; consumes token and revokes sessions. |
| POST | `/users/{id}/password-reset` | ADMIN | Sends the same reset mail; reveals no token/password. |

## Milestones

### Milestone 1 - Persistence and ports

- [x] Add password reset token migration.
- [x] Add password reset token port/adapter.
- [x] Extend user and refresh-session ports for password update/session revoke.

### Milestone 2 - Application/API/mail

- [x] Add password reset service.
- [x] Add public and ADMIN endpoints.
- [x] Add mail port and SMTP/Mailpit adapter.
- [x] Add audit events.

### Milestone 3 - Verification

- [x] Add `PasswordResetFlowIT`.
- [x] Run focused proof.
- [x] Run backend gate.
- [x] Update this plan with exact results.

## Test and acceptance matrix

| Behavior | Test level | Test/file | Expected result |
| --- | --- | --- | --- |
| Public request is enumeration-safe | Integration | `PasswordResetFlowIT` | Pass |
| Active account receives fragment-token link | Integration | `PasswordResetFlowIT` | Pass |
| Confirm changes password and consumes token | Integration | `PasswordResetFlowIT` | Pass |
| Confirm revokes refresh sessions | Integration | `PasswordResetFlowIT` | Pass |
| ADMIN trigger sends reset email without revealing token | Integration | `PasswordResetFlowIT` | Pass |

## Progress log

- 2026-07-07 - Started WP-S1-03 after WP-S1-02 backend gate passed.
- 2026-07-07 - Added hashed password reset token persistence, public/admin
  reset endpoints, SMTP/Mailpit mail adapter, password confirmation, refresh
  session revocation, and reset audit events.
- 2026-07-07 - Full backend gate initially exposed a missing
  `PasswordResetMailPort` in regular application contexts. Fixed by registering
  the SMTP adapter normally and using a primary recording mail port in the
  integration test.
- 2026-07-07 - WP-S1-03 backend verification completed successfully.

## Final verification

- [x] Focused tests: `mvn "-Dtest=PasswordResetFlowIT" test` - 3 tests,
  0 failures, build success.
- [x] Targeted context/architecture/reset tests:
  `mvn "-Dtest=ApplicationContextIT,ArchitectureRulesTest,PasswordResetFlowIT" test`
  - 14 tests, 0 failures, build success.
- [x] Backend `clean verify` when affected: `mvn clean verify` - 37 tests,
  0 failures, build success.
- [x] Frontend lint/test/build when affected: not run; WP-S1-03 is backend
  scope and no frontend source was edited.
- [x] Docker/health/E2E checks when affected: not run; Maven integration gate
  passed for the affected backend flow.
- [x] Diff reviewed for unrelated changes: repo content is currently untracked,
  so tracked git diff cannot isolate this work reliably.
- [x] Documentation and traceability updated: this plan updated.

## Handoff

Completed for backend/IntelliJ. Frontend password reset screens remain owned by
WP-S1-07 in VS Code.
