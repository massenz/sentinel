// Copyright (c) 2014 AlertAvert.com.  All rights reserved.

import scoverage.ScoverageSbtPlugin.ScoverageKeys._

scalaVersion := "2.11.4"

name := "sentinel"

version := "0.4-SNAPSHOT"

organization := "AlertAvert.com"

scalacOptions ++= Seq("-deprecation", "-feature", "-language:postfixOps")

// This is necessary to avoid failures due to sbt not propagating system properties.
// See: http://stackoverflow.com/questions/10155870/setting-system-properties-with-sbt-run
fork in Test := false

// Code coverage and support for coveralls.io
// See: https://github.com/scoverage/sbt-scoverage
coverageExcludedPackages := "<empty>;controllers\\..*Reverse.*;"

coverageMinimum := 80

coverageFailOnMinimum := true

// The REST project depends on Core Sentinel classes
lazy val sentinel_core = project

lazy val sentinel = (project in file("."))
    .enablePlugins(PlayScala)
    .aggregate(sentinel_core)
    .dependsOn(sentinel_core)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalatestplus" %% "play" % "1.2.0" % "test",
  cache
)
