import play.core.PlayVersion
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.19.0"
  private val hmrcMongoVersion = "2.10.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "uk.gov.hmrc"             %% "domain-play-30"             % "10.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % hmrcMongoVersion,
    "org.scalatestplus"       %% "scalacheck-1-15"            % "3.2.11.0",
    "org.scalacheck"          %% "scalacheck"                 % "1.18.1"
  ).map(_ % "test, it")
}
