import sbt.*
import sbt.Keys.*

object ProjectDefaults extends AutoPlugin {
  object autoImport {
    lazy val unstash = taskKey[Unit]("Unstash cached compiled files")
    lazy val CompileAndTest = "compile->compile;test->test"
    lazy val TestOnly = "test->test"
  }

  override def trigger = allRequirements

  override def projectSettings: Seq[Def.Setting[?]] = compiler ++ other

  override def buildSettings: Seq[Def.Setting[?]] = {
    sys.props += "packaging.type" -> "jar"
    Nil
  }

  private lazy val other = Seq(
    publishMavenStyle                      := true,
    isSnapshot                             := true,
    Compile / doc / sources                := Seq.empty,
    Compile / packageDoc / publishArtifact := false,
  )

  private lazy val compiler = Seq(
    scalacOptions ++= Seq(
      "-unchecked", // Enable additional warnings where generated code depends on assumptions.
      "-deprecation", // Enable additional warnings when code uses deprecated elements.
      "-feature", // Emit warning and location for usages of features that should be imported explicitly.
      "-language:existentials", // Existential types (besides wildcard types) can be written and inferred.
      "-language:experimental.macros", // Allow macro definition (besides implementation and application).
      "-language:higherKinds", // Allow higher-kinded types.
      "-language:reflectiveCalls",
      "-language:implicitConversions", // Allow definition of implicit functions called views.
      "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
      "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
      "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
      "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
      "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
      "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
      "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
      "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
      "-Xlint:option-implicit", // Option.apply used implicit view.
      "-Xlint:package-object-classes", // Class or object defined in package object.
      "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
      "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
      "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
      "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
      "-Ywarn-dead-code", // Warn when dead code is identified.
      "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
      "-Ywarn-numeric-widen", // Warn when numerics are widened.
      "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
      "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
      "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
      "-Yrangepos",
      "-Ymacro-annotations",
      "-Wconf:" +
        "cat=deprecation:ws," +
        "cat=other-match-analysis:error," +
        "cat=w-flag-value-discard:error," +
        "cat=w-flag-numeric-widen:error," +
        "cat=lint-infer-any:error," +
        "cat=w-flag-dead-code:error," +
        "cat=lint-type-parameter-shadow:error," +
        "msg=Reference to uninitialized value:error",
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
  )
}
