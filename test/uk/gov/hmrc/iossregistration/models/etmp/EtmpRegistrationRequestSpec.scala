package uk.gov.hmrc.iossregistration.models.etmp

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

class EtmpRegistrationRequestSpec extends BaseSpec {

  private val administration = etmpRegistrationRequest.administration
  private val customerIdentification = etmpRegistrationRequest.customerIdentification
  private val tradingNames = etmpRegistrationRequest.tradingNames
  private val schemeDetails = etmpRegistrationRequest.schemeDetails
  private val bankDetails = etmpRegistrationRequest.bankDetails


  "EtmpRegistrationRequest" - {

    "must deserialise/serialise to and from EtmpRegistrationRequest" in {

      val json = Json.obj(
        "administration" -> administration,
        "customerIdentification" -> customerIdentification,
        "tradingNames" -> tradingNames,
        "schemeDetails" -> schemeDetails,
        "bankDetails" -> bankDetails
      )

      val expectedResult = EtmpRegistrationRequest(
        administration = administration,
        customerIdentification = customerIdentification,
        tradingNames = tradingNames,
        schemeDetails = schemeDetails,
        bankDetails = bankDetails
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpRegistrationRequest] mustBe JsSuccess(expectedResult)
    }
  }
}
