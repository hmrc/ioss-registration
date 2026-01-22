package uk.gov.hmrc.iossregistration.models.audit

import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.controllers.actions.AuthorisedMandatoryVrnRequest
import uk.gov.hmrc.iossregistration.models.binders.Format.eisDateTimeFormatter
import uk.gov.hmrc.iossregistration.models.etmp.{EtmpEnrolmentErrorResponse, EtmpEnrolmentResponse, EtmpErrorDetail, EtmpRegistrationRequest}
import uk.gov.hmrc.iossregistration.testutils.RegistrationData.etmpRegistrationRequest

import java.time.LocalDateTime

class EtmpRegistrationRequestAuditModelSpec extends BaseSpec {

  private val request = AuthorisedMandatoryVrnRequest(FakeRequest("GET", "/"), testCredentials, userId, vrn, None, None, enrolments)
  private implicit val dataRequest: AuthorisedMandatoryVrnRequest[AnyContent] = AuthorisedMandatoryVrnRequest(request, testCredentials, userId, vrn, None, None, enrolments)

  private val etmpRegistrationAuditType: EtmpRegistrationAuditType = EtmpRegistrationAuditType.CreateRegistration
  private val submissionResultSuccess: SubmissionResult = SubmissionResult.Success
  private val submissionResultFailure: SubmissionResult = SubmissionResult.Failure
  private val submissionResultDuplicate: SubmissionResult = SubmissionResult.Duplicate

  private val arbitraryEtmpRegistrationRequest: EtmpRegistrationRequest = etmpRegistrationRequest

  private val etmpEnrolmentResponse: EtmpEnrolmentResponse =
    EtmpEnrolmentResponse(
      processingDateTime = LocalDateTime.now(stubClock),
      formBundleNumber = "123456789",
      vrn = vrn.vrn,
      iossReference = "test",
      businessPartner = "test businessPartner"
    )

  private val etmpEnrolmentErrorResponse: EtmpEnrolmentErrorResponse =
    EtmpEnrolmentErrorResponse(
      errorDetail = EtmpErrorDetail(
        timestamp = LocalDateTime.now(stubClock).format(eisDateTimeFormatter),
        errorCode = Some("123"),
        errorMessage = Some("error")
      )
    )

  "RegistrationAuditModelSpec" - {

    "must create correct json object for Submission Result Success" in {

      val etmpRegistrationAuditModel = EtmpRegistrationRequestAuditModel.build(
        etmpRegistrationAuditType = etmpRegistrationAuditType,
        etmpRegistrationRequest = arbitraryEtmpRegistrationRequest,
        etmpEnrolmentResponse = Some(etmpEnrolmentResponse),
        etmpAmendResponse = None,
        errorResponse = None,
        submissionResult = submissionResultSuccess
      )

      val expectedJson = Json.obj(
        "userId" -> request.userId,
        "browserUserAgent" -> "",
        "requestersVrn" -> request.vrn.vrn,
        "etmpRegistrationRequest" -> Json.toJson(arbitraryEtmpRegistrationRequest),
        "submissionResult" -> Json.toJson(submissionResultSuccess),
        "etmpEnrolmentResponse" -> Json.toJson(etmpEnrolmentResponse)
      )

      etmpRegistrationAuditModel.detail mustBe expectedJson
    }

    "must create correct json object for Submission Result Failure" in {

      val etmpRegistrationAuditModel = EtmpRegistrationRequestAuditModel.build(
        etmpRegistrationAuditType = etmpRegistrationAuditType,
        etmpRegistrationRequest = arbitraryEtmpRegistrationRequest,
        etmpEnrolmentResponse = None,
        etmpAmendResponse = None,
        errorResponse = Some(Json.toJson(etmpEnrolmentErrorResponse).toString()),
        submissionResult = submissionResultFailure
      )

      val expectedJson = Json.obj(
        "userId" -> request.userId,
        "browserUserAgent" -> "",
        "requestersVrn" -> request.vrn.vrn,
        "etmpRegistrationRequest" -> Json.toJson(arbitraryEtmpRegistrationRequest),
        "submissionResult" -> Json.toJson(submissionResultFailure),
        "errorResponse" -> Json.toJson(etmpEnrolmentErrorResponse).toString()
      )

      etmpRegistrationAuditModel.detail mustBe expectedJson
    }

    "must create correct json object for Submission Result Duplicate" in {

      val etmpRegistrationAuditModel = EtmpRegistrationRequestAuditModel.build(
        etmpRegistrationAuditType = etmpRegistrationAuditType,
        etmpRegistrationRequest = arbitraryEtmpRegistrationRequest,
        etmpEnrolmentResponse = None,
        etmpAmendResponse = None,
        errorResponse = None,
        submissionResult = submissionResultDuplicate
      )

      val expectedJson = Json.obj(
        "userId" -> request.userId,
        "browserUserAgent" -> "",
        "requestersVrn" -> request.vrn.vrn,
        "etmpRegistrationRequest" -> Json.toJson(arbitraryEtmpRegistrationRequest),
        "submissionResult" -> Json.toJson(submissionResultDuplicate),
      )

      etmpRegistrationAuditModel.detail mustBe expectedJson
    }
  }
}

