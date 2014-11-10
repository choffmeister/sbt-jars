import sbt._
import sbt.Keys._
import de.choffmeister.sbt.JarsPlugin._

object Build extends sbt.Build {
  val commonSettings = Defaults.defaultSettings ++ jarsSettings

  lazy val root = Project(
    id = "duplicate-jars",
    base = file("."),
    settings = commonSettings
  ) dependsOn(module1, module2)

  lazy val module1 = Project(
    id = "module1",
    base = file("module1"),
    settings = commonSettings ++ Seq(
      libraryDependencies ++= Seq("org.slf4j" % "slf4j-api" % "1.7.2" force())
    )
  )

  lazy val module2 = Project(
    id = "module2",
    base = file("module2"),
    settings = commonSettings ++ Seq(
      libraryDependencies ++= Seq("org.slf4j" % "slf4j-api" % "1.7.6")
    )
  )
}
