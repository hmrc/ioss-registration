package uk.gov.hmrc.iossregistration.models.core

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, Json}

class SourceTypeSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "SourceType" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(SourceType.values)

      forAll(gen) {
        sourceType =>

          JsString(sourceType.toString).validate[SourceType].asOpt.value mustBe sourceType
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!SourceType.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[SourceType] mustBe JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(SourceType.values)

      forAll(gen) {
        sourceType =>

          Json.toJson(sourceType) mustBe JsString(sourceType.toString)
      }
    }
  }
}

