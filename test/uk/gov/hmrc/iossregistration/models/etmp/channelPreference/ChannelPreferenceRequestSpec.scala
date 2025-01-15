package uk.gov.hmrc.iossregistration.models.etmp.channelPreference

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

class ChannelPreferenceRequestSpec extends BaseSpec {

  "ChannelPreferenceRequest" - {

    "serialize to JSON correctly" in {

      val request = ChannelPreferenceRequest(
        identifierType = "NINO",
        identifier = "AB123456C",
        emailAddress = "test@example.com",
        unusableStatus = false
      )

      val expectedJson: JsValue = Json.parse(
        """
          |{
          |  "identifierType": "NINO",
          |  "identifier": "AB123456C",
          |  "emailAddress": "test@example.com",
          |  "unusableStatus": false
          |}
          |""".stripMargin
      )

      val actualJson = Json.toJson(request)
      actualJson mustBe expectedJson
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "identifierType": "NINO",
          |  "identifier": "AB123456C",
          |  "emailAddress": "test@example.com",
          |  "unusableStatus": false
          |}
          |""".stripMargin
      )

      val expectedRequest = ChannelPreferenceRequest(
        identifierType = "NINO",
        identifier = "AB123456C",
        emailAddress = "test@example.com",
        unusableStatus = false
      )

      val actualRequest = json.as[ChannelPreferenceRequest]
      actualRequest mustBe expectedRequest
    }

    "fail deserialization for invalid JSON" in {
      val invalidJson: JsValue = Json.parse(
        """
          |{
          |  "identifierType": "NINO",
          |  "identifier": "AB123456C",
          |  "emailAddress": "test@example.com"
          |}
          |""".stripMargin // Missing "unusableStatus"
      )

      an[Exception] should be thrownBy invalidJson.as[ChannelPreferenceRequest]
    }

    "create an instance with the correct values" in {
      val request = ChannelPreferenceRequest(
        identifierType = "NINO",
        identifier = "AB123456C",
        emailAddress = "test@example.com",
        unusableStatus = true
      )

      request.identifierType mustBe "NINO"
      request.identifier mustBe "AB123456C"
      request.emailAddress mustBe "test@example.com"
      request.unusableStatus mustBe true
    }


  }
}
