package uk.gov.hmrc.iossregistration.models.etmp

import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsError, Json, JsSuccess}
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.generators.Generators

class EtmpCustomerIdentificationSpec extends BaseSpec with Generators {

  "EtmpCustomerIdentification" - {

    "for the new model" - {

      "must serialise/deserialise to and from EtmpCustomerIdentificationNew" in {

        val etmpCustomerIdentification = arbitrary[EtmpCustomerIdentificationNew].sample.value

        val expectedJson = Json.obj(
          "idType" -> s"${etmpCustomerIdentification.idType}",
          "idValue" -> s"${etmpCustomerIdentification.idValue}"
        )

        Json.toJson(etmpCustomerIdentification) mustBe expectedJson
        expectedJson.validate[EtmpCustomerIdentification] mustBe JsSuccess(etmpCustomerIdentification)
      }
    }

    "for the legacy model" - {

      "must serialise/deserialise to and from EtmpCustomerIdentificationNew" in {

        val etmpCustomerIdentification = arbitrary[EtmpCustomerIdentificationLegacy].sample.value

        val expectedJson = Json.obj(
          "vrn" -> s"${etmpCustomerIdentification.vrn}",
        )

        Json.toJson(etmpCustomerIdentification) mustBe expectedJson
        expectedJson.validate[EtmpCustomerIdentification] mustBe JsSuccess(etmpCustomerIdentification)
      }
    }

    "must handle missing fields during deserialization" in {

      val expectedJson = Json.obj()

      expectedJson.validate[EtmpCustomerIdentification] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val expectedJson = Json.obj(
        "vrn" -> 123456789
      )

      expectedJson.validate[EtmpCustomerIdentification] mustBe a[JsError]
    }
  }
}
