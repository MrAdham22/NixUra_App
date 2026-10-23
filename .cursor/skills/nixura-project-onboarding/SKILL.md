---
name: nixura-project-onboarding
description: Recovers NixUra project context for new Cursor chats or agent sessions. Reads AGENTS.md, docs, architecture, git state, and compares docs to the repo. Use when starting work on NixUra, resuming after a break, or when the user asks to onboard or catch up.
disable-model-invocation: true
---

# NixUra project onboarding

Do not implement features automatically when this skill is invoked. (Onboarding itself changes no files — but if you notice stale docs, report them, and fix them once the user asks for work.)

## Steps

1. Read root [AGENTS.md](../../../AGENTS.md).
2. Read [docs/PROJECT_CONTEXT.md](../../../docs/PROJECT_CONTEXT.md).
3. Read [docs/DEVELOPMENT_STATUS.md](../../../docs/DEVELOPMENT_STATUS.md).
4. Read [docs/PHASE2_ARCHITECTURE.md](../../../docs/PHASE2_ARCHITECTURE.md) (including its addendum) and skim [docs/AI_COACH_UPGRADE.md](../../../docs/AI_COACH_UPGRADE.md).
5. Inspect repository structure (`:shared`, `:androidApp`, `docs/`, `.cursor/skills/`).
6. Run `git status`, `git branch`, `git log -5 --oneline`, and `git remote -v`.
7. Compare documentation claims to actual code (phase, features, dependencies).
8. Report inconsistencies between docs and repo — do not silently pick one.
9. Summarize: what the app is, what is verified vs unverified (see DEVELOPMENT_STATUS "Verification state"), the "What is left" list, and the **next authorized task** (check DEVELOPMENT_STATUS; today that is normally iOS bring-up and iOS notifications, since Android already builds and runs).
10. If authorization for the requested work is unclear, **stop and ask the user**.

## Output template

- **Phase / authorization**
- **Doc vs repo mismatches** (if any)
- **Git:** branch, clean/dirty, last commit
- **Recommended next step**

## Documentation auto-update (mandatory, every time)

After **any** modification, decide whether `README.md` or other docs are now stale and update them **automatically, in the same change, without waiting to be asked**. The full table of "changed X → update Y" is in [AGENTS.md](../../../AGENTS.md) ("Documentation auto-update rule"). At minimum: README for features/architecture/stack/build/test changes; `docs/DEVELOPMENT_STATUS.md` for fixes, bugs found, verification results, risks and the "what is left" list; `docs/PROJECT_CONTEXT.md` for scope/data-model changes. Say which docs you updated in your final reply.
