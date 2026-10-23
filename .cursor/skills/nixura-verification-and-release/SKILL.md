---
name: nixura-verification-and-release
description: Verifies NixUra changes via tests and builds, then commits and safely pushes with Conventional Commits. Use after completing a scoped task or when the user asks to verify, commit, or push.
disable-model-invocation: true
---

# NixUra verification and release

## Verify

1. `git status` and `git diff` — preserve unrelated changes.
2. Run checks relevant to the change (if Gradle cannot run in your environment, say so explicitly — never imply a build passed) (e.g. `:androidApp:assembleDebug`, `:shared:testAndroidHostTest`, common tests; on macOS also `:shared:iosSimulatorArm64Test`, `:shared:linkDebugFrameworkIosSimulatorArm64` and an `xcodebuild`/Xcode run of `iosApp`). iOS changes made on a machine without macOS stay marked unverified.
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

## After success (and after any fix)

Update [docs/DEVELOPMENT_STATUS.md](../../../docs/DEVELOPMENT_STATUS.md) with verification results, commit hash, and push status.

## Stop safely when

Tests fail, auth fails, push blocked, or merge conflicts need user action.

## Documentation auto-update (mandatory, every time)

After **any** modification, decide whether `README.md` or other docs are now stale and update them **automatically, in the same change, without waiting to be asked**. The full table of "changed X → update Y" is in [AGENTS.md](../../../AGENTS.md) ("Documentation auto-update rule"). At minimum: README for features/architecture/stack/build/test changes; `docs/DEVELOPMENT_STATUS.md` for fixes, bugs found, verification results, risks and the "what is left" list; `docs/PROJECT_CONTEXT.md` for scope/data-model changes. Say which docs you updated in your final reply.
