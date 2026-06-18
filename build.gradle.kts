plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("org.apache.commons:commons-compress:1.26.1")
    implementation("com.github.luben:zstd-jni:1.5.6-3")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    runServer {
        minecraftVersion("1.21.1")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version , "description" to project.description )
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("org.apache.commons.compress", "net.oryn.mc.orynTunnelv2.libs.commons.compress")
        relocate("com.github.luben.zstd", "net.oryn.mc.orynTunnelv2.libs.zstd")
    }

    build {
        dependsOn(shadowJar)
    }
}
