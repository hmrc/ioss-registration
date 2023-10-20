package uk.gov.hmrc.iossregistration.models.etmp

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

class EtmpEuRegistrationDetailsSpec extends BaseSpec {

  private val countryOfRegistration = etmpEuRegistrationDetails.countryOfRegistration
  private val traderId = etmpEuRegistrationDetails.traderId
  private val tradingName = etmpEuRegistrationDetails.tradingName
  private val fixedEstablishmentAddressLine1 = etmpEuRegistrationDetails.fixedEstablishmentAddressLine1
  private val fixedEstablishmentAddressLine2 = etmpEuRegistrationDetails.fixedEstablishmentAddressLine2
  private val townOrCity = etmpEuRegistrationDetails.townOrCity
  private val regionOrState = etmpEuRegistrationDetails.regionOrState
  private val postcode = etmpEuRegistrationDetails.postcode

  "EtmpEuRegistrationDetails" - {

    "must deserialise/serialise to and from EtmpEuRegistrationDetails" - {

      "when all optional values are present" in {

        val json = Json.obj(
          "countryOfRegistration" -> countryOfRegistration,
          "traderId" -> traderId,
          "tradingName" -> tradingName,
          "fixedEstablishmentAddressLine1" -> fixedEstablishmentAddressLine1,
          "fixedEstablishmentAddressLine2" -> fixedEstablishmentAddressLine2,
          "townOrCity" -> townOrCity,
          "regionOrState" -> regionOrState,
          "postcode" -> postcode
        )

        val expectedResult = EtmpEuRegistrationDetails(
          countryOfRegistration = countryOfRegistration,
          traderId = traderId,
          tradingName = tradingName,
          fixedEstablishmentAddressLine1 = fixedEstablishmentAddressLine1,
          fixedEstablishmentAddressLine2 = fixedEstablishmentAddressLine2,
          townOrCity = townOrCity,
          regionOrState = regionOrState,
          postcode = postcode
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpEuRegistrationDetails] mustBe JsSuccess(expectedResult)
      }

      "when all optional values are absent" in {

        val json = Json.obj(
          "countryOfRegistration" -> countryOfRegistration,
          "traderId" -> traderId,
          "tradingName" -> tradingName,
          "fixedEstablishmentAddressLine1" -> fixedEstablishmentAddressLine1,
          "townOrCity" -> townOrCity
        )

        val expectedResult = EtmpEuRegistrationDetails(
          countryOfRegistration = countryOfRegistration,
          traderId = traderId,
          tradingName = tradingName,
          fixedEstablishmentAddressLine1 = fixedEstablishmentAddressLine1,
          fixedEstablishmentAddressLine2 = None,
          townOrCity = townOrCity,
          regionOrState = None,
          postcode = None
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpEuRegistrationDetails] mustBe JsSuccess(expectedResult)
      }
    }
  }
}
