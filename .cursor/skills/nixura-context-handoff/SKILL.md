---
name: nixura-context-handoff
description: Prepares NixUra handoff for a new chat by updating DEVELOPMENT_STATUS and summarizing state. Use when the conversation is long or the user wants to continue in a new session.
disable-model-invocation: true
---

# NixUra context handoff

## Steps

1. Review current task and `git status` / `git diff`.
2. Update [docs/DEVELOPMENT_STATUS.md](../../../docs/DEVELOPMENT_STATUS.md):
   - Current task status (complete / partial / blocked) and what is verified vs unverified
   - Update the "What is left" list
   - Files touched this session
   - Checks run and **actual** outcomes
   - Commit hash and push status (if any)
   - Uncommitted work that must be preserved
   - Approved vs proposed decisions from the session
   - Next authorized task
3. Update other docs only if a decision was **approved** (architecture → PHASE2 + PROJECT_CONTEXT).
4. Do **not** commit unfinished work as complete.
5. Do **not** discard uncommitted changes.

## Handoff message template

List files the next session must read:

1. `AGENTS.md`
2. `docs/DEVELOPMENT_STATUS.md`
3. `docs/PROJECT_CONTEXT.md`
4. `docs/PHASE2_ARCHITECTURE.md` (if architecture touched)

Suggest invoking **nixura-project-onboarding** in the new chat.

## Documentation auto-update (mandatory, every time)

After **any** modification, decide whether `README.md` or other docs are now stale and update them **automatically, in the same change, without waiting to be asked**. The full table of "changed X → update Y" is in [AGENTS.md](../../../AGENTS.md) ("Documentation auto-update rule"). At minimum: README for features/architecture/stack/build/test changes; `docs/DEVELOPMENT_STATUS.md` for fixes, bugs found, verification results, risks and the "what is left" list; `docs/PROJECT_CONTEXT.md` for scope/data-model changes. Say which docs you updated in your final reply.
