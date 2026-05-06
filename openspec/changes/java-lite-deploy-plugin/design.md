# Design: Java Lite Deploy Plugin

## Architecture Overview

The plugin is designed around a small deployment pipeline engine and three lightweight UI modules.

```text
UI Layer
  ├─ Servers Panel
  ├─ Commands Panel
  └─ Deploy Panel / RunConfig Editor

Application Layer
  ├─ DeployOrchestrator
  ├─ BuildStep
  ├─ UploadStep
  ├─ BeforeCommandStep
  ├─ AfterCommandStep
  └─ TerminalStep

Infrastructure Layer
  ├─ SshClient (direct)
  ├─ SshClient (jump host)
  ├─ CredentialStoreAdapter
  ├─ PluginStateRepository
  └─ ActImportAdapter
```

## Key Technical Decisions

1. Java-only codebase
   - All plugin source code under `src/main/java`.
   - Remove Kotlin plugin usage from Gradle and plugin metadata.

2. Minimal dependency policy
   - Keep only required IntelliJ platform modules.
   - Remove Compose/Jewel and unrelated bundled plugin dependencies.
   - Use Swing-based IntelliJ native UI APIs for consistency and lower overhead.

3. Unified deployment pipeline
   - Both ToolWindow trigger and Run/Debug trigger use the same orchestrator.
   - Stage execution model:
     - `PREPARE`
     - `BUILD` (optional Maven/Gradle command)
     - `UPLOAD`
     - `BEFORE_COMMANDS`
     - `AFTER_COMMANDS`
     - `OPEN_TERMINAL` (optional)

4. Secure credential handling
   - Sensitive data stored via IntelliJ Password Safe.
   - Persistent plugin state stores only non-sensitive references.

5. Import compatibility strategy
   - Implement ACT import through an adapter that maps source config into internal domain model.
   - Provide import preview and conflict strategy (skip/overwrite/rename).

## Domain Model

- `ServerProfile`
  - `id`, `name`, `host`, `port`, `username`, `authType`, `credentialRef`
  - `jumpHostEnabled`, `jumpHostRef`
  - `tags`, `description`

- `CommandTemplate`
  - `id`, `name`, `content`, `timeoutSeconds`, `failFast`, `executionType`
  - `executionType`: `BEFORE`, `AFTER`, `TERMINAL`

- `UploadConfig`
  - `id`, `localPath`, `isDirectory`, `remotePath`
  - `filters`, `overwritePolicy`

- `DeployProfile`
  - `id`, `name`, `serverRef`, `uploadRef`, `beforeCommands`, `afterCommands`, `terminalCommand`
  - `buildTool`, `buildCommand`, `enabledStages`

- `DeployExecutionRecord`
  - `id`, `profileRef`, `startTime`, `endTime`, `status`, `stageLogs`

## UI Design Notes

1. ToolWindow has three tabs only: Servers, Commands, Deploy.
2. Prefer compact forms and progressive disclosure (advanced options collapsed by default).
3. Real-time logs in Deploy tab with per-stage status badges.
4. Search-first interactions for servers and profiles.

## Run/Debug Integration

- Custom `ConfigurationType` + `ConfigurationFactory`.
- Run configuration binds one `DeployProfile`.
- Execution path delegates to `DeployOrchestrator`.

## I18n

- Message bundle keys in Java resource bundles.
- Start with `messages_zh_CN.properties` and `messages_en.properties`.
- Ensure all UI labels and notifications are key-based.

## Risks and Mitigations

1. SSH jump host behavior differs across target environments
   - Mitigation: define clear connection strategy and add connection test workflow.

2. ACT config format/version drift
   - Mitigation: adapter layer with version sniffing and import diagnostics report.

3. Over-minimizing dependencies may remove required APIs
   - Mitigation: trim in phases and run plugin sandbox verification after each removal.
