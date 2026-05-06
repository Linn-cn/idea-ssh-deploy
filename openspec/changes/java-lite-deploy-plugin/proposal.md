# Proposal: Java Lite Deploy Plugin

## Why

Current deployment plugins provide many cloud-specific or broad operational features that are not required for the target workflow. The desired experience is focused: one-click packaging and deployment of Spring Boot services to Linux hosts, with optional pre/post command execution and remote terminal access. The existing project template also includes unnecessary Kotlin/Compose dependencies, which increases build complexity and maintenance cost.

## Goals

1. Build a lightweight IntelliJ IDEA plugin for deployment workflows only.
2. Use Java implementation only (no Kotlin source, no Compose UI dependency).
3. Minimize plugin dependencies to only what is required for:
   - Host management
   - Command management
   - Upload and deployment pipeline
   - Run/Debug integration
   - I18n and settings
4. Support migration/import of Alibaba Cloud Toolkit related deployment configuration fields:
   - SSH connection settings
   - Deploy path in Run/Debug configuration
   - `afterCommand`
   - `terminalCommand`

## Non-goals

1. Rebuilding full Alibaba Cloud Toolkit capabilities.
2. Providing cloud-provider-specific resource management.
3. Supporting complex orchestration clusters in the first release.

## Scope

### In scope

- CRUD + search for server profiles
- CRUD for command templates
- File/directory upload configuration
- Before/after command execution
- Remote terminal entry after deployment
- SSH jump host support
- Run/Debug deployment configuration
- I18n framework (initial zh-CN/en-US)
- Plugin-level settings
- ACT-compatible import for required fields

### Out of scope (initial release)

- Full ACT compatibility for unrelated modules
- Multi-host parallel rollout strategies
- Visual dashboards and analytics

## Success Criteria

1. A user can configure one deployment profile and complete package -> upload -> restart in one run.
2. The plugin is Java-only and removes Kotlin/Compose dependencies from build and plugin descriptors.
3. A user can import supported ACT fields and execute deployment without manual re-entry for imported fields.
4. Run/Debug configuration can trigger deployment with the same core pipeline as manual execution.
