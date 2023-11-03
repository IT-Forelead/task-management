name := "integrations"
lazy val integration_opersms = project.in(file("opersms"))
lazy val integration_aws = project.in(file("aws"))

aggregateProjects(
  integration_opersms,
  integration_aws,
)
