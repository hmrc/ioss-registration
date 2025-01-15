package uk.gov.hmrc.iossregistration.connectors

import play.api.libs.json.{JsError, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.etmp.amend.AmendRegistrationResponse

import java.time.LocalDateTime


class RegistrationHttpParserSpec extends BaseSpec with WireMockHelper {

  "CreateAmendRegistrationResponseReads" - {
    "parse a successful response with all fields present" in {
      val validJson = Json.parse(
        s"""
           |{
           |  "processingDateTime": "${LocalDateTime.now(stubClock)}",
           |  "formBundleNumber": "123456789",
           |  "vrn": "123456789",
           |  "iossReference": "test",
           |  "businessPartner": "test businessPartner"
           |}
         """.stripMargin
      )

      val result = validJson.as[AmendRegistrationResponse]
      result mustBe AmendRegistrationResponse(
        processingDateTime = LocalDateTime.now(stubClock),
        formBundleNumber = "123456789",
        vrn = vrn.vrn,
        iossReference = "test",
        businessPartner = "test businessPartner"
      )
    }

    "return a JsError when the JSON structure is invalid" in {
      val invalidJson = Json.parse(
        s"""
           |{
           |  "processingDate": "2023-01-01T12:00:00",
           |  "formBundle": "12345"
           |}
           """.stripMargin
      )

      val result = invalidJson.validate[AmendRegistrationResponse]
      result mustBe a[JsError]
    }
  }
}
