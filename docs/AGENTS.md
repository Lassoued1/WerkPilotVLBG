# Documentation Agent Instructions

These instructions extend the repository-level `AGENTS.md` for `docs/`.

- Keep developer documentation in English. Preserve approved German UI wording
  when documenting labels or messages.
- `WerkPilot_VLBG_Cahier_des_Charges_v2.1.docx` is the final contractual
  baseline and always wins. Derived
  documents must cite its section and requirement IDs instead of weakening or
  silently reinterpreting it.
- The 106-ID inventory is unchanged in v2.1. Update
  `requirements/REQUIREMENTS.csv` only after the DOCX is formally rebaselined.
- OQ-001..OQ-013 and CR-001 are resolved in Section 36.3. Keep their labels for
  traceability; never present them as open or block implementation on them.
- Record future proposed deviations as dated change records in `decisions/`.
  A local record does not override the DOCX; do not rewrite history.
- Report a new source ambiguity to Mohamed rather than inventing behavior or
  reopening a resolved OQ.
- Keep execution plans factual and current. Move completed plans to
  `plans/completed/`.
- Never claim that an acceptance item passed without linking the evidence or
  exact verification command.
