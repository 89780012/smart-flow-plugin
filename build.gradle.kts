plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.16.1"  // 尝试更新到最新版本
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.smart"
version = "1.1.6"

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
    localPath.set("D:\\ideaIU-2023.2.win")
    // 会远程下载一个
    //version.set("2023.2")  // 更新到较新的IDE版本
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
    
    patchPluginXml {
        sinceBuild.set("231")  //IntelliJ IDEA Ultimate IU-231.9423.9
        untilBuild.set("")
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
    implementation("org.json:json:20231013")
    implementation("org.jetbrains:annotations:24.0.0")
//    implementation("org.apache.httpcomponents:httpclient:4.5.13")
//    implementation("com.google.code.gson:gson:2.8.9")
}
