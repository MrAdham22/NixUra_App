---
name: nixura-verification-and-release
description: Verifies NixUra changes via tests and builds, then commits and safely pushes with Conventional Commits. Use after completing a scoped task or when the user asks to verify, commit, or push.
disable-model-invocation: true
---

# NixUra verification and release

## Verify

1. `git status` and `git diff` — preserve unrelated changes.
2. Run checks relevant to the change (e.g. `:androidApp:assembleDebug`, `:shared:testAndroidHostTest`, common tests).
3. Report failures honestly; do not claim tests passed if they did not.
4. Scan diff for secrets, debug noise, unintended generated files.

## Commit

- Conventional Commits (e.g. `feat:`, `fix:`, `docs:`, `test:`).
- Only verified, scoped changes.
- Message explains **why** when non-obvious.

## Push

1. Confirm remote and branch (`git remote -v`, current branch).
2. Integrate remote changes safely if needed — **never force-push**.
3. No secrets; respect branch protection.
4. Report **commit hash**, message, test results, and **whether push succeeded**.

## After success

Update [docs/DEVELOPMENT_STATUS.md](../../../docs/DEVELOPMENT_STATUS.md) with verification results, commit hash, and push status.

## Stop safely when

Tests fail, auth fails, push blocked, or merge conflicts need user action.
