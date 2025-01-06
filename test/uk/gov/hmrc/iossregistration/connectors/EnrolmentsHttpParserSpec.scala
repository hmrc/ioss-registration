package uk.gov.hmrc.iossregistration.connectors

import play.api.http.Status.*
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.TaxEnrolmentErrorResponse


class EnrolmentsHttpParserSpec extends BaseSpec with WireMockHelper {

  "EnrolmentsHttpParserSpec" - {

    "EnrolmentsResponseReads should return Right(()) for CREATED status" in {
      val response = HttpResponse(CREATED, "")
      val result = EnrolmentsHttpParser.EnrolmentsResponseReads.read("PUT", "/enrolments", response)
      result mustBe Right(())
    }
  }

  "return TaxEnrolmentErrorResponse with empty body for non-CREATED status and no body" in {
    val response = HttpResponse(BAD_REQUEST, "")
    val result = EnrolmentsHttpParser.EnrolmentsResponseReads.read("PUT", "/enrolments", response)
    result mustBe Left(TaxEnrolmentErrorResponse("UNEXPECTED_400", "The response body was empty"))
  }

  "return TaxEnrolmentErrorResponse with parsed JSON for non-CREATED status and valid error JSON body" in {
    val errorResponse =
      """
        |{
        |  "code": "ERROR_CODE",
        |  "reason": "Some error occurred"
        |}
    """.stripMargin
    val response = HttpResponse(BAD_REQUEST, errorResponse, Map.empty)

    val result = EnrolmentsHttpParser.EnrolmentsResponseReads.read("PUT", "/enrolments", response)

    result mustBe Left(TaxEnrolmentErrorResponse("UNEXPECTED_400", errorResponse))
  }

  "return TaxEnrolmentErrorResponse with raw body if JSON validation fails" in {
    val invalidJson = """{ "invalid": "json" }"""
    val response = HttpResponse(BAD_REQUEST, invalidJson)
    val result = EnrolmentsHttpParser.EnrolmentsResponseReads.read("PUT", "/enrolments", response)
    result mustBe Left(TaxEnrolmentErrorResponse("UNEXPECTED_400", invalidJson))
  }
}
