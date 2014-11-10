import de.choffmeister.sbt.JarsPlugin._

version := "0.1.2"

jarsSettings

libraryDependencies ++= Seq(
  "log4j" % "log4j" % "1.2.17"
)

TaskKey[Unit]("check-jars-runtime") <<= (jarsRuntime) map { (jars) =>
  assert(jars.map(_._1.getName) == "simple2_2.10-0.1.2.jar" :: Nil)
}

TaskKey[Unit]("check-jars-dependencies") <<= (jarsDependencies) map { (jars) =>
  assert(jars.map(_._1.getName) == "scala-library.jar" :: "log4j-1.2.17.jar" :: Nil)
}
