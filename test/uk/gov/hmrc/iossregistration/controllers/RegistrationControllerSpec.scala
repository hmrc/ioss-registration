package uk.gov.hmrc.iossregistration.controllers

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import play.api.http.Status.CREATED
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.connectors.EnrolmentsConnector
import uk.gov.hmrc.iossregistration.controllers.actions.AuthorisedMandatoryVrnRequest
import uk.gov.hmrc.iossregistration.models.audit.{EtmpRegistrationAuditType, EtmpRegistrationRequestAuditModel, SubmissionResult}
import uk.gov.hmrc.iossregistration.models.etmp.amend.AmendRegistrationResponse
import uk.gov.hmrc.iossregistration.models.etmp.{EtmpEnrolmentResponse, EtmpRegistrationStatus}
import uk.gov.hmrc.iossregistration.models.{EtmpEnrolmentError, EtmpException, RegistrationStatus, ServiceUnavailable}
import uk.gov.hmrc.iossregistration.repositories.InsertResult.InsertSucceeded
import uk.gov.hmrc.iossregistration.repositories.RegistrationStatusRepository
import uk.gov.hmrc.iossregistration.services.{AuditService, RegistrationService, RetryService}
import uk.gov.hmrc.iossregistration.testutils.RegistrationData
import uk.gov.hmrc.iossregistration.testutils.RegistrationData.etmpRegistrationRequest
import uk.gov.hmrc.iossregistration.utils.FutureSyntax.FutureOps

import java.time.LocalDateTime
import scala.concurrent.Future

class RegistrationControllerSpec extends BaseSpec with BeforeAndAfterEach {

  private val mockRegistrationService: RegistrationService = mock[RegistrationService]
  private val mockEnrolmentsConnector: EnrolmentsConnector = mock[EnrolmentsConnector]
  private val mockRegistrationStatusRepository: RegistrationStatusRepository = mock[RegistrationStatusRepository]
  private val mockRetryService: RetryService = mock[RetryService]
  private val mockAuditService: AuditService = mock[AuditService]

  private lazy val createRegistrationRoute: String = routes.RegistrationController.createRegistration().url

  override def beforeEach(): Unit = {
    reset(mockRegistrationService)
    reset(mockEnrolmentsConnector)
    reset(mockRegistrationStatusRepository)
    reset(mockRetryService)
    reset(mockAuditService)

    super.beforeEach()
  }


  private val amendRegistrationResponse: AmendRegistrationResponse =
    AmendRegistrationResponse(
      processingDateTime = LocalDateTime.now(),
      formBundleNumber = "12345",
      vrn = "123456789",
      iossReference = "IM900100000001",
      businessPartner = "businessPartner"
    )

  ".createRegistration" - {

    "must audit the event and return CREATED with a response payload when given a valid payload and the registration is created successfully" in {

      val fbNumber = "123456789"

      val etmpEnrolmentResponse =
        EtmpEnrolmentResponse(
          processingDateTime = LocalDateTime.now(stubClock),
          formBundleNumber = fbNumber,
          vrn = vrn.vrn,
          iossReference = "test",
          businessPartner = "test businessPartner"
        )

      when(mockRegistrationService.createRegistration(eqTo(etmpRegistrationRequest))) thenReturn Right(etmpEnrolmentResponse).toFuture
      when(mockRegistrationStatusRepository.delete(eqTo(fbNumber))) thenReturn true.toFuture
      when(mockRegistrationStatusRepository.insert(eqTo(RegistrationStatus(fbNumber, EtmpRegistrationStatus.Pending)))) thenReturn InsertSucceeded.toFuture
      when(mockEnrolmentsConnector.confirmEnrolment(any())(any())) thenReturn HttpResponse(204, "").toFuture
      when(mockRetryService.getEtmpRegistrationStatus(any(), any(), any())) thenReturn EtmpRegistrationStatus.Success.toFuture
      doNothing.when(mockAuditService).audit(any())(any(), any())

      val app = applicationBuilder
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .overrides(bind[EnrolmentsConnector].toInstance(mockEnrolmentsConnector))
        .overrides(bind[RegistrationStatusRepository].toInstance(mockRegistrationStatusRepository))
        .overrides(bind[RetryService].toInstance(mockRetryService))
        .overrides(bind[AuditService].toInstance(mockAuditService))
        .build()

      running(app) {
        val request = FakeRequest(POST, createRegistrationRoute)
          .withJsonBody(Json.toJson(etmpRegistrationRequest))

        val result = route(app, request).value

        implicit val dataRequest: AuthorisedMandatoryVrnRequest[AnyContentAsJson] =
          AuthorisedMandatoryVrnRequest(request, "id", vrn, None)

        val expectedAuditEvent = EtmpRegistrationRequestAuditModel.build(
          etmpRegistrationAuditType = EtmpRegistrationAuditType.CreateRegistration,
          etmpRegistrationRequest = etmpRegistrationRequest,
          etmpEnrolmentResponse = Some(etmpEnrolmentResponse),
          errorResponse = None,
          submissionResult = SubmissionResult.Success
        )

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(etmpEnrolmentResponse)
        verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
      }
    }

    "must throw exception when enrolment is not confirmed" in {

      val fbNumber = "123456789"

      val etmpEnrolmentResponse =
        EtmpEnrolmentResponse(
          processingDateTime = LocalDateTime.now(stubClock),
          formBundleNumber = fbNumber,
          vrn = vrn.vrn,
          iossReference = "test",
          businessPartner = "test businessPartner"
        )

      when(mockRegistrationService.createRegistration(eqTo(etmpRegistrationRequest))) thenReturn Right(etmpEnrolmentResponse).toFuture
      when(mockRegistrationStatusRepository.delete(eqTo(fbNumber))) thenReturn true.toFuture
      when(mockRegistrationStatusRepository.insert(eqTo(RegistrationStatus(fbNumber, EtmpRegistrationStatus.Pending)))) thenReturn InsertSucceeded.toFuture
      when(mockEnrolmentsConnector.confirmEnrolment(any())(any())) thenReturn HttpResponse(204, "").toFuture
      when(mockRetryService.getEtmpRegistrationStatus(any(), any(), any())) thenReturn EtmpRegistrationStatus.Error.toFuture

      val app = applicationBuilder
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .overrides(bind[EnrolmentsConnector].toInstance(mockEnrolmentsConnector))
        .overrides(bind[RegistrationStatusRepository].toInstance(mockRegistrationStatusRepository))
        .overrides(bind[RetryService].toInstance(mockRetryService))
        .build()

      running(app) {
        val request = FakeRequest(POST, createRegistrationRoute)
          .withJsonBody(Json.toJson(etmpRegistrationRequest))

        val result = route(app, request).value

        whenReady(result.failed, Timeout(Span(30, Seconds))) { exp =>
          exp mustBe EtmpException(s"Failed to add enrolment, got registration status Error")
        }
      }
    }

    "must audit the event and return Conflict when the error response is a Left EtmpEnrolmentError with error code 007" in {

      val etmpEnrolmentError = EtmpEnrolmentError("007", "test error")

      when(mockRegistrationService.createRegistration(eqTo(etmpRegistrationRequest))) thenReturn Left(etmpEnrolmentError).toFuture
      doNothing.when(mockAuditService).audit(any())(any(), any())

      val app = applicationBuilder
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .overrides(bind[AuditService].toInstance(mockAuditService))
        .build()

      running(app) {

        val request = FakeRequest(POST, createRegistrationRoute)
          .withJsonBody(Json.toJson(etmpRegistrationRequest))

        val result = route(app, request).value

        implicit val dataRequest: AuthorisedMandatoryVrnRequest[AnyContentAsJson] =
          AuthorisedMandatoryVrnRequest(request, "id", vrn, None)

        val expectedAuditEvent = EtmpRegistrationRequestAuditModel.build(
          etmpRegistrationAuditType = EtmpRegistrationAuditType.CreateRegistration,
          etmpRegistrationRequest = etmpRegistrationRequest,
          etmpEnrolmentResponse = None,
          errorResponse = Some(etmpEnrolmentError.body),
          submissionResult = SubmissionResult.Duplicate
        )

        status(result) mustBe CONFLICT
        contentAsJson(result) mustBe Json.toJson(
          s"Business Partner already has an active IOSS Subscription for this regime with error code ${etmpEnrolmentError.code}" +
          s"with message body ${etmpEnrolmentError.body}"
        )
        verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
      }
    }

    "must audit the event and return INTERNAL_SERVER_ERROR when there is any other error response" in {

      when(mockRegistrationService.createRegistration(eqTo(etmpRegistrationRequest))) thenReturn Left(ServiceUnavailable).toFuture
      doNothing.when(mockAuditService).audit(any())(any(), any())

      val app = applicationBuilder
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .overrides(bind[AuditService].toInstance(mockAuditService))
        .build()

      running(app) {

        val request = FakeRequest(POST, createRegistrationRoute)
          .withJsonBody(Json.toJson(etmpRegistrationRequest))

        val result = route(app, request).value

        implicit val dataRequest: AuthorisedMandatoryVrnRequest[AnyContentAsJson] =
          AuthorisedMandatoryVrnRequest(request, "id", vrn, None)

        val expectedAuditEvent = EtmpRegistrationRequestAuditModel.build(
          etmpRegistrationAuditType = EtmpRegistrationAuditType.CreateRegistration,
          etmpRegistrationRequest = etmpRegistrationRequest,
          etmpEnrolmentResponse = None,
          errorResponse = Some(ServiceUnavailable.body),
          submissionResult = SubmissionResult.Failure
        )

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.toJson(s"Internal server error ${ServiceUnavailable.body}")
        verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
      }
    }
  }

  "get" - {

    "must return OK and a registration when one is found" in {

      val mockService = mock[RegistrationService]
      when(mockService.get()(any(), any())) thenReturn RegistrationData.registrationWrapper.toFuture

      val app =
        applicationBuilder
          .overrides(bind[RegistrationService].toInstance(mockService))
          .build()

      running(app) {
        val request = FakeRequest(GET, routes.RegistrationController.get().url)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(RegistrationData.registrationWrapper)
      }
    }

    "must return INTERNAL_SERVER_ERROR when a registration connector response with Error" in {

      val mockService = mock[RegistrationService]
      when(mockService.get()(any(), any())) thenReturn Future.failed(EtmpException("Error Occurred"))

      val app =
        applicationBuilder
          .overrides(bind[RegistrationService].toInstance(mockService))
          .build()

      running(app) {
        val request = FakeRequest(GET, routes.RegistrationController.get().url)
        val result = route(app, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }

  "amend" - {

    "must return 201 when given a valid non reRegistration payload and the registration is created successfully with no enrollment" in {

      val mockService = mock[RegistrationService]
      when(mockService.amendRegistration(any())) thenReturn Future.successful(Right(amendRegistrationResponse))

      val app =
        applicationBuilder
          .overrides(bind[RegistrationService].toInstance(mockService))
          .build()

      running(app) {

        val request =
          FakeRequest(POST, routes.RegistrationController.amend().url)
            .withJsonBody(Json.toJson(RegistrationData.etmpAmendRegistrationRequest(reRegistration = false)))

        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(amendRegistrationResponse)
      }
    }

    "must return 201 when given a valid reRegistration payload and the registration is created successfully with an enrollment" in {
      val formBundleNumber = amendRegistrationResponse.formBundleNumber

      val mockService = mock[RegistrationService]
      when(mockService.amendRegistration(any())) thenReturn Future.successful(Right(amendRegistrationResponse))
      when(mockEnrolmentsConnector.confirmEnrolment(any())(any())) thenReturn HttpResponse(204, "").toFuture
      when(mockRegistrationStatusRepository.delete(eqTo(formBundleNumber))) thenReturn true.toFuture
      when(mockRegistrationStatusRepository.insert(eqTo(RegistrationStatus(formBundleNumber, EtmpRegistrationStatus.Pending)))) thenReturn InsertSucceeded.toFuture
      when(mockRetryService.getEtmpRegistrationStatus(any(), any(), any())) thenReturn EtmpRegistrationStatus.Success.toFuture

      val app =
        applicationBuilder
          .overrides(bind[RegistrationService].toInstance(mockService))
          .overrides(bind[RegistrationStatusRepository].toInstance(mockRegistrationStatusRepository))
          .overrides(bind[EnrolmentsConnector].toInstance(mockEnrolmentsConnector))
          .overrides(bind[RetryService].toInstance(mockRetryService))
          .build()

      running(app) {

        val request =
          FakeRequest(POST, routes.RegistrationController.amend().url)
            .withJsonBody(Json.toJson(RegistrationData.etmpAmendRegistrationRequest(reRegistration = true)))

        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(amendRegistrationResponse)
      }
    }



    "must return 400 when the JSON request payload is not a registration" in {

      val app = applicationBuilder.build()

      running(app) {

        val request =
          FakeRequest(POST, routes.RegistrationController.amend().url)
            .withJsonBody(Json.toJson(RegistrationData.invalidRegistration))

        val result = route(app, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }
  }
}