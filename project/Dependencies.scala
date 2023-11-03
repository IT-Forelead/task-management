import sbt.*

object Dependencies {
  object Versions {
    lazy val circe = "0.14.1"
    lazy val skunk = "0.6.0"
    lazy val http4s = "0.23.10"
    lazy val flyway = "9.16.0"
    lazy val refined = "0.10.2"
    lazy val cats = "2.9.0"
    lazy val `cats-effect` = "3.4.8"
    lazy val logback = "1.4.7"
    lazy val log4cats = "2.5.0"
    lazy val `mu-rpc` = "0.30.3"
    lazy val ciris = "2.3.2"
    lazy val pureconfig = "0.17.2"
    lazy val `cats-retry` = "3.1.0"
    lazy val fs2 = "3.6.1"
    lazy val enumeratum = "1.7.3"
    lazy val sttp = "3.7.2"
    lazy val `http4s-jwt-auth` = "1.2.0"
    lazy val newtype = "0.4.4"
    lazy val tsec = "0.4.0"
    lazy val monocle = "3.2.0"
    lazy val redis4cats = "1.1.1"
    lazy val `cats-tagless` = "0.14.0"
    lazy val derevo = "0.13.0"
    lazy val weaver = "0.8.1"
    lazy val `test-container` = "1.17.6"
    lazy val postgresql = "42.5.4"
    lazy val awsSdk = "1.12.111"
    lazy val awsSoftwareS3 = "2.20.68"
    lazy val guava = "31.0.1-jre"
  }
  trait LibGroup {
    def all: Seq[ModuleID]
  }
  object com {
    object amazonaws extends LibGroup {
      private def awsJdk(artifact: String): ModuleID =
        "com.amazonaws" % artifact % Versions.awsSdk

      lazy val awsCore: ModuleID = awsJdk("aws-java-sdk-core")
      lazy val awsS3: ModuleID = awsJdk("aws-java-sdk-s3")
      val awsSoftwareS3: ModuleID = "software.amazon.awssdk" % "s3" % Versions.awsSoftwareS3

      override def all: Seq[ModuleID] = Seq(awsCore, awsS3, awsSoftwareS3)
    }
    object google {
      lazy val guava = "com.google.guava" % "guava" % Versions.guava
    }
    object github {
      object pureconfig extends LibGroup {
        private def repo(artifact: String): ModuleID =
          "com.github.pureconfig" %% artifact % Versions.pureconfig

        lazy val core: ModuleID = repo("pureconfig")
        lazy val enumeratum: ModuleID = repo("pureconfig-enumeratum")

        override def all: Seq[ModuleID] = Seq(core, enumeratum)
      }
      object cb372 {
        lazy val retry: ModuleID = "com.github.cb372" %% "cats-retry" % Versions.`cats-retry`
      }
    }
    object beachape {
      object enumeratum extends LibGroup {
        private def enumeratum(artifact: String): ModuleID =
          "com.beachape" %% artifact % Versions.enumeratum

        lazy val core: ModuleID = enumeratum("enumeratum")
        lazy val circe: ModuleID = enumeratum("enumeratum-circe")
        lazy val cats: ModuleID = enumeratum("enumeratum-cats")
        override def all: Seq[ModuleID] = Seq(core, circe, cats)
      }
    }
    object softwaremill {
      object sttp extends LibGroup {
        private def sttp(artifact: String): ModuleID =
          "com.softwaremill.sttp.client3" %% artifact % Versions.sttp

        lazy val circe: ModuleID = sttp("circe")
        lazy val `fs2-backend`: ModuleID = sttp("async-http-client-backend-fs2")
        override def all: Seq[ModuleID] = Seq(circe, `fs2-backend`)
      }
    }
  }

  object io {
    object circe extends LibGroup {
      private def circe(artifact: String): ModuleID =
        "io.circe" %% s"circe-$artifact" % Versions.circe

      lazy val core: ModuleID = circe("core")
      lazy val generic: ModuleID = circe("generic")
      lazy val parser: ModuleID = circe("parser")
      lazy val refined: ModuleID = circe("refined")
      lazy val optics: ModuleID = circe("optics")
      lazy val `generic-extras`: ModuleID = circe("generic-extras")
      override def all: Seq[ModuleID] =
        Seq(core, generic, parser, refined, optics, `generic-extras`)
    }
    object grpc extends LibGroup {
      private def muRpc(artifact: String): ModuleID =
        "io.higherkindness" %% artifact % Versions.`mu-rpc`

      lazy val service = muRpc("mu-rpc-service")
      lazy val server = muRpc("mu-rpc-server")
      lazy val fs2 = muRpc("mu-rpc-fs2")
      override def all: Seq[ModuleID] = Seq(service, server, fs2)
    }
    object estatico {
      lazy val newtype = "io.estatico" %% "newtype" % Versions.newtype
    }
    object github {
      object jmcardon {
        lazy val `tsec-password` = "io.github.jmcardon" %% "tsec-password" % Versions.tsec
      }
    }
  }
  object org {
    lazy val postgresql: ModuleID = "org.postgresql" % "postgresql" % Versions.postgresql

    object typelevel {
      object cats {
        lazy val core = "org.typelevel"           %% "cats-core"           % Versions.cats
        lazy val effect = "org.typelevel"         %% "cats-effect"         % Versions.`cats-effect`
        lazy val `cats-tagless` = "org.typelevel" %% "cats-tagless-macros" % Versions.`cats-tagless`
      }
      lazy val log4cats = "org.typelevel" %% "log4cats-slf4j" % Versions.log4cats
    }
    object tpolecat {
      object skunk extends LibGroup {
        private def skunk(artifact: String): ModuleID =
          "org.tpolecat" %% artifact % Versions.skunk

        lazy val core = skunk("skunk-core")
        lazy val circe = skunk("skunk-circe")
        override def all: Seq[ModuleID] = Seq(core, circe)
      }
    }

    object http4s extends LibGroup {
      private def http4s(artifact: String): ModuleID =
        "org.http4s" %% s"http4s-$artifact" % Versions.http4s

      lazy val dsl = http4s("dsl")
      lazy val server = http4s("ember-server")
      lazy val client = http4s("ember-client")
      lazy val circe = http4s("circe")
      lazy val `blaze-server` = http4s("blaze-server")
      override def all: Seq[ModuleID] = Seq(dsl, server, client, circe)
    }

    object flywaydb {
      lazy val core = "org.flywaydb" % "flyway-core" % Versions.flyway
    }
  }
  object eu {
    object timepit {
      object refined extends LibGroup {
        private def refined(artifact: String): ModuleID =
          "eu.timepit" %% artifact % Versions.refined

        lazy val core = refined("refined")
        lazy val cats = refined("refined-cats")
        lazy val pureconfig: ModuleID = refined("refined-pureconfig")

        override def all: Seq[ModuleID] = Seq(core, cats, pureconfig)
      }
    }
  }

  object ch {
    object qos {
      lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logback
    }
  }

  object co {
    object fs2 extends LibGroup {
      private def fs2(artifact: String): ModuleID =
        "co.fs2" %% s"fs2-$artifact" % Versions.fs2

      lazy val core: ModuleID = fs2("core")
      lazy val io: ModuleID = fs2("io")
      override def all: Seq[ModuleID] = Seq(core, io)
    }
  }

  object tf {
    object tofu {
      object derevo extends LibGroup {
        private def derevo(artifact: String): ModuleID =
          "tf.tofu" %% s"derevo-$artifact" % Versions.derevo

        lazy val core: ModuleID = derevo("core")
        lazy val cats: ModuleID = derevo("cats")
        override def all: Seq[ModuleID] = Seq(core, cats)
      }
    }
  }
  object dev {
    object optics {
      lazy val monocle = "dev.optics" %% "monocle-core" % Versions.monocle
    }
    object profunktor {
      object redis4cats extends LibGroup {
        private def redis4cats(artifact: String): ModuleID =
          "dev.profunktor" %% artifact % Versions.redis4cats

        lazy val catsEffects: ModuleID = redis4cats("redis4cats-effects")
        lazy val log4cats: ModuleID = redis4cats("redis4cats-log4cats")
        override def all: Seq[ModuleID] = Seq(catsEffects, log4cats)
      }
      lazy val `http4s-jwt-auth` =
        "dev.profunktor" %% "http4s-jwt-auth" % Versions.`http4s-jwt-auth`
    }
  }

  object uz {
    object scala extends LibGroup {
      lazy val common: ModuleID = "uz.scala" %% "common" % "1.0.1"
      lazy val skunk: ModuleID = "uz.scala"  %% "skunk"  % "1.0.1"
      override def all: Seq[ModuleID] = Seq(skunk)
    }
  }
}
