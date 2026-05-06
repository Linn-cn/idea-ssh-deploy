# Tasks: Java Lite Deploy Plugin

## Phase 0 - Baseline Cleanup (Java-only and dependency trim)

- [x] 0.1 Remove Kotlin source/template files and create Java equivalents for plugin entry points.
- [x] 0.2 Update `build.gradle.kts`:
  - remove Kotlin/Compose plugin usage
  - keep only required IntelliJ platform plugin modules
- [x] 0.3 Update `plugin.xml` to remove unnecessary `<depends>` and ensure Java-based extension classes.
- [x] 0.4 Verify plugin runs in sandbox after cleanup.

## Phase 1 - Core Domain and Persistence

- [x] 1.1 Define domain models: `ServerProfile`, `CommandTemplate`, `UploadConfig`, `DeployProfile`.
- [x] 1.2 Implement `PersistentStateComponent` repository for non-sensitive config.
- [x] 1.3 Implement credential adapter using Password Safe.
- [x] 1.4 Implement validation layer for profile completeness and path/command sanity checks.

## Phase 2 - SSH and Deploy Pipeline

- [x] 2.1 Implement SSH direct connection client and file upload capability.
- [x] 2.2 Implement SSH jump host connectivity mode.
- [x] 2.3 Implement pipeline orchestrator with stage model and structured logs.
- [x] 2.4 Add fail-fast and timeout behavior per command stage.
- [x] 2.5 Add connection test and dry-run validation actions.

## Phase 3 - UI (Lightweight)

- [x] 3.1 Build Servers panel (add/edit/delete/search/test connection).
- [x] 3.2 Build Commands panel (add/edit/delete command templates).
- [x] 3.3 Build Deploy panel (profile select, run, logs, terminal open).
- [ ] 3.4 Add plugin settings page (defaults, timeouts, encoding, behavior flags).

## Phase 4 - Run/Debug and I18n

- [x] 4.1 Implement custom Run/Debug configuration type for deployment.
- [x] 4.2 Connect Run/Debug execution to the same orchestrator.
- [x] 4.3 Externalize all user-facing text to message bundles.
- [x] 4.4 Provide zh-CN/en-US translations for core flows.

## Phase 5 - ACT Compatibility Import

- [x] 5.1 Research and document ACT config sources/format assumptions.
- [x] 5.2 Implement ACT adapter to map:
  - SSH connection fields
  - deploy path
  - `afterCommand`
  - `terminalCommand`
- [x] 5.3 Build import wizard with preview and conflict resolution options.
- [x] 5.4 Add import result report (success/warn/fail items).

## Phase 6 - Hardening and Release Readiness

- [x] 6.1 Add integration tests for pipeline stages and error paths.
- [ ] 6.2 Add import compatibility tests for representative ACT samples.
- [ ] 6.3 Perform UX polish pass for compactness and clarity.
- [x] 6.4 Finalize plugin metadata, change notes, and packaging checks.
