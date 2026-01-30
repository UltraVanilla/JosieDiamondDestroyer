plugins {
    java
    `java-library`
    `maven-publish`
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.diffplug.spotless") version "6.23.3"
    id("xyz.jpenilla.run-paper") version "2.2.2"
}

group = "josie.diamonddestroyer"
version = "1.1.0"
description =
    "Destroys ores when TNT duping is detected"

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name = "${project.name} ${project.version}"
                description = project.description
                url = "https://github.com/UltraVanilla/${project.name}"

                licenses {
                    license {
                        name = "AGPL-3.0-or-later"
                        url = "https://www.gnu.org/licenses/agpl-3.0.html"
                        distribution = "repo"
                    }
                }

                developers {
                    developer {
                        name = "lordpipe"
                        organization = "UltraVanilla"
                        organizationUrl = "https://ultravanilla.world/"
                    }
                    developer {
                        name = "JosieToolkit Contributors"
                    }
                }

                scm {
                    url = "https://github.com/UltraVanilla/${project.name}"
                    connection = "scm:https://UltraVanilla@github.com/UltraVanilla/${project.name}.git"
                    developerConnection = "scm:git://github.com/UltraVanilla/${project.name}.git"
                }

                issueManagement {
                    system = "GitHub"
                    url = "https://github.com/UltraVanilla/${project.name}/issues"
                }
            }
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.codemc.org/repository/maven-public/") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = 21
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName = project.name
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("1.21.4")
    }
}

spotless {
    isEnforceCheck = false
    encoding("UTF-8")
    format("misc") {
        target("*.gradle.kts", ".gitignore", "README.md")
        indentWithSpaces()
        endWithNewline()
        trimTrailingWhitespace()
    }
    java {
        target("**/*.java")
        indentWithSpaces()
        endWithNewline()
        trimTrailingWhitespace()
        removeUnusedImports()
        palantirJavaFormat()
    }
}

paper {
    // To fork and rebrand:
    //
    // ```kotlin
    // name = "JosieDiamondDestroyerFork"
    // provides = listOf("JosieDiamondDestroyer")
    // authors = listOf("ForkAuthor")
    // contributors = listOf("lordpipe", "JosieToolkit Contributors", "UltraVanilla Contributors")
    // website = "https://gitlab.com/..."
    // ```
    authors = listOf("lordpipe", "JosieToolkit Contributors", "UltraVanilla Contributors")
    website = "https://ultravanilla.world/"

    main = "${project.group}.${project.name}"
    apiVersion = "1.21"

    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.STARTUP
}
