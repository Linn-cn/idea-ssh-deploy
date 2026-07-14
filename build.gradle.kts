plugins {
    id("java")
    alias(libs.plugins.intellijPlatform)
}

group = "com.sshdeploy"
version = "0.0.5"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    implementation("com.github.mwiede:jsch:0.2.24")
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        val localIdePath = providers.gradleProperty("localIdePath")
        if (localIdePath.isPresent) {
            local(localIdePath)
        } else {
            intellijIdea(providers.gradleProperty("platformVersion"))
        }
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.plugins.terminal")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }

        // Patched into plugin.xml at build time — keep in sync with src/main/resources/META-INF/plugin.xml change-notes.
        changeNotes = """
            <b>0.0.5</b><br/>
            <ul>
                <li>Run configuration: add SSH server inline next to the server dropdown (reuses the full server form)<br/>运行配置：在 SSH 服务器下拉旁可内联新增服务器（复用完整服务器表单）</li>
                <li>Command templates: types General / Before upload / After upload / Terminal; command management shows and edits type<br/>命令模板：支持类型「通用 / 上传前 / 上传后 / 终端」；命令管理可展示与编辑类型</li>
                <li>Run configuration: command dropdowns filtered by slot type (+ General); save current input into command management<br/>运行配置：命令下拉按槽位类型筛选（含通用）；可将当前输入保存到命令管理</li>
            </ul>
            <b>0.0.4</b><br/>
            <ul>
                <li>Tool window: per-section JSON export/import for Servers, Commands, and File match rules (also accepts a full backup and reads only that section)<br/>工具窗口：服务器 / 命令 / 文件匹配规则支持分项 JSON 导出导入（也可选择完整备份并只读取对应项）</li>
                <li>Remote commands: on failure, automatically retry up to 2 more times (3 attempts total) with reconnect; progress shown in Run logs<br/>远端命令：失败后自动再试最多 2 次（共 3 次）并重连；进度显示在运行日志中</li>
                <li>Migrate off Terminal / JediTerm / FileChooser / SAXBuilder APIs scheduled for removal or deprecated<br/>迁移弃用或计划移除的 Terminal / JediTerm / FileChooser / SAXBuilder API</li>
                <li>Upload: prefer faster SSH ciphers (AES-GCM / ChaCha20 first)<br/>上传：优先使用更快的 SSH 加密算法（AES-GCM / ChaCha20 优先）</li>
                <li>Upload: refresh progress every 2% instead of every 1%<br/>上传：进度改为每 2% 刷新一次（原为每 1%）</li>
            </ul>
            <b>0.0.3</b><br/>
            <ul>
                <li>Run configuration: browse remote upload directory on the selected SSH server (SFTP)<br/>运行配置：可在所选 SSH 服务器上浏览远端上传目录（SFTP）</li>
                <li>Run configuration: preview server, command, and regex presets in dropdown lists<br/>运行配置：服务器、命令、正则预设下拉支持预览</li>
                <li>Command management: multi-line preview in the command list (up to 5 lines)<br/>命令管理：列表中支持多行命令预览（最多 5 行）</li>
                <li>Run tool window logs now include timestamps like the tool-window Console tab<br/>运行工具窗口日志增加时间戳，与工具窗口控制台一致</li>
                <li>Remote upload directory browser: default start at <code>/</code>, reuse SFTP session, retry on transient disconnects<br/>远端目录浏览器：默认从 <code>/</code> 开始，复用 SFTP 会话，瞬时断连时自动重试</li>
            </ul>
            <b>0.0.2</b><br/>
            <ul>
                <li>Command management add/edit: multi-line command editor with <code>${'$'}{fileName}</code> placeholder insertion (same as Run configuration)<br/>命令管理新增/编辑：多行命令编辑器，支持插入 <code>${'$'}{fileName}</code> 占位符（与运行配置一致）</li>
                <li>Settings → SSH Deploy: clearer layout for JSON backup and ACT import sections<br/>设置 → SSH Deploy：JSON 备份与 ACT 导入分区布局更清晰</li>
            </ul>
            <b>0.0.1</b><br/>
            <ul>
                <li>SSH server profiles (password or private key), with credentials in IDE Password Safe<br/>SSH 服务器配置（密码或私钥），凭据保存在 IDE 密码保险箱</li>
                <li>Reusable command templates for Run/Debug configurations<br/>可在运行/调试配置中复用的命令模板</li>
                <li>File match rules (built-in + user-defined regex) and “Select regex” + Apply in directory upload mode<br/>文件匹配规则（内置 + 用户自定义正则）；目录上传模式下支持「选择正则」并应用</li>
                <li>Run/Debug configuration: SFTP upload, optional local Maven/Gradle build, before/after remote commands, optional post-success terminal line<br/>运行/调试配置：SFTP 上传、可选本地 Maven/Gradle 构建、上传前后远端命令、成功后可选终端命令</li>
                <li>Tool window: Servers, Commands, File match rules, Console<br/>工具窗口：服务器、命令、文件匹配规则、控制台</li>
                <li>Import Alibaba Cloud Toolkit (ACT) style XML<br/>支持导入阿里云 Toolkit（ACT）风格 XML</li>
                <li>English and Simplified Chinese UI (Settings → SSH Deploy)<br/>英文与简体中文界面（设置 → SSH Deploy）</li>
                <li>JSON configuration backup: export/import servers, commands, and user file match rules (deduplicated import)<br/>JSON 配置备份：导出/导入服务器、命令与用户文件匹配规则（导入去重）</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }
}
