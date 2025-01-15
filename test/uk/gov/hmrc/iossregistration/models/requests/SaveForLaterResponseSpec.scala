package uk.gov.hmrc.iossregistration.models.requests

import play.api.libs.json.*
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.{DesAddress, SavedUserAnswers}
import uk.gov.hmrc.iossregistration.models.des.VatCustomerInfo

import java.time.{Instant, LocalDate}

class SaveForLaterResponseSpec extends BaseSpec  {

  "SaveForLaterResponse apply method" - {
    "should correctly create SaveForLaterResponse from SavedUserAnswers and VatCustomerInfo" in {
      val savedUserAnswers = SavedUserAnswers(vrn, Json.obj("key" -> "value"), Instant.now())

      val result = SaveForLaterResponse(savedUserAnswers, vatCustomerInfo)

      result.vrn mustEqual vrn
      result.data mustEqual savedUserAnswers.data
      result.vatInfo mustEqual vatCustomerInfo
      result.lastUpdated mustEqual savedUserAnswers.lastUpdated
    }
  }

  "SaveForLaterResponse format" - {
    "serialize SaveForLaterResponse correctly" in {
      val vatCustomerInfo = VatCustomerInfo(
        desAddress = DesAddress("Line 1", None, None, None, None, Some("AA11 1AA"), "GB"),
        registrationDate = Some(LocalDate.of(2022, 1, 1)),
        partOfVatGroup = false,
        organisationName = Some("Company Name"),
        individualName = None,
        singleMarketIndicator = true,
        deregistrationDecisionDate = None,
        overseasIndicator = false
      )

      val saveForLaterResponse = SaveForLaterResponse(
        vrn = Vrn("123456789"),
        data = Json.obj("key" -> "value"),
        vatInfo = vatCustomerInfo,
        lastUpdated = Instant.parse("2025-01-06T00:00:00Z")
      )

      val actualJson = Json.toJson(saveForLaterResponse)

      val expectedJson = Json.parse(
        s"""
           |{
           |  "vrn": "123456789",
           |  "data": {
           |    "key": "value"
           |  },
           |  "vatInfo": {
           |    "desAddress": {
           |      "line1": "Line 1",
           |      "countryCode": "GB",
           |      "postCode": "AA11 1AA"
           |    },
           |    "overseasIndicator": false,
           |    "singleMarketIndicator": true,
           |    "partOfVatGroup": false,
           |    "registrationDate": "2022-01-01",
           |    "organisationName": "Company Name"
           |  },
           |  "lastUpdated": "2025-01-06T00:00:00Z"
           |}
           |""".stripMargin
      )

      actualJson mustBe expectedJson
    }

    "deserialize SaveForLaterResponse correctly" in {
      val json = Json.parse(
        s"""
           |{
           |  "vrn": "123456789",
           |  "data": {
           |    "key": "value"
           |  },
           |  "vatInfo": {
           |    "approvedInformation": {
           |      "PPOB": {
           |        "address": {
           |          "line1": "Line 1",
           |          "countryCode": "GB",
           |          "postCode": "AA11 1AA"
           |        }
           |      },
           |      "customerDetails": {
           |        "singleMarketIndicator": true,
           |        "overseasIndicator": false,
           |        "effectiveRegistrationDate": "2022-01-01",
           |        "organisationName": "Company Name"
           |      }
           |    }
           |  },
           |  "lastUpdated": "2025-01-06T00:00:00Z"
           |}
           |""".stripMargin
      )

      val expectedVatCustomerInfo = VatCustomerInfo(
        desAddress = DesAddress("Line 1", None, None, None, None, Some("AA11 1AA"), "GB"),
        registrationDate = Some(LocalDate.of(2022, 1, 1)),
        partOfVatGroup = false,
        organisationName = Some("Company Name"),
        individualName = None,
        singleMarketIndicator = true,
        deregistrationDecisionDate = None,
        overseasIndicator = false
      )

      val expectedSaveForLaterResponse = SaveForLaterResponse(
        vrn = Vrn("123456789"),
        data = Json.obj("key" -> "value"),
        vatInfo = expectedVatCustomerInfo,
        lastUpdated = Instant.parse("2025-01-06T00:00:00Z")
      )

      val actualSaveForLaterResponse = json.as[SaveForLaterResponse]

      actualSaveForLaterResponse mustBe expectedSaveForLaterResponse
    }
  }
}
