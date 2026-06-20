plugins {
    id("java-library")
    id("maven-publish")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.4.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.oryn.my.id")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("net.oryn.mc:orynplugins:1.0.1")
    implementation("com.github.luben:zstd-jni:1.5.7-11")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "oryntunnelv2"
            version = project.version.toString()

            artifact(tasks.shadowJar)

            pom {
                name.set("Oryn-Tunnelv2")
                description.set("A Cloudflare Tunnel Plugin For Tunneling Web To Cloudflare")
                url.set("https://github.com/Fahry-a/Oryn-Tunnelv2")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("Fahry-a")
                        name.set("Fahry-a")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/Fahry-a/Oryn-Tunnelv2.git")
                    developerConnection.set("scm:git:ssh://github.com/Fahry-a/Oryn-Tunnelv2.git")
                    url.set("https://github.com/Fahry-a/Oryn-Tunnelv2")
                }
            }
        }
    }

    repositories {
        maven {
            name = "LocalRepo"
            url = uri(layout.buildDirectory.dir("repo"))
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
                "Main-Class" to "net.oryn.mc.orynTunnelv2.module.TunnelModule"
            )
        }
    }

    shadowJar {
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }
}
