package uk.gov.hmrc.iossregistration.models.etmp

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

class EtmpPreviousEuRegistrationDetailsSpec extends BaseSpec {

  private val issuingCountry = etmpEuPreviousRegistrationDetails.issuedBy
  private val registrationNumber = etmpEuPreviousRegistrationDetails.registrationNumber
  private val schemeType = etmpEuPreviousRegistrationDetails.schemeType
  private val intermediaryNumber = etmpEuPreviousRegistrationDetails.intermediaryNumber


  "EtmpPreviousEuRegistrationDetails" - {

    "must deserialise/serialise to and from EtmpPreviousEuRegistrationDetails" - {

      "when all optional values are present" in {

        val json = Json.obj(
          "issuedBy" -> issuingCountry,
          "registrationNumber" -> registrationNumber,
          "schemeType" -> schemeType,
          "intermediaryNumber" ->intermediaryNumber
        )

        val expectedResult = EtmpPreviousEuRegistrationDetails(
          issuedBy = issuingCountry,
          registrationNumber = registrationNumber,
          schemeType = schemeType,
          intermediaryNumber = intermediaryNumber
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpPreviousEuRegistrationDetails] mustBe JsSuccess(expectedResult)
      }

      "when all optional values are absent" in {

        val json = Json.obj(
          "issuedBy" -> issuingCountry,
          "registrationNumber" -> registrationNumber,
          "schemeType" -> schemeType
        )

        val expectedResult = EtmpPreviousEuRegistrationDetails(
          issuedBy = issuingCountry,
          registrationNumber = registrationNumber,
          schemeType = schemeType,
          intermediaryNumber = None
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpPreviousEuRegistrationDetails] mustBe JsSuccess(expectedResult)
      }
    }
  }
}
