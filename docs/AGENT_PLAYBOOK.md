# Codex Agent Playbook

## Repository setup

Open Codex at the independent project root:

```powershell
cd D:\Reactprojects\WerkPilotVLBG-agent-kit
```

Trust the repository so project `.codex/config.toml` and nested `AGENTS.md`
files can load. Start a new Codex session after changing durable instructions.

## Recommended task loop

1. Select the next dependency-ready item in `IMPLEMENTATION_BACKLOG.md`.
2. Give Codex the goal, relevant task ID, constraints, and completion proof.
3. Use Plan mode for ambiguous work and an execution-plan file for substantial
   work.
4. Let the agent inspect existing code before editing.
5. Require relevant tests and full affected quality gates.
6. Review the diff and ask for a regression/security pass.
7. Update the plan and traceability before closing the task.
8. Stop for Mohamed's approval at every sprint boundary.

Avoid two concurrent agents editing the same files or migrations. Parallel work
is safe only when ownership boundaries are explicit.

## Starter goal prompt

```text
Implement backlog item WP-S0-01 from docs/IMPLEMENTATION_BACKLOG.md.
Read AGENTS.md and all applicable nested instructions first. Keep the approved
Java 25 / Spring Boot 4.1.x / React TypeScript baseline. Create and maintain an
execution plan from docs/plans/TEMPLATE.md. The task is done only when the
documented wrapper/build commands pass, the repository contains no unrelated
changes, and the plan records exact verification results.
```

## Feature prompt

```text
Implement backlog item <TASK-ID> and only its coherent dependencies.
Trace the work to these requirement IDs: <IDS>. Read the source-derived product,
architecture, API, security, domain, and test documents that apply. Do not
reopen OQ-001..OQ-013: all are resolved in DOCX v2.1 Section 36.3. If a derived
file conflicts with v2.1, the DOCX wins and the conflict is reported. Add
tests for success, validation, unauthenticated, forbidden, and domain edge cases
as applicable. Run the relevant focused checks and the full affected quality
gate, inspect the diff, update traceability, and report remaining risks.
```

## Debugging prompt

```text
Analyze this failing test or build error in the WerkPilot repository. Reproduce
it first. Explain the root cause briefly, patch only the minimal related files,
preserve the approved public behavior, add a regression test, and run the
smallest relevant test followed by the affected quality gate. Do not rewrite
unrelated files or change requirement behavior to make the test pass.
```

## Review prompt

```text
Review the current diff against AGENTS.md, backlog item <TASK-ID>, and linked
requirement IDs <IDS>. Prioritize correctness, authorization gaps, KPI/data
errors, migration safety, import atomicity, API compatibility, and missing
tests. Report concrete findings with file/line references. If no blocking
finding remains, list the commands/evidence still required before merge.
```

## Change-control prompt

```text
Prepare a change request for <CHANGE>. Do not implement it. Extract the affected
v2.1 sections and requirement IDs, verify it against OOS-01..OOS-15 and Section
36.2 automatic rejection rules, and document business, security, data, API, and
testing impact using docs/decisions/TEMPLATE.md. The request is not authoritative
unless Mohamed approves it and the contractual DOCX is rebaselined.
```

## Handoff checklist

Every agent handoff states:

- task and requirement IDs;
- files and behavior changed;
- migrations or API changes;
- tests added;
- exact commands run and their result;
- remaining risks or detected source conflicts;
- next smallest safe task.
