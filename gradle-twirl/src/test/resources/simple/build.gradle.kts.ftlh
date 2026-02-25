plugins {
  application
  id("org.playframework.twirl")
}

repositories {
  mavenCentral()
  mavenLocal()
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(${javaVersion}))
  }
}

dependencies {
  implementation("org.playframework.twirl:twirl-api_${scalaVersion}:${twirlVersion}")
}

twirl {
  scalaVersion.set("${scalaVersion}")
}

sourceSets {
  main {
    twirl {
      templateImports.add("java.lang._")
      templateImports.add("a.b.%format%._")
      constructorAnnotations.add("@java.lang.Deprecated()")
    }
  }
}
