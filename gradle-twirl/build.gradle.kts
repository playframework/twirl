/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
import java.time.Duration
import java.util.Base64
import java.util.Properties
import kotlin.text.Charsets.UTF_8

plugins {
    alias(libs.plugins.gradle.plugin.publish)
    alias(libs.plugins.nexus.publish)
    alias(libs.plugins.spotless)
    signing
}

val compilerVersionKey = "twirl.compiler.version"
val compilerVersion: String = System.getProperty(compilerVersionKey) ?: Properties().apply {
    val file = file("$projectDir/../compiler/version.properties")
    if (!file.exists()) throw GradleException("Install Twirl Compiler to local Maven repository by `sbt +compiler/publishM2` command")
    file.inputStream().use { load(it) }
    if (this.getProperty(compilerVersionKey).isNullOrEmpty()) {
        throw GradleException("`$compilerVersionKey` key didn't find in ${file.absolutePath}")
    }
}.getProperty(compilerVersionKey)

val isRelease = !compilerVersion.endsWith("SNAPSHOT")

group = "org.playframework.twirl"
version = compilerVersion

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    compileOnly("org.playframework.twirl:twirl-compiler_2.13:$compilerVersion")
    testImplementation(libs.assertj)
    testImplementation(libs.commons.io)
    testImplementation(libs.commons.lang)
    testImplementation(libs.freemarker)
}

tasks.jar {
    manifest {
        attributes("Implementation-Version" to version)
    }
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            targets {
                all {
                    testTask.configure {
                        systemProperty("twirl.version", compilerVersion)
                        project.findProperty("scala.version")?.let { scalaVersion ->
                            val ver = (scalaVersion as String).trimEnd { !it.isDigit() }
                            systemProperty("scala.version", ver)
                        }
                        // Required to test configuration cache in tests when using withDebug()
                        // https://github.com/gradle/gradle/issues/22765#issuecomment-1339427241
                        jvmArgs(
                            "--add-opens",
                            "java.base/java.util=ALL-UNNAMED",
                            "--add-opens",
                            "java.base/java.util.concurrent.atomic=ALL-UNNAMED",
                            "--add-opens",
                            "java.base/java.lang.invoke=ALL-UNNAMED",
                            "--add-opens",
                            "java.base/java.net=ALL-UNNAMED",
                        )
                    }
                }
            }
        }
    }
}

signing {
    isRequired = isRelease
    if (isRelease) {
        val signingKey =
            Base64.getDecoder().decode(System.getenv("PGP_SECRET").orEmpty()).toString(UTF_8)
        val signingPassword = System.getenv("PGP_PASSPHRASE").orEmpty()
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}

nexusPublishing {
    packageGroup.set(project.group.toString())
    clientTimeout.set(Duration.ofMinutes(60))
    this.repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

@Suppress("UnstableApiUsage")
gradlePlugin {
    website.set("https://www.playframework.com/documentation/latest/ScalaTemplates")
    vcsUrl.set("https://github.com/playframework/twirl")
    val twirl by plugins.creating {
        id = "org.playframework.twirl"
        displayName = "Twirl Plugin"
        description = "A Gradle plugin to compile Twirl templates"
        tags.set(listOf("playframework", "web", "template", "java", "scala"))
        implementationClass = "play.twirl.gradle.TwirlPlugin"
    }
}

val headerLicense =
    "Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>"
val headerLicenseHash = "# $headerLicense"
val headerLicenseJava = "/*\n * $headerLicense\n */"

spotless {
    java {
        googleJavaFormat()
        licenseHeader(headerLicenseJava)
    }
    kotlinGradle {
        licenseHeader(headerLicenseJava, "[^/*]")
    }
    format("properties") {
        target("**/*.properties")
        targetExclude("gradle/**")
        licenseHeader(headerLicenseHash, "[^#]")
    }
}
