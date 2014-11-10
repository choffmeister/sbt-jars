import de.choffmeister.sbt.JarsPlugin._

version := "0.1.2"

jarsSettings

TaskKey[Unit]("check-jars-runtime") <<= (jarsRuntime) map { (jars) =>
  assert(jars.map(_._1.getName) == "duplicate-jars_2.10-0.1.2.jar" :: "module1_2.10-0.1-SNAPSHOT.jar" :: "module2_2.10-0.1-SNAPSHOT.jar" :: Nil)
}

TaskKey[Unit]("check-jars-dependencies") <<= (jarsDependencies) map { (jars) =>
  assert(jars.map(_._1.getName) == "scala-library.jar" :: "slf4j-api-1.7.6.jar" :: Nil)
}
