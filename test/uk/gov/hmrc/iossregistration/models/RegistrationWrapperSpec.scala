package uk.gov.hmrc.iossregistration.models

import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{Json, JsValue}
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.des.VatCustomerInfo
import uk.gov.hmrc.iossregistration.models.etmp.*
import uk.gov.hmrc.iossregistration.testutils.DisplayRegistrationData.{stubClock, *}

import java.time.{LocalDate, LocalDateTime}

class RegistrationWrapperSpec extends BaseSpec with Matchers {

  private val vatCustomerInfo = VatCustomerInfo(
    desAddress = DesAddress("Line 1", None, None, None, None, Some("AA11 1AA"), "GB"),
    registrationDate = Some(LocalDate.of(2022, 1, 1)),
    partOfVatGroup = true,
    organisationName = Some("Company Name"),
    individualName = None,
    singleMarketIndicator = false,
    deregistrationDecisionDate = None,
    overseasIndicator = false
  )

  private val etmpDisplayRegistration = EtmpDisplayRegistration(
    customerIdentification = arbitraryEtmpCustomerIdentificationNew.arbitrary.sample.value,
    tradingNames = Seq(EtmpTradingName("Test Trading Name")),
    schemeDetails = EtmpDisplaySchemeDetails(
      commencementDate = "2022-01-01",
      euRegistrationDetails = Seq.empty,
      previousEURegistrationDetails = Seq.empty,
      websites = Seq.empty,
      contactName = "John Doe",
      businessTelephoneNumber = "123456789",
      businessEmailId = "test@example.com",
      unusableStatus = false,
      nonCompliantReturns = None,
      nonCompliantPayments = None
    ),
    bankDetails = Some(EtmpBankDetails(
      accountName = "Mr Test",
      bic = Some(Bic("ABCDEF2A").get),
      iban = Iban("GB33BUKB20201555555555").toOption.get
    )),
    exclusions = Seq.empty,
    adminUse = EtmpAdminUse(Some(LocalDateTime.now(stubClock)))
  )

  "RegistrationWrapper" - {

    "serialize correctly" in {

      val registrationWrapper = RegistrationWrapper(Some(vatCustomerInfo), etmpDisplayRegistration)

      val json: JsValue = Json.toJson(registrationWrapper)

      val expectedJson = Json.parse(
        s"""
           |{
           |  "vatInfo": {
           |    "desAddress": {
           |      "line1": "Line 1",
           |      "countryCode": "GB",
           |      "postCode": "AA11 1AA"
           |    },
           |    "registrationDate": "2022-01-01",
           |    "partOfVatGroup": true,
           |    "organisationName": "Company Name",
           |    "singleMarketIndicator": false,
           |    "overseasIndicator": false
           |  },
           |  "registration": {
           |  "customerIdentification": {
           |  "idType":"VRN",
           |  "idValue":"${etmpDisplayRegistration.customerIdentification.idValue}"
           |  },
           |    "tradingNames": [
           |      {
           |        "tradingName": "Test Trading Name"
           |      }
           |    ],
           |    "schemeDetails": {
           |      "commencementDate": "2022-01-01",
           |      "contactName": "John Doe",
           |      "businessTelephoneNumber": "123456789",
           |      "businessEmailId": "test@example.com",
           |      "unusableStatus": false,
           |      "previousEURegistrationDetails": [],
           |      "euRegistrationDetails": [],
           |      "websites": []
           |    },
           |    "bankDetails": {
           |      "accountName": "Mr Test",
           |      "iban": "GB33BUKB20201555555555",
           |      "bic": "ABCDEF2A"
           |    },
           |    "adminUse": {
           |      "changeDate": "${LocalDate.now(stubClock)}T00:00:00"
           |    },
           |    "exclusions": []
           |  }
           |}
           |""".stripMargin
      )

      json mustBe expectedJson
    }
  }

  "deserialize correctly" in {

    val expectedVatCustomerInfo = VatCustomerInfo(
      desAddress = DesAddress("Line 1", None, None, None, None, Some("AA11 1AA"), "CC"),
      registrationDate = Some(LocalDate.of(2022, 1, 2)),
      partOfVatGroup = false,
      organisationName = Some("Company Name"),
      singleMarketIndicator = false,
      individualName = Some("A B C"),
      deregistrationDecisionDate = None,
      overseasIndicator = false
    )

    val expectedRegistrationWrapper = RegistrationWrapper(Some(expectedVatCustomerInfo), etmpDisplayRegistration)

    val json = Json.parse(
      s"""
         |{
         |  "vatInfo": {
         |    "approvedInformation": {
         |      "customerDetails": {
         |        "overseasIndicator": false,
         |        "singleMarketIndicator": false,
         |        "organisationName": "Company Name",
         |        "partyType": "ZZ",
         |        "effectiveRegistrationDate": "2022-01-02",
         |        "individual": {
         |          "firstName": "A",
         |          "middleName": "B",
         |          "lastName": "C"
         |        }
         |      },
         |      "PPOB": {
         |        "address": {
         |          "line1": "Line 1",
         |          "countryCode": "CC",
         |          "postCode": "AA11 1AA"
         |        }
         |      }
         |    }
         |  },
         |  "registration": {
         |    "customerIdentification": {
         |      "idType":"VRN",
         |      "idValue":"${etmpDisplayRegistration.customerIdentification.idValue}"
         |     },
         |    "tradingNames": [
         |      {
         |        "tradingName": "Test Trading Name"
         |      }
         |    ],
         |    "schemeDetails": {
         |      "commencementDate": "2022-01-01",
         |      "contactDetails": {
         |        "contactNameOrBusinessAddress": "John Doe",
         |        "businessTelephoneNumber": "123456789",
         |        "businessEmailAddress": "test@example.com",
         |        "unusableStatus": false
         |      },
         |      "nonCompliantReturns": null,
         |      "nonCompliantPayments": null,
         |      "websites": []
         |    },
         |    "bankDetails": {
         |      "accountName": "Mr Test",
         |      "iban": "GB33BUKB20201555555555",
         |      "bic": "ABCDEF2A"
         |    },
         |    "adminUse": {
         |      "changeDate": "${LocalDate.now(stubClock)}T00:00:00"
         |    },
         |    "exclusions": []
         |  }
         |}
         |""".stripMargin
    )

    val deserialized = json.as[RegistrationWrapper]
    deserialized mustBe expectedRegistrationWrapper
  }
}
