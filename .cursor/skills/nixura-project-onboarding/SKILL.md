---
name: nixura-project-onboarding
description: Recovers NixUra project context for new Cursor chats or agent sessions. Reads AGENTS.md, docs, architecture, git state, and compares docs to the repo. Use when starting work on NixUra, resuming after a break, or when the user asks to onboard or catch up.
disable-model-invocation: true
---

# NixUra project onboarding

Do not implement features automatically when this skill is invoked.

## Steps

1. Read root [AGENTS.md](../../../AGENTS.md).
2. Read [docs/PROJECT_CONTEXT.md](../../../docs/PROJECT_CONTEXT.md).
3. Read [docs/DEVELOPMENT_STATUS.md](../../../docs/DEVELOPMENT_STATUS.md).
4. Read [docs/PHASE2_ARCHITECTURE.md](../../../docs/PHASE2_ARCHITECTURE.md).
5. Inspect repository structure (`:shared`, `:androidApp`, `docs/`, `.cursor/skills/`).
6. Run `git status`, `git branch`, `git log -5 --oneline`, and `git remote -v`.
7. Compare documentation claims to actual code (phase, features, dependencies).
8. Report inconsistencies between docs and repo — do not silently pick one.
9. Summarize: current phase, completed work, open decisions, **next authorized task**.
10. If authorization for the requested work is unclear, **stop and ask the user**.

## Output template

- **Phase / authorization**
- **Doc vs repo mismatches** (if any)
- **Git:** branch, clean/dirty, last commit
- **Recommended next step**
