/*
* Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
*/

plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`
    id("com.diffplug.spotless") version "6.19.0"
}

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

gradlePlugin {
    // Define the plugin
    val greeting by plugins.creating {
        id = "com.playframework.twirl"
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
