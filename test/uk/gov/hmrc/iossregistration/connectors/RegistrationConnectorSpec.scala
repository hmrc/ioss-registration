package uk.gov.hmrc.iossregistration.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import play.api.Application
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{CONTENT_TYPE, running}
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.connectors.RegistrationHttpParser.serviceName
import uk.gov.hmrc.iossregistration.models._
import uk.gov.hmrc.iossregistration.models.binders.Format.eisDateTimeFormatter
import uk.gov.hmrc.iossregistration.models.etmp.{EtmpEnrolmentErrorResponse, EtmpEnrolmentResponse, EtmpErrorDetail}
import uk.gov.hmrc.iossregistration.testutils.RegistrationData.etmpRegistrationRequest

import java.time.LocalDateTime

class RegistrationConnectorSpec extends BaseSpec with WireMockHelper {

  private def application: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.if.host" -> "127.0.0.1",
        "microservice.services.if.port" -> server.port,
        "microservice.services.if.authorizationToken" -> "auth-token",
        "microservice.services.if.environment" -> "test-environment"
      )
      .build()


  private val createRegistrationUrl = "/ioss-registration-stub/vec/iosssubscription/subdatatransfer/v1"

  private val fixedDelay = 21000

  private val timeOutSpan = 30

  ".create" - {

    "should return an ETMP Enrolment Response correctly" in {

      val now = LocalDateTime.now()

      val formBundleNumber = arbitrary[String].sample.value
      val iossReference = arbitraryVatNumberTraderId.arbitrary.sample.value.vatNumber
      val businessPartner = arbitrary[String].sample.value

      val app = application

      val requestJson = Json.stringify(Json.toJson(etmpRegistrationRequest))

      server.stubFor(
        post(urlEqualTo(createRegistrationUrl))
          .withHeader(AUTHORIZATION, equalTo("Bearer auth-token"))
          .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .withRequestBody(equalTo(requestJson))
          .willReturn(aResponse().withStatus(CREATED)
            .withBody(Json.stringify(Json.toJson(
              EtmpEnrolmentResponse(now, Some(formBundleNumber), vrn.vrn, iossReference, businessPartner)))))

      )

      running(app) {
        val connector = app.injector.instanceOf[RegistrationConnector]
        val result = connector.create(etmpRegistrationRequest).futureValue

        result mustBe Right(EtmpEnrolmentResponse(now, Some(formBundleNumber), vrn.vrn, iossReference, businessPartner))
      }
    }

    "should return Invalid Json when server responds with InvalidJson" in {

      val app = application

      val requestJson = Json.stringify(Json.toJson(etmpRegistrationRequest))

      server.stubFor(
        post(urlEqualTo(createRegistrationUrl))
          .withHeader(AUTHORIZATION, equalTo("Bearer auth-token"))
          .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .withRequestBody(equalTo(requestJson))
          .willReturn(aResponse()
            .withStatus(CREATED)
            .withBody(Json.stringify(Json.toJson("tests" -> "invalid"))))
      )

      running(app) {
        val connector = app.injector.instanceOf[RegistrationConnector]
        val result = connector.create(etmpRegistrationRequest).futureValue
        result mustBe Left(InvalidJson)
      }
    }

    "should return EtmpError when server responds with status 422 and correct error response json" in {

      val app = application

      val requestJson = Json.stringify(Json.toJson(etmpRegistrationRequest))

      val errorResponse = EtmpEnrolmentErrorResponse(EtmpErrorDetail(LocalDateTime.now(stubClock).format(eisDateTimeFormatter), "123", "error"))

      server.stubFor(
        post(urlEqualTo(createRegistrationUrl))
          .withHeader(AUTHORIZATION, equalTo("Bearer auth-token"))
          .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .withRequestBody(equalTo(requestJson))
          .willReturn(aResponse().withStatus(UNPROCESSABLE_ENTITY)
            .withBody(Json.stringify(Json.toJson(errorResponse))))
      )

      running(app) {
        val connector = app.injector.instanceOf[RegistrationConnector]
        val result = connector.create(etmpRegistrationRequest).futureValue
        result mustBe Left(EtmpEnrolmentError("123", "error"))
      }
    }

    "should return Invalid Json when server responds with status 422 and incorrect error response json" in {

      val app = application

      val requestJson = Json.stringify(Json.toJson(etmpRegistrationRequest))

      server.stubFor(
        post(urlEqualTo(createRegistrationUrl))
          .withHeader(AUTHORIZATION, equalTo("Bearer auth-token"))
          .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .withRequestBody(equalTo(requestJson))
          .willReturn(aResponse().withStatus(UNPROCESSABLE_ENTITY)
            .withBody(Json.stringify(Json.toJson("tests" -> "invalid"))))
      )

      running(app) {
        val connector = app.injector.instanceOf[RegistrationConnector]
        val result = connector.create(etmpRegistrationRequest).futureValue
        result mustBe Left(UnexpectedResponseStatus(UNPROCESSABLE_ENTITY, "Unexpected response from etmp registration, received status 422"))
      }
    }


    Seq((NOT_FOUND, NotFound), (CONFLICT, Conflict), (INTERNAL_SERVER_ERROR, ServerError), (BAD_REQUEST, InvalidVrn), (SERVICE_UNAVAILABLE, ServiceUnavailable), (123, UnexpectedResponseStatus(123, s"Unexpected response from ${serviceName}, received status 123")))
      .foreach { error =>
        s"should return correct error response when server responds with ${error._1}" in {

          val app = application

          val requestJson = Json.stringify(Json.toJson(etmpRegistrationRequest))

          server.stubFor(
            post(urlEqualTo(createRegistrationUrl))
              .withHeader(AUTHORIZATION, equalTo("Bearer auth-token"))
              .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
              .withRequestBody(equalTo(requestJson))
              .willReturn(aResponse().withStatus(error._1))
          )

          running(app) {
            val connector = app.injector.instanceOf[RegistrationConnector]
            val result = connector.create(etmpRegistrationRequest).futureValue
            result mustBe Left(UnexpectedResponseStatus(error._1, s"Unexpected response from etmp registration, received status ${error._1}"))
          }
        }
      }

    "should return Error Response when server responds with Http Exception" in {

      val app = application

      val requestJson = Json.stringify(Json.toJson(etmpRegistrationRequest))

      server.stubFor(
        post(urlEqualTo(createRegistrationUrl))
          .withHeader(AUTHORIZATION, equalTo("Bearer auth-token"))
          .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .withRequestBody(equalTo(requestJson))
          .willReturn(aResponse()
            .withStatus(GATEWAY_TIMEOUT)
            .withFixedDelay(fixedDelay))
      )

      running(app) {
        val connector = app.injector.instanceOf[RegistrationConnector]
        whenReady(connector.create(etmpRegistrationRequest), Timeout(Span(timeOutSpan, Seconds))) { exp =>
          exp.isLeft mustBe true
          exp.left.toOption.get mustBe a[ErrorResponse]
        }
      }
    }
  }
}
