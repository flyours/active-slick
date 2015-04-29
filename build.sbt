scalaVersion in ThisBuild := "2.11.6"

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

organization := "io.strongtyped"

crossScalaVersions := Seq("2.10.5", "2.11.6")

fork in run := true