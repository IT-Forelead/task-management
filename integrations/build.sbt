name         := "integrations"
lazy val integration_opersms = project.in(file("opersms"))

aggregateProjects(
  integration_opersms
)
