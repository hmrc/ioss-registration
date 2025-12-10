package uk.gov.hmrc.iossregistration.models.etmp

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.testutils.RegistrationData.etmpEuPreviousRegistrationDetails

class EtmpPreviousEuRegistrationDetailsSpec extends BaseSpec {

  "EtmpPreviousEuRegistrationDetails" - {

    "must deserialise/serialise to and from EtmpPreviousEuRegistrationDetails" - {

      "when all optional values are present" in {

        val json = Json.obj(
          "issuedBy" -> etmpEuPreviousRegistrationDetails.issuedBy,
          "registrationNumber" -> etmpEuPreviousRegistrationDetails.registrationNumber,
          "schemeType" -> etmpEuPreviousRegistrationDetails.schemeType,
          "intermediaryNumber" -> etmpEuPreviousRegistrationDetails.intermediaryNumber
        )

        val expectedResult = EtmpPreviousEuRegistrationDetails(
          issuedBy = etmpEuPreviousRegistrationDetails.issuedBy,
          registrationNumber = etmpEuPreviousRegistrationDetails.registrationNumber,
          schemeType = etmpEuPreviousRegistrationDetails.schemeType,
          intermediaryNumber = etmpEuPreviousRegistrationDetails.intermediaryNumber
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpPreviousEuRegistrationDetails] mustBe JsSuccess(expectedResult)
      }

      "when all optional values are absent" in {

        val json = Json.obj(
          "issuedBy" -> etmpEuPreviousRegistrationDetails.issuedBy,
          "registrationNumber" -> etmpEuPreviousRegistrationDetails.registrationNumber,
          "schemeType" -> etmpEuPreviousRegistrationDetails.schemeType
        )

        val expectedResult = EtmpPreviousEuRegistrationDetails(
          issuedBy = etmpEuPreviousRegistrationDetails.issuedBy,
          registrationNumber = etmpEuPreviousRegistrationDetails.registrationNumber,
          schemeType = etmpEuPreviousRegistrationDetails.schemeType,
          intermediaryNumber = None
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpPreviousEuRegistrationDetails] mustBe JsSuccess(expectedResult)
      }
    }
  }
}
