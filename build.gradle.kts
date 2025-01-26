plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.13.3"  // 尝试更新到最新版本
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "com.smart"
version = "1.0-SNAPSHOT"

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
    // 移除 Gson 依赖
    // implementation("com.google.code.gson:gson:2.8.9")
    // 添加hutool核心包
    implementation("cn.hutool:hutool-all:5.8.25")  // 使用最新稳定版本
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.5")  // 使用最新稳定版本
    implementation("org.commonmark:commonmark:0.24.0")  // 使用最新稳定版本
    implementation("dev.langchain4j:langchain4j:0.36.0")
    implementation("dev.langchain4j:langchain4j-open-ai:0.36.0")
    implementation("mysql:mysql-connector-java:8.0.33")
    // 如果只需要特定模块，也可以单独引入：
    // implementation 'cn.hutool:hutool-core:5.8.25'      // 核心工具
    // implementation 'cn.hutool:hutool-json:5.8.25'      // JSON工具
    // implementation 'cn.hutool:hutool-db:5.8.25'        // 数据库工具
    // implementation 'cn.hutool:hutool-http:5.8.25'      // HTTP客户端
    // implementation 'cn.hutool:hutool-crypto:5.8.25'    // 加密解密
    // implementation("com.vladsch.flexmark:flexmark-all:0.64.0")
}
