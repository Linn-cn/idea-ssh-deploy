<p align="center">
  <img src="src/main/resources/META-INF/pluginIcon.svg" alt="SSH Deploy" width="120" height="120">
</p>

<p align="center">在 IntelliJ IDEA 中通过 <b>SSH</b> / <b>SFTP</b> 将本地构建产物上传到远端 Linux 主机的插件</p>

<div align="center">
  <a href="#"><img src="https://img.shields.io/badge/IntelliJ%20Platform-2025.3%2B-blue" alt="Platform"></a>
  <a href="#"><img src="https://img.shields.io/badge/Java-21-orange" alt="Java"></a>
</div>

[**English**](README.md)

**SSH Deploy** 用于在 IDE 内管理 SSH 服务器、可复用的远端命令以及部署配置。可提供可选的本地构建（Maven/Gradle）、SFTP 上传、上传**前/后**远程命令（流式日志），以及可选的 **Terminal** 后续操作——**工具窗口**与 **运行/调试配置** 共用同一套流水线。

功能范围刻意保持精简：主机管理、上传与命令执行，便于专注开发与部署闭环，而非云平台控制台类的大而全能力。

> [!TIP]
> 服务器只需配置一次（密码或私钥），之后在工具窗口触发部署，或使用 **SSH Deploy** 运行配置绑定日常流程。

## 功能

- [x] **SSH 服务器**的添加 / 编辑 / 删除 / 搜索（密码或私钥）
- [x] **命令模板** — 保存片段，在部署配置或运行配置中复用
- [x] **上传流水线** — 可选本地构建、SFTP 上传、上传**前后**远端命令、可选 **Terminal**
- [x] **运行/调试配置** — 选择服务器、本地上传源（文件或目录+规则）、远端路径、上传前后命令、成功后可选单行终端命令
- [x] 远端命令中支持占位符 **`${fileName}`**（例如带版本号的 JAR 名）
- [x] 导入 **Alibaba Cloud Toolkit（ACT）** 风格 XML，便于迁移
- [x] **英文**与**简体中文**界面（可跟随 IDE 或在设置中指定）
- [x] **连接测试**、**演练模式**及各阶段的控制台日志

## 环境要求

- 带内置 **Java** 与 **Terminal** 支持的 **IntelliJ IDEA**（最低兼容版本见 `gradle.properties` 中的 `pluginSinceBuild`）
- 远端需支持标准 **SSH** 与 **SFTP**

## 使用说明（概要）

**工具窗口**

1. 打开 **视图 → 工具窗口 → SSH Deploy**（或使用右侧工具窗口栏图标）。
2. 在 **服务器** 中维护主机与凭据（可使用 IDE 密码保险箱）。
3. 在 **命令** 中维护可复用的 Shell 行。
4. 在 **部署** 中关联服务器、上传映射及可选的上传前后或终端步骤，然后执行 **部署**、**测试连接** 或 **演练模式**。

**运行/调试**

1. **运行 → 编辑配置 → + → SSH Deploy**
2. 选择服务器、本地上传源、远端目录及可选命令 / 终端行。
3. 运行后日志显示在 **运行** 工具窗口中。

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
