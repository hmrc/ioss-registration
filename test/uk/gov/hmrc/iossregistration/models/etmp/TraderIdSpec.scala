package uk.gov.hmrc.iossregistration.models.etmp

import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

class TraderIdSpec extends BaseSpec {

  "TraderId" - {

    "must deserialise/serialise to and from VatNumberTraderId" in {

      val vatNumberTraderId = arbitrary[VatNumberTraderId].sample.value

      val expectedJson = Json.obj(
        "vatNumber" -> s"${vatNumberTraderId.vatNumber}"
      )

      Json.toJson(vatNumberTraderId) mustBe expectedJson
      expectedJson.validate[TraderId] mustBe JsSuccess(vatNumberTraderId)
    }

    "must deserialise/serialise to and from TaxRefTraderID" in {

      val taxRefTraderID = arbitrary[TaxRefTraderID].sample.value

      val expectedJson = Json.obj(
        "taxReferenceNumber" -> s"${taxRefTraderID.taxReferenceNumber}"
      )

      Json.toJson(taxRefTraderID) mustBe expectedJson
      expectedJson.validate[TraderId] mustBe JsSuccess(taxRefTraderID)
    }
  }
}
