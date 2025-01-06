package uk.gov.hmrc.iossregistration.models.core


import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsValue, Json}

import java.time.Instant

class EisErrorResponseSpec extends AnyFreeSpec with Matchers {

  "EisErrorResponse" - {

    "serialize to JSON correctly" in {

      val errorResponse = EisErrorResponse(
        timestamp = Instant.parse("2023-01-01T12:00:00Z"),
        error = "SomeError",
        errorMessage = "An error occurred"
      )

      val expectedJson: JsValue = Json.parse(
        """
          |{
          |  "timestamp": "2023-01-01T12:00:00Z",
          |  "error": "SomeError",
          |  "errorMessage": "An error occurred"
          |}
          |""".stripMargin
      )

      Json.toJson(errorResponse) mustBe expectedJson
    }

    "deserialize from JSON correctly" in {

      val json: JsValue = Json.parse(
        """
          |{
          |  "timestamp": "2023-01-01T12:00:00Z",
          |  "error": "SomeError",
          |  "errorMessage": "An error occurred"
          |}
          |""".stripMargin
      )

      val expectedResponse = EisErrorResponse(
        timestamp = Instant.parse("2023-01-01T12:00:00Z"),
        error = "SomeError",
        errorMessage = "An error occurred"
      )

      json.as[EisErrorResponse] mustBe expectedResponse
    }
  }

  "EisDisplayErrorResponse" - {

    "serialize to JSON correctly" in {
      val errorDetail = EisDisplayErrorDetail(
        correlationId = "12345",
        errorCode = "089",
        errorMessage = "No registration found",
        timestamp = "2023-01-01T12:00:00Z"
      )

      val displayErrorResponse = EisDisplayErrorResponse(errorDetail)

      val expectedJson: JsValue = Json.parse(
        """
          |{
          |  "errorDetail": {
          |    "correlationId": "12345",
          |    "errorCode": "089",
          |    "errorMessage": "No registration found",
          |    "timestamp": "2023-01-01T12:00:00Z"
          |  }
          |}
          |""".stripMargin
      )

      Json.toJson(displayErrorResponse) mustBe expectedJson
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "errorDetail": {
          |    "correlationId": "12345",
          |    "errorCode": "089",
          |    "errorMessage": "No registration found",
          |    "timestamp": "2023-01-01T12:00:00Z"
          |  }
          |}
          |""".stripMargin
      )

      val expectedErrorDetail = EisDisplayErrorDetail(
        correlationId = "12345",
        errorCode = "089",
        errorMessage = "No registration found",
        timestamp = "2023-01-01T12:00:00Z"
      )

      val expectedResponse = EisDisplayErrorResponse(expectedErrorDetail)

      json.as[EisDisplayErrorResponse] mustBe expectedResponse
    }
  }

  "EisDisplayErrorDetail" - {

    "serialize to JSON correctly" in {
      val errorDetail = EisDisplayErrorDetail(
        correlationId = "12345",
        errorCode = "089",
        errorMessage = "No registration found",
        timestamp = "2023-01-01T12:00:00Z"
      )

      val expectedJson: JsValue = Json.parse(
        """
          |{
          |  "correlationId": "12345",
          |  "errorCode": "089",
          |  "errorMessage": "No registration found",
          |  "timestamp": "2023-01-01T12:00:00Z"
          |}
          |""".stripMargin
      )

      Json.toJson(errorDetail) mustBe expectedJson
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "correlationId": "12345",
          |  "errorCode": "089",
          |  "errorMessage": "No registration found",
          |  "timestamp": "2023-01-01T12:00:00Z"
          |}
          |""".stripMargin
      )

      val expectedErrorDetail = EisDisplayErrorDetail(
        correlationId = "12345",
        errorCode = "089",
        errorMessage = "No registration found",
        timestamp = "2023-01-01T12:00:00Z"
      )

      json.as[EisDisplayErrorDetail] mustBe expectedErrorDetail
    }

    "have the correct display error code constant" in {
      EisDisplayErrorDetail.displayErrorCodeNoRegistration mustBe "089"
    }
  }

  "EisDisplayErrorResponse companion object" - {

    "have the correct display error code constant" in {
      EisDisplayErrorResponse.displayErrorCodeNoRegistration mustBe "089"
    }
  }
}

