package uk.gov.hmrc.iossregistration.models.etmp

import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsSuccess, Json}
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
  }
}
