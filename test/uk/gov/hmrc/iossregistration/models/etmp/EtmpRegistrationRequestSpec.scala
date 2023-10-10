package uk.gov.hmrc.iossregistration.models.etmp

import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

class EtmpRegistrationRequestSpec extends BaseSpec {

  private val administration = arbitrary[EtmpAdministration].sample.value
  private val customerIdentification = arbitrary[EtmpCustomerIdentification].sample.value
  private val tradingNames = Seq(arbitrary[EtmpTradingName].sample.value)
  private val schemeDetails = etmpSchemeDetails
  private val bankDetails = genBankDetails


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
