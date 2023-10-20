package uk.gov.hmrc.iossregistration.models.etmp

import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.generators.Generators

class EtmpAdministrationSpec extends BaseSpec with ScalaCheckPropertyChecks with Generators {

  "EtmpAdministration" - {

    "must serialise/deserialise to and from EtmpAdministration" in {

      val etmpAdministration = arbitrary[EtmpAdministration].sample.value

      val expectedJson = Json.obj(
        "messageType" -> s"${etmpAdministration.messageType}",
        "regimeID" -> s"${etmpAdministration.regimeID}"
      )

      Json.toJson(etmpAdministration) mustBe expectedJson
      expectedJson.validate[EtmpAdministration] mustBe JsSuccess(etmpAdministration)
    }
  }
}