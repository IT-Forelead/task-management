name := "supports"

lazy val support_sttp = project.in(file("sttp"))
lazy val support_services = project.in(file("services"))
lazy val support_logback = project.in(file("logback"))
lazy val support_redis = project.in(file("redis"))
lazy val support_database = project.in(file("database"))

aggregateProjects(
  support_sttp,
  support_services,
  support_logback,
  support_redis,
  support_database,
)
