/*
* Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
*/

import java.time.Duration
import java.util.Base64
import kotlin.text.Charsets.UTF_8

plugins {
    id("com.gradle.plugin-publish") version "1.2.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    id("com.diffplug.spotless") version "6.19.0"
    signing
}

// group = "com.playframework" // group and plugin id must use same top level namespace
group = "com.typesafe.play" // TODO: uncomment line above and remove this
version = "0.0.1-SNAPSHOT"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit Jupiter test framework
            useJUnitJupiter("5.9.1")
        }

        // Create a new test suite
        val functionalTest by registering(JvmTestSuite::class) {
            dependencies {
                // functionalTest test suite depends on the production code in tests
                implementation(project())
            }

            targets {
                all {
                    // This test suite should run after the built-in test suite has run its tests
                    testTask.configure { shouldRunAfter(test) }
                }
            }
        }
    }
}

signing {
    val signingKey = Base64.getDecoder().decode(System.getenv("PGP_SECRET").orEmpty()).toString(UTF_8)
    val signingPassword = System.getenv("PGP_PASSPHRASE").orEmpty()
    useInMemoryPgpKeys(signingKey, signingPassword)
}

nexusPublishing {
    packageGroup.set(project.group.toString())
    clientTimeout.set(Duration.ofMinutes(60))
    this.repositories {
        sonatype()
    }
}

gradlePlugin {
    website.set("https://www.playframework.com/documentation/latest/ScalaTemplates")
    vcsUrl.set("https://github.com/playframework/twirl")
    // Define the plugin
    val greeting by plugins.creating {
        id = "com.typesafe.play.twirl" // TODO: rename to "com.playframework.twirl"
        displayName = "Twirl Plugin"
        description = "A Gradle plugin to compile Twirl templates"
        tags.set(listOf("playframework", "web", "template", "java", "scala"))
        implementationClass = "play.twirl.gradle.TwirlPlugin"
    }
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

val headerLicense = "Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>"
val headerLicenseHash = "# $headerLicense"
val headerLicenseJava = "/*\n* $headerLicense\n*/"

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

tasks.named<Task>("check") {
    // Include functionalTest as part of the check lifecycle
    dependsOn(testing.suites.named("functionalTest"))
}
