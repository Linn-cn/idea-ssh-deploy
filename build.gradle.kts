plugins {
    id("java")
    alias(libs.plugins.intellijPlatform)
}

group = "com.sshdeploy"
version = "0.0.1"

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
        intellijIdea(providers.gradleProperty("platformVersion"))
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
            <b>0.0.1</b><br/>
            <ul>
                <li>SSH server profiles (password or private key), with credentials in IDE Password Safe</li>
                <li>Reusable command templates for Run/Debug configurations</li>
                <li>File match rules (built-in + user-defined regex) and “Select regex” + Apply in directory upload mode</li>
                <li>Run/Debug configuration: SFTP upload, optional local Maven/Gradle build, before/after remote commands, optional post-success terminal line</li>
                <li>Tool window: Servers, Commands, File match rules, Console</li>
                <li>Import Alibaba Cloud Toolkit (ACT) style XML</li>
                <li>English and Simplified Chinese UI (Settings → SSH Deploy)</li>
                <li>JSON configuration backup: export/import servers, commands, and user file match rules (deduplicated import)</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }
}
