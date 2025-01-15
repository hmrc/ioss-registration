package uk.gov.hmrc.iossregistration.models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.iossregistration.models.core.{EisDisplayErrorDetail, EisDisplayErrorResponse}


class ErrorResponseSpec extends AnyFreeSpec with Matchers {

  "TaxEnrolmentErrorResponse" - {

    "serialize to JSON correctly" in {
      val taxEnrolmentErrorResponse = TaxEnrolmentErrorResponse(
        code = "ERROR_CODE",
        message = "An error occurred"
      )

      val json = Json.toJson(taxEnrolmentErrorResponse)
      val expectedJson = Json.parse(
        """
          |{
          |  "code": "ERROR_CODE",
          |  "message": "An error occurred"
          |}
          |""".stripMargin
      )

      json mustBe expectedJson
    }

    "deserialize from JSON correctly" in {
      val json = Json.parse(
        """
          |{
          |  "code": "ERROR_CODE",
          |  "message": "An error occurred"
          |}
          |""".stripMargin
      )

      val result = json.as[TaxEnrolmentErrorResponse]
      result mustBe TaxEnrolmentErrorResponse(
        code = "ERROR_CODE",
        message = "An error occurred"
      )
    }

    "fail to deserialize when required fields are missing" in {
      val json = Json.parse(
        """
          |{
          |  "code": "ERROR_CODE"
          |}
          |""".stripMargin
      )

      an[Exception] should be thrownBy json.as[TaxEnrolmentErrorResponse]
    }

    "fail to deserialize when fields are of the wrong type" in {
      val json = Json.parse(
        """
          |{
          |  "code": 12345,
          |  "message": true
          |}
          |""".stripMargin
      )

      an[Exception] should be thrownBy json.as[TaxEnrolmentErrorResponse]
    }
  }

  "EisDisplayErrorResponse" - {

    "generate the correct body string from an EisDisplayErrorResponse" in {
      val eisDisplayErrorDetail = EisDisplayErrorDetail(
        correlationId = "test-correlation-id",
        errorCode = "089",
        errorMessage = "No registration found",
        timestamp = "2025-01-01T12:00:00Z"
      )

      val eisDisplayErrorResponse = EisDisplayErrorResponse(
        errorDetail = eisDisplayErrorDetail
      )

      val eisDisplayRegistrationError = EisDisplayRegistrationError(eisDisplayErrorResponse)

      eisDisplayRegistrationError.body mustBe
        "2025-01-01T12:00:00Z 089 No registration found "
    }

    "correctly serialize and deserialize EisDisplayErrorDetail to and from JSON" in {
      val errorDetail = EisDisplayErrorDetail(
        correlationId = "test-correlation-id",
        errorCode = "089",
        errorMessage = "No registration found",
        timestamp = "2025-01-01T12:00:00Z"
      )

      val json = Json.toJson(errorDetail)
      val expectedJson = Json.parse(
        """
          |{
          |  "correlationId": "test-correlation-id",
          |  "errorCode": "089",
          |  "errorMessage": "No registration found",
          |  "timestamp": "2025-01-01T12:00:00Z"
          |}
          |""".stripMargin
      )

      json mustBe expectedJson

      val deserialized = json.as[EisDisplayErrorDetail]
      deserialized mustBe errorDetail
    }

    "correctly serialize and deserialize EisDisplayErrorResponse to and from JSON" in {
      val errorDetail = EisDisplayErrorDetail(
        correlationId = "test-correlation-id",
        errorCode = "089",
        errorMessage = "No registration found",
        timestamp = "2025-01-01T12:00:00Z"
      )

      val errorResponse = EisDisplayErrorResponse(errorDetail)

      val json = Json.toJson(errorResponse)
      val expectedJson = Json.parse(
        """
          |{
          |  "errorDetail": {
          |    "correlationId": "test-correlation-id",
          |    "errorCode": "089",
          |    "errorMessage": "No registration found",
          |    "timestamp": "2025-01-01T12:00:00Z"
          |  }
          |}
          |""".stripMargin
      )

      json mustBe expectedJson

      val deserialized = json.as[EisDisplayErrorResponse]
      deserialized mustBe errorResponse
    }

    "handle invalid EisDisplayErrorResponse JSON gracefully" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "errorDetail": {
          |    "correlationId": "test-correlation-id",
          |    "errorMessage": "No registration found"
          |  }
          |}
          |""".stripMargin
      )

      an[Exception] should be thrownBy invalidJson.as[EisDisplayErrorResponse]
    }
  }
}

