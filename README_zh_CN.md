<p align="center">
  <img src="src/main/resources/META-INF/pluginIcon.svg" alt="SSH Deploy" width="120" height="120">
</p>

<p align="center">在 IntelliJ IDEA 中通过 <b>SSH</b> / <b>SFTP</b> 将本地构建产物上传到远端 Linux 主机的插件</p>

<div align="center">
  <a href="#"><img src="https://img.shields.io/badge/IntelliJ%20Platform-2025.3%2B-blue" alt="Platform"></a>
  <a href="#"><img src="https://img.shields.io/badge/Java-21-orange" alt="Java"></a>
</div>

[**English**](README.md)

**SSH Deploy** 用于在 IDE 内管理 SSH 服务器、可复用的远端命令以及文件匹配规则预设。可提供可选的本地构建（Maven/Gradle）、SFTP 上传、上传**前/后**远程命令（流式日志），以及可选的 **Terminal** 后续操作——主要入口是 **运行/调试配置**；工具窗口负责服务器/命令/规则管理与日志控制台。

功能范围刻意保持精简：主机管理、上传与命令执行，便于专注开发与部署闭环，而非云平台控制台类的大而全能力。

> [!TIP]
> 服务器只需配置一次（密码或私钥），之后在工具窗口触发部署，或使用 **SSH Deploy** 运行配置绑定日常流程。

## 功能

- [x] **SSH 服务器**的添加 / 编辑 / 删除 / 搜索（密码或私钥）
- [x] **命令模板** — 多行片段，支持插入 `${fileName}` 占位符；在运行配置中复用（从 ACT 导入的配置文件中也会引用）
- [x] **文件匹配规则** — 内置模板（只读）与用户自定义「名称 + 正则」；运行配置在「目录匹配」模式下可从 **选择正则** 下拉选用，点 **应用** 填入下方正则框
- [x] **上传流水线** — 可选本地构建、SFTP 上传、上传**前后**远端命令、可选 **Terminal**
- [x] **运行/调试配置** — 选择服务器、本地上传源（文件或目录 + 正则）、远端路径、上传前后命令、成功后可选单行终端命令
- [x] 远端命令中支持占位符 **`${fileName}`**（例如带版本号的 JAR 名）
- [x] 导入 **Alibaba Cloud Toolkit（ACT）** 风格 XML，便于迁移
- [x] **英文**与**简体中文**界面（可跟随 IDE 或在设置中指定）
- [x] **配置备份** — **设置 → SSH Deploy** 全量 JSON；工具窗口服务器 / 命令 / 文件匹配规则页可分别 **导出… / 导入…**（分项导入也可选择完整备份，只读取该项；追加合并并去重）
- [x] **远程命令重试** — 上传前/后命令失败时自动再试最多 2 次（共 3 次），并重连 SSH
- [x] **连接测试**、**演练模式**及各阶段的控制台日志

## 环境要求

- 带内置 **Java** 与 **Terminal** 支持的 **IntelliJ IDEA**（最低兼容版本见 `gradle.properties` 中的 `pluginSinceBuild`）
- 远端需支持标准 **SSH** 与 **SFTP**

## 使用说明（概要）

**工具窗口**（**SSH Deploy** 内的分栏标签）

1. 打开 **视图 → 工具窗口 → SSH Deploy**（或使用右侧工具窗口栏图标）。
2. **服务器** — 维护主机与凭据（可使用 IDE 密码保险箱）。可用 **导出… / 导入…** 仅备份或恢复服务器，也可从完整备份中只导入服务器。
3. **命令** — 维护可在运行配置中引用的多行 Shell 片段（与运行配置相同的多行编辑器，右侧可插入 `${fileName}` 占位符）。可用 **导出… / 导入…** 仅处理命令。
4. **文件匹配规则** — 可选：维护名称 + 正则（内置行不可删除）；运行配置「目录匹配」时 **选择正则** 下列表来自此处。可用 **导出… / 导入…** 仅处理用户规则。
5. **控制台** — 部分部署相关输出会在此追加显示。

**设置**

- **设置 → 工具 → SSH Deploy** — 语言/默认项、**全量** JSON 配置备份，以及 ACT XML 导入。

**运行/调试**

1. **运行 → 编辑配置 → + → SSH Deploy**
2. 选择服务器、本地上传源（指定文件或目录 + 文件匹配正则）、远端目录及可选命令 / 终端行。
3. 在 **目录匹配** 模式下，于 **选择正则** 中选预设后点 **应用**，可将正则填入输入框（仍可手动修改）。
4. 运行后日志显示在 **运行** 工具窗口中。远程命令失败时会自动重试（最多 3 次），并在日志中提示每次重试。

## 开发构建

需要 **JDK 21**，Gradle 可使用仓库自带的 Wrapper。

| 任务 | 命令 |
|------|------|
| 编译 | `./gradlew compileJava`（Windows：`gradlew.bat compileJava`） |
| 沙箱运行插件 | `./gradlew runIde` |
| 打包插件 | `./gradlew buildPlugin` |

产物位于 `build/` 目录（具体路径见 IntelliJ Platform Gradle 插件文档）。

```text
.
├── src/main/java/          插件源码（Java）
├── src/main/resources/
│   ├── META-INF/plugin.xml
│   ├── icons/              工具窗口 / 运行配置图标
│   └── messages/           国际化资源
├── build.gradle.kts
├── gradle.properties       平台版本与 since-build
└── settings.gradle.kts
```

## 支持

若本项目对你有帮助，欢迎给仓库点个 Star。

建议或问题可通过 **Issues**（托管在代码平台时）或团队常用渠道反馈。
