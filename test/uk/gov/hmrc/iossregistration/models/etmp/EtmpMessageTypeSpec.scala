package uk.gov.hmrc.iossregistration.models.etmp

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

class EtmpMessageTypeSpec extends BaseSpec with ScalaCheckPropertyChecks {

  "EtmpMessageType" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(EtmpMessageType.values)

      forAll(gen) {
        etmpMessageType =>

          JsString(etmpMessageType.toString).validate[EtmpMessageType].asOpt.value mustBe etmpMessageType
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String].suchThat(!EtmpMessageType.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValues =>

          JsString(invalidValues).validate[EtmpMessageType] mustBe JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(EtmpMessageType.values)

      forAll(gen) {
        etmpMessageType =>

          Json.toJson(etmpMessageType) mustBe JsString(etmpMessageType.toString)
      }
    }
  }
}
