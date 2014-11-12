package de.choffmeister.sbt

import sbt._
import sbt.Keys._

object JarsPlugin extends Plugin {
  private val defaultVersionOrdering = new Ordering[String] {
    override def compare(a: String, b: String): Int = {
      DefaultVersionStringOrdering.compare(VersionString(a), VersionString(b))
    }
  }

  val jarsExcludeProjects = SettingKey[Seq[String]]("jars-exclude-projects")
  val jarsDuplicatedStrategy = SettingKey[DuplicatedJarStrategy]("jars-duplicated-strategy")
  val jarsUpdateReports = TaskKey[Seq[(UpdateReport, ProjectRef)]]("jars-update-reports")
  val jarsRuntime = TaskKey[Seq[(File, ProjectRef)]]("jars-runtime")
  val jarsDependencies = TaskKey[Seq[(File, ProjectRef)]]("jars-dependencies")
  val jarsUnmanaged = TaskKey[Seq[(File, ProjectRef)]]("jars-unmanaged")
  val jarsAll = TaskKey[Seq[(File, ProjectRef)]]("jars-all")

  lazy val jarsSettings = Seq[Def.Setting[_]](
    jarsExcludeProjects := Seq.empty,
    jarsDuplicatedStrategy := DuplicatedJarStrategies.Latest(defaultVersionOrdering),
    jarsUpdateReports <<= (thisProjectRef, buildStructure, jarsExcludeProjects) flatMap getFromSelectedProjects(update),
    jarsRuntime <<= (thisProjectRef, buildStructure, jarsExcludeProjects) flatMap getFromSelectedProjects(packageBin in Runtime),
    jarsDependencies <<= (streams, jarsUpdateReports, jarsDuplicatedStrategy) map { (streams, updates, duplicateStrategy) =>
      val dependentJars =
        for {
          (r, projectRef) <- updates
          c <- r.configurations if c.configuration == "runtime"
          m <- c.modules
          (artifact, file) <- m.artifacts if DependencyFilter.allPass(c.configuration, m.module, artifact)}
        yield
          JarEntry(
            m.module.organization,
            m.module.name,
            m.module.revision,
            artifact.classifier,
            file.getName,
            projectRef,
            file)

      val distinctDependentJars = dependentJars
        .groupBy(_.noVersionJarName)
        .map {
          case (key, entries) if entries.groupBy(_.version).size == 1 => entries.head
          case (key, entries) =>
            val versions = entries.groupBy(_.version).map(_._1).toList
            duplicateStrategy match {
              case DuplicatedJarStrategies.Latest(ordering) =>
                val latest = entries.sortBy(_.version)(ordering).last
                streams.log.warn(s"Version conflict on $key. Using ${latest.version} (found ${versions.mkString(", ")})")
                latest
              case DuplicatedJarStrategies.Error =>
                sys.error(s"Version conflict on $key (found ${versions.mkString(", ")})")
            }
        }
        .toSeq

      distinctDependentJars.map(entry => (entry.file, entry.projectRef))
    },
    jarsUnmanaged <<= (thisProjectRef, buildStructure, jarsExcludeProjects) flatMap getFromSelectedProjects(unmanagedJars in Runtime) map { x =>
      for ((m, projectRef) <- x; um <- m) yield um.data -> projectRef
    },
    jarsAll <<= (jarsRuntime, jarsDependencies, jarsUnmanaged) map { (runtime, dependencies, unmanaged) =>
      runtime ++ dependencies ++ unmanaged
    })

  private def getFromAllProjects[T](targetTask: TaskKey[T])(currentProject: ProjectRef, structure: BuildStructure): Task[Seq[(T, ProjectRef)]] =
    getFromSelectedProjects(targetTask)(currentProject, structure, Seq.empty)

  private def getFromSelectedProjects[T](targetTask: TaskKey[T])(currentProject: ProjectRef, structure: BuildStructure, exclude: Seq[String]): Task[Seq[(T, ProjectRef)]] = {
    def allProjectRefs(currentProject: ProjectRef): Seq[ProjectRef] = {
      def isExcluded(p: ProjectRef) = exclude.contains(p.project)
      val children = Project.getProject(currentProject, structure).toSeq.flatMap(_.uses)
      (currentProject +: (children flatMap allProjectRefs)) filterNot isExcluded
    }

    val projects: Seq[ProjectRef] = allProjectRefs(currentProject).distinct
    projects.map(p => Def.task(((targetTask in p).value, p)) evaluate structure.data).join
  }

  case class JarEntry(
      organization: String,
      name: String,
      version: String,
      classifier: Option[String],
      originalFileName: String,
      projectRef: ProjectRef,
      file: File) {
    private def classifierSuffix = classifier.map("-" + _).getOrElse("")
    def jarName = "%s-%s%s.jar".format(name, version, classifierSuffix)
    def fullJarName = "%s.%s-%s%s.jar".format(organization, name, version, classifierSuffix)
    def noVersionJarName = "%s.%s%s.jar".format(organization, name, classifierSuffix)
    override def toString = "%s:%s:%s%s".format(organization, name, version, classifierSuffix)
  }

  sealed trait DuplicatedJarStrategy
  object DuplicatedJarStrategies {
    case class Latest(ordering: Ordering[String]) extends DuplicatedJarStrategy
    case object Error extends DuplicatedJarStrategy
  }
}
