plugins {
    id("java-library")
    id("maven-publish")
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.oryn.my.id/releases")
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.orynplugins)
    implementation(libs.commons.compress)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
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
        minecraftVersion(libs.versions.minecraft.get())
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
