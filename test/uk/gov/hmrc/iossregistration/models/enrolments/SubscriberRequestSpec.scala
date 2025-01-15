package uk.gov.hmrc.iossregistration.models.enrolments

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

class SubscriberRequestSpec extends BaseSpec {

  "SubscriberRequest" - {

    "serialize to JSON correctly" in {
      val subscriberRequest = SubscriberRequest(
        serviceName = "HMRC-IOSS-ORG",
        callback = "http://example.com/callback",
        etmpId = "12345-etmp-id"
      )

      val expectedJson: JsValue = Json.parse(
        """
          |{
          |  "serviceName": "HMRC-IOSS-ORG",
          |  "callback": "http://example.com/callback",
          |  "etmpId": "12345-etmp-id"
          |}
          |""".stripMargin
      )

      Json.toJson(subscriberRequest) mustBe expectedJson
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "serviceName": "HMRC-IOSS-ORG",
          |  "callback": "http://example.com/callback",
          |  "etmpId": "12345-etmp-id"
          |}
          |""".stripMargin
      )

      val expectedRequest = SubscriberRequest(
        serviceName = "HMRC-IOSS-ORG",
        callback = "http://example.com/callback",
        etmpId = "12345-etmp-id"
      )

      json.as[SubscriberRequest] mustBe expectedRequest
    }

    "fail deserialization for invalid JSON" in {
      val invalidJson: JsValue = Json.parse(
        """
          |{
          |  "serviceName": "HMRC-IOSS-ORG",
          |  "callback": "http://example.com/callback"
          |}
          |""".stripMargin
      )

      an[Exception] should be thrownBy invalidJson.as[SubscriberRequest]
    }

    "create an instance with the correct values" in {
      val subscriberRequest = SubscriberRequest(
        serviceName = "HMRC-IOSS-ORG",
        callback = "http://example.com/callback",
        etmpId = "12345-etmp-id"
      )

      subscriberRequest.serviceName mustBe "HMRC-IOSS-ORG"
      subscriberRequest.callback mustBe "http://example.com/callback"
      subscriberRequest.etmpId mustBe "12345-etmp-id"
    }
  }
}
