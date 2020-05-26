import sbt.Keys._
import sbt._
import sbt.io.IO

object Playdoc extends AutoPlugin {

  object autoImport {
    final val Docs       = config("docs")
    val playdocDirectory = settingKey[File]("Base directory of play documentation")
    val playdocPackage   = taskKey[File]("Package play documentation")
  }

  import autoImport._

  override def requires = sbt.plugins.JvmPlugin

  override def trigger = noTrigger

  override def projectSettings =
    Defaults.packageTaskSettings(playdocPackage, mappings in playdocPackage) ++
      Seq(
        playdocDirectory := (baseDirectory in ThisBuild).value / "docs" / "manual",
        mappings in playdocPackage := {
          val base: File = playdocDirectory.value
          base.allPaths.pair(IO.relativize(base.getParentFile(), _))
        },
        artifactClassifier in playdocPackage := Some("playdoc"),
        artifact in playdocPackage ~= { _.withConfigurations(Vector(Docs)) }
      ) ++
      addArtifact(artifact in playdocPackage, playdocPackage)

}
