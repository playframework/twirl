import sbt.Keys._
import sbt._

class Publish(isLibrary: Boolean) extends AutoPlugin {

  import bintray.BintrayPlugin
  import bintray.BintrayPlugin.autoImport._

  override def trigger = noTrigger

  override def requires = BintrayPlugin

  val (releaseRepo, snapshotRepo) =
    if (isLibrary)
      ("maven", "snapshots")
    else
      ("sbt-plugin-releases", "sbt-plugin-snapshots")

  override def projectSettings =
    Seq(
      bintrayOrganization := Some("playframework"),
      bintrayRepository := (if (isSnapshot.value) snapshotRepo else releaseRepo),
      bintrayPackage := "twirl",
      // maven style should only be used for libraries, not for plugins
      publishMavenStyle := isLibrary,
      bintrayPackageLabels := {
        val labels = Seq("playframework", "twirl")
        if (isLibrary) labels
        else labels :+ "plugin"
      }
    )
}

object PublishLibrary extends Publish(isLibrary = true)

object PublishSbtPlugin extends Publish(isLibrary = false) {
  import sbt.ScriptedPlugin.autoImport._
  override def projectSettings =
    super.projectSettings ++ Seq(
      scriptedLaunchOpts += version.apply { v => s"-Dproject.version=$v" }.value
    )
}
