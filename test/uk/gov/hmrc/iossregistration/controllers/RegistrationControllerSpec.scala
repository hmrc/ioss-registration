package uk.gov.hmrc.iossregistration.controllers

import org.mockito.ArgumentMatchers.{eq => eqTo}
import play.api.http.Status.CREATED
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.etmp.EtmpEnrolmentResponse
import uk.gov.hmrc.iossregistration.models.{EtmpEnrolmentError, ServiceUnavailable}
import uk.gov.hmrc.iossregistration.services.RegistrationService
import uk.gov.hmrc.iossregistration.testutils.RegistrationData.etmpRegistrationRequest
import uk.gov.hmrc.iossregistration.utils.FutureSyntax.FutureOps

import java.time.LocalDateTime

class RegistrationControllerSpec extends BaseSpec {

  private val mockRegistrationService: RegistrationService = mock[RegistrationService]

  private lazy val createRegistrationRoute: String = routes.RegistrationController.create().url

  ".create" - {

    "must return CREATED with a response payload when given a valid payload and the registration is created successfully" in {

      val etmpEnrolmentResponse =
        EtmpEnrolmentResponse(
          processingDateTime = LocalDateTime.now(stubClock),
          formBundleNumber = Some("123456789"),
          vrn = vrn.vrn,
          iossReference = "test",
          businessPartner = "test businessPartner"
        )

      when(mockRegistrationService.createRegistration(eqTo(etmpRegistrationRequest))) thenReturn Right(etmpEnrolmentResponse).toFuture

      val app = applicationBuilder
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      running(app) {
        val request = FakeRequest(POST, createRegistrationRoute)
          .withJsonBody(Json.toJson(etmpRegistrationRequest))

        val result = route(app, request).value

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.toJson(etmpEnrolmentResponse)
      }
    }

    "must return Conflict when the error response is a Left EtmpEnrolmentError with error code 007" in {

      val etmpEnrolmentError = EtmpEnrolmentError("007", "test error")

      when(mockRegistrationService.createRegistration(eqTo(etmpRegistrationRequest))) thenReturn Left(etmpEnrolmentError).toFuture

      val app = applicationBuilder
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      running(app) {

        val request = FakeRequest(POST, createRegistrationRoute)
          .withJsonBody(Json.toJson(etmpRegistrationRequest))

        val result = route(app, request).value

        status(result) mustBe CONFLICT
        contentAsJson(result) mustBe Json.toJson(
          s"Business Partner already has an active IOSS Subscription for this regime with error code ${etmpEnrolmentError.code}" +
          s"with message body ${etmpEnrolmentError.body}"
        )
      }
    }

    "must return INTERNAL_SERVER_ERROR when there is any other error response" in {

      when(mockRegistrationService.createRegistration(eqTo(etmpRegistrationRequest))) thenReturn Left(ServiceUnavailable).toFuture

      val app = applicationBuilder
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      running(app) {

        val request = FakeRequest(POST, createRegistrationRoute)
          .withJsonBody(Json.toJson(etmpRegistrationRequest))

        val result = route(app, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.toJson(s"Internal server error ${ServiceUnavailable.body}")
      }
    }
  }
}