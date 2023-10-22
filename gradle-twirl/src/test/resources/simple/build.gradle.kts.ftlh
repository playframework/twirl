plugins {
  application
  id("org.playframework.twirl")
}

repositories {
  mavenCentral()
  mavenLocal()
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
      constructorAnnotations.add("@java.lang.Deprecated()")
    }
  }
}
