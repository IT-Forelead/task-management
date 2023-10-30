name         := "integrations"
scalaVersion := "2.13.12"

lazy val integration_opersms = project.in(file("opersms"))

aggregateProjects(
  integration_opersms
)
