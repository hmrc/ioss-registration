package uk.gov.hmrc.iossregistration.models.etmp

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

class SchemeTypeSpec extends BaseSpec with ScalaCheckPropertyChecks {

  "SchemeType" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(SchemeType.values)

      forAll(gen) {
        schemeType =>

          JsString(schemeType.toString).validate[SchemeType].asOpt.value mustBe schemeType
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String].suchThat(!SchemeType.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValues =>

          JsString(invalidValues).validate[SchemeType] mustBe JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(SchemeType.values)

      forAll(gen) {
        schemeType =>

          Json.toJson(schemeType) mustBe JsString(schemeType.toString)
      }
    }
  }
}
