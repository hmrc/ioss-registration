package uk.gov.hmrc.iossregistration.models.etmp

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

class EtmpSchemeDetailsSpec extends BaseSpec {

  private val commencementDate = etmpSchemeDetails.commencementDate
  private val euRegistrationDetails = etmpSchemeDetails.euRegistrationDetails
  private val previousEURegistrationDetails = etmpSchemeDetails.previousEURegistrationDetails
  private val websites = etmpSchemeDetails.websites
  private val contactName = etmpSchemeDetails.contactName
  private val businessTelephoneNumber = etmpSchemeDetails.businessTelephoneNumber
  private val businessEmailId = etmpSchemeDetails.businessEmailId
  private val nonCompliantReturns = etmpSchemeDetails.nonCompliantReturns
  private val nonCompliantPayments = etmpSchemeDetails.nonCompliantPayments

  "EtmpSchemeDetails" - {

    "must deserialise/serialise to and from EtmpSchemeDetails" - {

      "when all optional values are present" in {

        val json = Json.obj(
          "commencementDate" -> commencementDate,
          "euRegistrationDetails" -> euRegistrationDetails,
          "previousEURegistrationDetails" -> previousEURegistrationDetails,
          "websites" -> websites,
          "contactName" -> contactName,
          "businessTelephoneNumber" -> businessTelephoneNumber,
          "businessEmailId" -> businessEmailId,
          "nonCompliantReturns" -> nonCompliantReturns,
          "nonCompliantPayments" -> nonCompliantPayments
        )

        val expectedResult = EtmpSchemeDetails(
          commencementDate = commencementDate,
          euRegistrationDetails = euRegistrationDetails,
          previousEURegistrationDetails = previousEURegistrationDetails,
          websites = websites,
          contactName = contactName,
          businessTelephoneNumber = businessTelephoneNumber,
          businessEmailId = businessEmailId,
          nonCompliantReturns = nonCompliantReturns,
          nonCompliantPayments = nonCompliantPayments
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpSchemeDetails] mustBe JsSuccess(expectedResult)
      }

      "when all optional values are absent" in {

        val json = Json.obj(
          "commencementDate" -> commencementDate,
          "euRegistrationDetails" -> euRegistrationDetails,
          "previousEURegistrationDetails" -> previousEURegistrationDetails,
          "websites" -> websites,
          "contactName" -> contactName,
          "businessTelephoneNumber" -> businessTelephoneNumber,
          "businessEmailId" -> businessEmailId,
        )

        val expectedResult = EtmpSchemeDetails(
          commencementDate = commencementDate,
          euRegistrationDetails = euRegistrationDetails,
          previousEURegistrationDetails = previousEURegistrationDetails,
          websites = websites,
          contactName = contactName,
          businessTelephoneNumber = businessTelephoneNumber,
          businessEmailId = businessEmailId,
          nonCompliantReturns = None,
          nonCompliantPayments = None
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpSchemeDetails] mustBe JsSuccess(expectedResult)
      }
    }
  }
}
