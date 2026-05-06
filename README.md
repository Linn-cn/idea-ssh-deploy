<p align="center">
  <img src="src/main/resources/META-INF/pluginIcon.svg" alt="SSH Deploy" width="120" height="120">
</p>

<p align="center">IntelliJ IDEA plugin for uploading build artifacts to remote Linux hosts over <b>SSH</b> / <b>SFTP</b></p>

<div align="center">
  <a href="#"><img src="https://img.shields.io/badge/IntelliJ%20Platform-2025.3%2B-blue" alt="Platform"></a>
  <a href="#"><img src="https://img.shields.io/badge/Java-21-orange" alt="Java"></a>
</div>

[**简体中文**](README_zh_CN.md)

**SSH Deploy** helps you manage SSH servers, reusable remote commands, and deployment profiles inside the IDE. Run an optional local build (Maven/Gradle), upload via SFTP, execute **before/after** shell steps with streaming logs, and optionally open **Terminal** for follow-up work—the same pipeline is available from the **SSH Deploy** tool window and from a **Run/Debug configuration**.

The scope is intentionally narrow: host management, upload, and command execution—similar in spirit to lightweight deploy tooling, without cloud-console extras.

> [!TIP]
> Configure servers once (password or private key), then deploy from the tool window or bind a **SSH Deploy** Run configuration to your workflow.

## Features

- [x] Add / edit / remove / search **SSH server** profiles (password or private key)
- [x] **Command templates** — save snippets and reuse them in deploy profiles or Run configurations
- [x] **Upload pipeline** — optional local build, SFTP upload, remote commands **before** and **after** upload, optional **Terminal** tab
- [x] **Run/Debug configuration** — choose server, local artifact (file or directory + pattern), remote path, before/after commands, optional one-line terminal command after success
- [x] Placeholder **`${fileName}`** in remote commands for the uploaded artifact name (e.g. versioned JAR)
- [x] Import **Alibaba Cloud Toolkit (ACT)**-style XML for faster migration
- [x] **English** and **Simplified Chinese** UI (follow IDE language or override in Settings)
- [x] **Connection test**, **dry run**, and console logging for deploy stages

## Requirements

- **IntelliJ IDEA** with bundled **Java** and **Terminal** support (see `gradle.properties` for the minimum build compatible with `pluginSinceBuild`)
- Remote hosts must support standard **SSH** and **SFTP**

## Usage (overview)

**Tool window**

1. Open **View → Tool Windows → SSH Deploy** (or the tool window button on the right).
2. Under **Servers**, add hosts and credentials (IDE Password Safe supported).
3. Under **Commands**, maintain reusable shell lines.
4. Under **Deploy**, link server, upload mapping, and optional before/after or terminal steps—then **Deploy**, **Test connection**, or **Dry run**.

**Run/Debug**

1. **Run → Edit Configurations → + → SSH Deploy**
2. Pick server, local upload source, remote directory, and optional commands / terminal line.
3. Run; logs appear in the **Run** tool window.

## Development

Prerequisites: **JDK 21**, Gradle (wrapper included).

| Task | Command |
|------|---------|
| Compile | `./gradlew compileJava` (Windows: `gradlew.bat compileJava`) |
| Run IDE with plugin | `./gradlew runIde` |
| Build plugin distribution | `./gradlew buildPlugin` |

The sandbox IDE and packaged ZIP are produced under `build/` (see IntelliJ Platform Gradle Plugin docs for paths).

```text
.
├── src/main/java/          Plugin sources (Java)
├── src/main/resources/
│   ├── META-INF/plugin.xml
│   ├── icons/              Tool window / Run config icons
│   └── messages/           i18n bundles
├── build.gradle.kts
├── gradle.properties       Platform version & since-build
└── settings.gradle.kts
```

## Support

If this project is useful to you, consider starring the repository.

Questions or bugs are welcome via **Issues** (when hosted on a forge) or your team’s usual channel.
