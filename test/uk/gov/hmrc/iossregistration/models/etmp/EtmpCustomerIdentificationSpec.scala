package uk.gov.hmrc.iossregistration.models.etmp

import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.generators.Generators

class EtmpCustomerIdentificationSpec extends BaseSpec with Generators {

  "EtmpCustomerIdentification" - {

    "must serialise/deserialise to and from EtmpCustomerIdentification" in {

      val etmpCustomerIdentification = arbitrary[EtmpCustomerIdentification].sample.value

      val expectedJson = Json.obj(
        "vrn" -> s"${etmpCustomerIdentification.vrn.vrn}"
      )

      Json.toJson(etmpCustomerIdentification) mustBe expectedJson
      expectedJson.validate[EtmpCustomerIdentification] mustBe JsSuccess(etmpCustomerIdentification)
    }
  }
}
