plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.13.3"  // 尝试更新到最新版本
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "com.smart"
version = "1.0.4"

repositories {
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
    maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    //本地安装目录
//    localPath.set("D:\\ideaIU-2023.2.win")
    // 会远程下载一个
    version.set("2022.2.5")  // 确保这与您的目标 IDE 版本匹配
    type.set("IC") // Target IDE Platform

    plugins.set(listOf("com.intellij.java"))
    updateSinceUntilBuild.set(false)
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
//    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//        kotlinOptions.jvmTarget = "17"
//    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("232.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    buildSearchableOptions {
        enabled = false
    }

    // 添加这个配置来处理重复文件
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles() // 合并服务文件，如果需要的话
    }
}

sourceSets {
    main {
        java.srcDirs("src/main/java")
        resources.srcDirs("src/main/resources")
    }
}

dependencies {
    // 添加hutool核心包
    implementation("cn.hutool:hutool-core:5.8.25")      // 核心工具
    implementation("cn.hutool:hutool-json:5.8.25")      // JSON工具
    implementation("org.commonmark:commonmark:0.24.0")  // 使用最新稳定版本
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.json:json:20231013")

}
