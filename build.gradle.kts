plugins {
    id("java-library")
    id("maven-publish")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.4.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.oryn.my.id/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("net.oryn.mc:orynplugins:1.2.0")
    implementation("com.github.luben:zstd-jni:1.5.7-11")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

publishing {
    repositories {
        maven {
            name = "orynRepo"
            url = uri("https://maven.oryn.my.id/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "oryntunnelv2"
            version = project.version.toString()
            artifact(tasks.shadowJar)
        }
    }
}

tasks {
    runServer {
        minecraftVersion("1.21.1")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        manifest {
            attributes(
                "Main-Class" to "net.oryn.mc.orynTunnelv2.module.TunnelModule",
                "Module-Name" to "tunnel"
            )
        }
    }

    shadowJar {
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }

    withType<PublishToMavenRepository> {
        dependsOn("jar")
        dependsOn("shadowJar")
    }
}
