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
      expectedJson.validate[VatNumberTraderId] mustBe JsSuccess(vatNumberTraderId)
    }

    "must deserialise/serialise to and from TaxRefTraderID" in {

      val taxRefTraderID = arbitrary[TaxRefTraderID].sample.value

      val expectedJson = Json.obj(
        "taxReferenceNumber" -> s"${taxRefTraderID.taxReferenceNumber}"
      )

      Json.toJson(taxRefTraderID) mustBe expectedJson
      expectedJson.validate[TraderId] mustBe JsSuccess(taxRefTraderID)
      expectedJson.validate[TaxRefTraderID] mustBe JsSuccess(taxRefTraderID)
    }

    "must correctly use Json.format for VatNumberTraderId" in {
      val vatNumberTraderId = VatNumberTraderId("GB123456789")
      val json = Json.toJson(vatNumberTraderId)
      
      json mustBe Json.obj("vatNumber" -> "GB123456789")
      
      json.as[VatNumberTraderId] mustBe vatNumberTraderId
    }
    
    "must correctly use Json.format for TaxRefTraderID" in {
      val taxRefTraderID = TaxRefTraderID("TR123456")
      val json = Json.toJson(taxRefTraderID)
      
      json mustBe Json.obj("taxReferenceNumber" -> "TR123456")
      
      json.as[TaxRefTraderID] mustBe taxRefTraderID
    }
    
    "must serialize TaxRefTraderID using its format" in {
      val taxRefTraderID = TaxRefTraderID("TR123456")
      val json = Json.toJson(taxRefTraderID)(TaxRefTraderID.format)
      
      json mustBe Json.obj("taxReferenceNumber" -> "TR123456")
    }
  }
}
