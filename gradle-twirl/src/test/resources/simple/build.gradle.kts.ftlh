plugins {
  application
  id("com.typesafe.play.twirl")
}

repositories {
  mavenCentral()
  mavenLocal()
}

dependencies {
  implementation("com.typesafe.play:twirl-api_${scalaVersion}:${twirlVersion}")
}

twirl {
  scalaVersion.set("${scalaVersion}")
}
