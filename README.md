# sbt-jars

This plugin provides `TaskKeys` to gather together all needed JARs (compiled, dependencies and unmanaged) for an project. The intention for this plugin is to be used by other plugins.

## Usage

To use this plugin in your own plugin, add the following lines to your plugins `build.sbt` file:

~~~ scala
resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

addSbtPlugin("de.choffmeister" % "sbt-jars" % "0.0.1-SNAPSHOT")
~~~

## License

Published under the permissive [MIT](http://opensource.org/licenses/MIT) license.

## Credit

Credit goes to the following projects, that gave inspiration on how to gather together all JARs:

* [sbt-pack](https://github.com/xerial/sbt-pack)
