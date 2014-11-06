import sbt._
import sbt.Keys._

object Build extends sbt.Build {
  lazy val buildSettings = Seq(
    scalaVersion := "2.10.4",
    scalacOptions ++= Seq("-encoding", "utf8"),
    sbtPlugin := true)

  lazy val publishSettings = Seq(
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"))

  lazy val mavenInfos = {
    <url>https://github.com/choffmeister/sbt-jars</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>http://opensource.org/licenses/MIT</url>
      </license>
    </licenses>
    <scm>
      <url>github.com/choffmeister/sbt-jars.git</url>
      <connection>scm:git:github.com/choffmeister/sbt-jars.git</connection>
      <developerConnection>scm:git:git@github.com:choffmeister/sbt-jars.git</developerConnection>
    </scm>
    <developers>
      <developer>
        <id>choffmeister</id>
        <name>Christian Hoffmeister</name>
        <url>http://choffmeister.de/</url>
      </developer>
    </developers> }

  lazy val root = (project in file("."))
    .settings(Defaults.defaultSettings: _*)
    .settings(buildSettings: _*)
    .settings(publishSettings: _*)
    .settings(pomExtra := mavenInfos)
    .settings(test <<= (publishLocal, baseDirectory, version) map { (_, baseDirectory, pluginVersion) =>
      val testSbts = (baseDirectory / "src/sbt-test").listFiles
      testSbts.foreach { testSbt =>
        IO.withTemporaryDirectory { baseDir =>
          val projectDir = baseDir / "project"
          baseDir.mkdirs()
          projectDir.mkdirs()

          IO.write(baseDir / "build.sbt",
            """|import de.choffmeister.sbt.JarsPlugin._
               |
               |%s
               |
               |jarsSettings""".stripMargin.format(IO.read(testSbt)))

          IO.write(projectDir / "build.properties",
            """|sbt.version=0.13.6
               |""".stripMargin)

          IO.write(projectDir / "plugins.sbt",
            """|addSbtPlugin("de.choffmeister" %% "sbt-jars" %% "%s")
               |""".stripMargin.format(pluginVersion))

          Process("sbt" ::
            "show jars-runtime" ::
            "show jars-dependencies" ::
            "show jars-all" :: Nil, baseDir) !
        }
      }
    })
    .settings(
      name := "sbt-jars",
      organization := "de.choffmeister",
      organizationName := "Christian Hoffmeister",
      organizationHomepage := Some(new URL("http://choffmeister.de/")),
      version := "0.0.1-SNAPSHOT")
}
