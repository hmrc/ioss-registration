package uk.gov.hmrc.iossregistration.models.etmp

import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec


class EtmpTradingNameSpec extends BaseSpec {

  "EtmpTradingName" - {

    "must serialise/deserialise to and from EtmpTradingName" in {

      val etmpTradingName = arbitrary[EtmpTradingName].sample.value

      val expectedJson = Json.obj(
        "tradingName" -> s"${etmpTradingName.tradingName}"
      )

      Json.toJson(etmpTradingName) mustBe expectedJson
      expectedJson.validate[EtmpTradingName] mustBe JsSuccess(etmpTradingName)
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "tradingName" -> "Test Trading Name"
      )

      val expectedTradingName = EtmpTradingName("Test Trading Name")
      json.validate[EtmpTradingName] mustBe JsSuccess(expectedTradingName)
    }

    "must handle missing fields during deserialization" in {
      val json = Json.obj()


      json.validate[EtmpTradingName] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {
      val json = Json.obj(
        "tradingName" -> 12345
      )

      json.validate[EtmpTradingName] mustBe a[JsError]
    }
  }
}
