# Execution Plan: WP-S0-01 - Repository and Toolchain Skeleton

- Status: In review
- Owner: Codex / Mohamed
- Started: 2026-07-06
- Last updated: 2026-07-06
- Backlog item: WP-S0-01 Repository and toolchain skeleton
- Requirement IDs: NFR-01, NFR-08
- Source sections: 29.1, 32 Sprint 0

## Goal and observable outcome

Create a reviewable repository skeleton with Git initialized, a Java 25 Maven
Wrapper backend baseline, a strict React/TypeScript/Vite frontend baseline for
the separate VS Code workflow, and a validation script that proves the expected
toolchain shape.

## In scope

- Git repository initialization and ignore rules.
- Backend Maven Wrapper, Java 25/Spring Boot 4.1.x project metadata, minimal
  application class, and smoke test.
- Frontend package metadata and strict React/TypeScript/Vite placeholder files
  only.
- Scripted version and structure checks in `scripts/validate-agent-kit.ps1`.

## Out of scope

- Business modules, persistence, Liquibase, Docker Compose, API contracts, and
  real frontend screens.
- Any change to the contractual DOCX or requirement inventory.

## Current-state findings

- Repository root initially contained `docs/`, `.idea/`, and `PLAN_ACTION.md`.
- No `.git` directory was present before this task.
- Java 25, Node v24.18.0, npm 11.6.2, and Git 2.45.1 are installed.
- Global `mvn` was initially not visible in the shell. Maven 3.9.16 was later
  found on the machine PATH at `D:\Tools\maven-3.9.16\bin`; the validation
  script refreshes User/Machine PATH values for this session.
- A Maven Wrapper resource exists locally under the VS Code Maven extension.
- `rg` is not installed; PowerShell native commands were used.

## Normative decisions and source conflicts

- DOCX v2.1 remains authoritative over derived Markdown.
- OQ labels are not reopened by this task.
- The user confirmed backend work happens here in IntelliJ; frontend
  application work happens separately in VS Code. This task creates only the
  minimum React frontend toolchain placeholder required by WP-S0-01.

## Milestones

### Milestone 1 - Repository and plan

- [x] Create this execution plan.
- [x] Initialize Git and add root ignore rules.
- [x] Verification command and expected result:
  `git status --short` shows only intentional new files.

### Milestone 2 - Backend toolchain

- [x] Add Maven Wrapper.
- [x] Add Spring Boot Java 25 backend skeleton.
- [x] Add backend smoke test.
- [x] Verification command and expected result:
  `mvn clean verify` in `backend/` reports `BUILD SUCCESS`.

### Milestone 3 - Frontend placeholder and validation

- [x] Add minimal strict React/TypeScript/Vite package placeholder.
- [x] Add `scripts/validate-agent-kit.ps1`.
- [x] Verification command and expected result:
  `powershell -File scripts\validate-agent-kit.ps1` exits with code 0.

## Test and acceptance matrix

| Behavior | Test level | Test/file | Expected result |
| --- | --- | --- | --- |
| Java toolchain is Java 25 | Script | `scripts/validate-agent-kit.ps1` | Pass |
| Backend declares Spring Boot 4.1.x and Java 25 | Script | `scripts/validate-agent-kit.ps1` | Pass |
| Frontend placeholder is React + Vite + strict TypeScript | Script | `scripts/validate-agent-kit.ps1` | Pass |
| Requirement inventory remains 106 IDs | Script | `scripts/validate-agent-kit.ps1` | Pass |

## Risks and rollback

- Data/migration risk: none; no database or migration is introduced.
- Security risk: none; no runtime authentication behavior is introduced.
- Compatibility risk: Spring Boot 4.1.x dependency availability will be proven
  by backend builds after wrapper resolution. This task first validates the
  declared baseline.
- Rollback or safe recovery: remove the new skeleton files or use Git to review
  and discard this task's changes before any commit.

## Progress log

- 2026-07-06 - Read docs and confirmed WP-S0-01 scope.
- 2026-07-06 - Verified local Java 25, Node, npm, Git, and Maven 3.9.16.
- 2026-07-06 - Created Git repository, backend skeleton, frontend placeholder,
  Maven Wrapper files, and validation script.
- 2026-07-06 - `powershell -File scripts\validate-agent-kit.ps1` passed with
  Java 25, Node v24.18.0, npm 11.6.2, Git 2.45.1, Maven 3.9.16, and Spring
  Boot parent 4.1.0.
- 2026-07-06 - `mvn clean verify` in `backend/` passed with `BUILD SUCCESS`.
- 2026-07-06 - `npm install`, `npm run lint`, `npm test -- --run`, and
  `npm run build` passed in `frontend/`.
- 2026-07-06 - Upgraded the placeholder to an actual React/Vite shell and
  reran `npm install`, `npm run lint`, `npm test -- --run`, `npm run build`,
  and `powershell -File scripts\validate-agent-kit.ps1` successfully.

## Decision log

- 2026-07-06 - Keep frontend work to a minimal toolchain placeholder because
  Mohamed assigned frontend development to VS Code.

## Unexpected findings

- The root was not yet a Git repository.
- Maven was installed globally but the current process PATH did not include the
  machine PATH update until the script refreshed it.

## Final verification

- [x] Focused tests: `powershell -File scripts\validate-agent-kit.ps1` passed.
- [x] Backend `clean verify` when affected: `mvn clean verify` passed in
  `backend/`.
- [x] Frontend lint/test/build when affected: `npm run lint`,
  `npm test -- --run`, and `npm run build` passed in `frontend/`.
- [x] Docker/health/E2E checks when affected: not affected by WP-S0-01.
- [x] Diff reviewed for unrelated changes:
- [x] Documentation and traceability updated:

## Handoff

WP-S0-01 created the repository/toolchain skeleton and passed the documented
focused proof plus backend/frontend skeleton gates. Remaining work moves to
WP-S0-02 or the next Mohamed-approved Sprint 0 task.
