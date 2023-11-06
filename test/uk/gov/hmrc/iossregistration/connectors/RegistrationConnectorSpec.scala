package uk.gov.hmrc.iossregistration.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import play.api.Application
import play.api.http.HeaderNames.{AUTHORIZATION, CONTENT_TYPE}
import play.api.http.MimeTypes
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.connectors.RegistrationHttpParser.serviceName
import uk.gov.hmrc.iossregistration.models._
import uk.gov.hmrc.iossregistration.models.binders.Format.eisDateTimeFormatter
import uk.gov.hmrc.iossregistration.models.etmp.{AmendRegistrationResponse, EtmpEnrolmentErrorResponse, EtmpEnrolmentResponse, EtmpErrorDetail}
import uk.gov.hmrc.iossregistration.testutils.DisplayRegistrationData.{arbitraryDisplayRegistration, optionalDisplayRegistration, writesEtmpSchemeDetails}
import uk.gov.hmrc.iossregistration.testutils.RegistrationData.etmpRegistrationRequest

import java.time.{LocalDate, LocalDateTime}

class RegistrationConnectorSpec extends BaseSpec with WireMockHelper {

  private def application: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.create-registration.host" -> "127.0.0.1",
        "microservice.services.create-registration.port" -> server.port,
        "microservice.services.create-registration.authorizationToken" -> "auth-token",
        "microservice.services.create-registration.environment" -> "test-environment",
        "microservice.services.display-registration.host" -> "127.0.0.1",
        "microservice.services.display-registration.port" -> server.port,
        "microservice.services.display-registration.authorizationToken" -> "auth-token",
        "microservice.services.display-registration.environment" -> "test-environment",
        "microservice.services.amend-registration.host" -> "127.0.0.1",
        "microservice.services.amend-registration.port" -> server.port,
        "microservice.services.amend-registration.authorizationToken" -> "auth-token"
      )
      .build()


  private val createRegistrationUrl = "/ioss-registration-stub/vec/iosssubscription/subdatatransfer/v1"

  private def getDisplayRegistrationUrl(iossNumber: String) = s"/ioss-registration-stub/vec/iossregistration/viewreg/v1/$iossNumber"

  private val amendRegistrationUrl = "/ioss-registration-stub/vec/iosssubscription/amendreg/v1"

  private val fixedDelay = 21000

  private val timeOutSpan = 30

  private val amendRegistrationResponse: AmendRegistrationResponse =
    AmendRegistrationResponse(
      processingDateTime = LocalDateTime.now(),
      formBundleNumber = "12345",
      vrn = "123456789",
      iossReference = "IM900100000001",
      businessPartner = "businessPartner"
    )

  ".get" - {

    "Should parse Registration payload with all optional fields present correctly" in {

      val app = application

      val etmpRegistration = arbitraryDisplayRegistration

      val responseJson =
        s"""{
           | "tradingNames": ${Json.toJson(etmpRegistration.tradingNames)},
           | "schemeDetails": ${Json.toJson(etmpRegistration.schemeDetails)(writesEtmpSchemeDetails)},
           | "bankDetails": ${Json.toJson(etmpRegistration.bankDetails)},
           | "exclusions": ${Json.toJson(etmpRegistration.exclusions)},
           | "adminUse": ${Json.toJson(etmpRegistration.adminUse)}
           |}""".stripMargin

      server.stubFor(
        get(urlEqualTo(getDisplayRegistrationUrl(iossNumber)))
          .withHeader(AUTHORIZATION, equalTo("Bearer auth-token"))
          .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .willReturn(aResponse().withStatus(OK)
            .withBody(responseJson))
      )

      running(app) {
        val connector = app.injector.instanceOf[RegistrationConnector]
        val result = connector.get(iossNumber).futureValue
        val expectedResult = etmpRegistration
        result mustBe Right(expectedResult)

      }
    }

    "Should parse Registration payload without all optional fields present correctly" in {

      val app = application

      val etmpRegistration = optionalDisplayRegistration

      val responseJson =
        s"""{
           | "tradingNames": [],
           | "schemeDetails": {
           |   "commencementDate": "2023-01-01",
           |   "euRegistrationDetails": [],
           |   "previousEURegistrationDetails": [],
           |   "onlineMarketPlace": true,
           |   "websites": [],
           |   "contactDetails": {
           |     "contactNameOrBusinessAddress": "Mr Test",
           |     "businessTelephoneNumber": "1234567890",
           |     "businessEmailAddress": "test@testEmail.com"
           |   }
           | },
           | "bankDetails": {
           |   "accountName": "Bank Account Name",
           |   "iban": "GB33BUKB20201555555555"
           | },
           | "exclusions": [],
           | "adminUse": {
           |   "changeDate": "${LocalDate.now(stubClock)}T00:00"
           | }
           |}""".stripMargin

      server.stubFor(
        get(urlEqualTo(getDisplayRegistrationUrl(iossNumber)))
          .withHeader(AUTHORIZATION, equalTo("Bearer auth-token"))
          .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .willReturn(aResponse().withStatus(OK)
            .withBody(responseJson))
      )

      running(app) {
        val connector = app.injector.instanceOf[RegistrationConnector]
        val result = connector.get(iossNumber).futureValue
        val expectedResult = etmpRegistration
        result mustBe Right(expectedResult)

      }
    }

    "must return Left(InvalidJson) when the server returns OK with a payload that cannot be parsed" in {

      val app = application

      val responseJson = """{ "foo": "bar" }"""

      server.stubFor(
        get(urlEqualTo(getDisplayRegistrationUrl(iossNumber)))
          .willReturn(ok(responseJson))
      )

      running(app) {

        val connector = app.injector.instanceOf[RegistrationConnector]
        val result = connector.get(iossNumber).futureValue

        result mustBe Left(InvalidJson)
      }
    }


    val body = ""

    Seq((NOT_FOUND, ServerError), (CONFLICT, ServerError), (INTERNAL_SERVER_ERROR, ServerError), (BAD_REQUEST, ServerError), (SERVICE_UNAVAILABLE, ServerError), (123, ServerError))
      .foreach { error =>
        s"should return correct error response when server responds with ${error._1}" in {

          val app = application

          server.stubFor(
            get(urlEqualTo(getDisplayRegistrationUrl(iossNumber)))
              .withHeader(AUTHORIZATION, equalTo("Bearer auth-token"))
              .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
              .willReturn(aResponse().withStatus(error._1).withBody(body))
          )

          running(app) {
            val connector = app.injector.instanceOf[RegistrationConnector]
            val result = connector.get(iossNumber).futureValue
            result mustBe Left(error._2)
          }
        }
      }

    "should return Error Response when server responds with Http Exception" in {

      val app = application

      server.stubFor(
        get(urlEqualTo(getDisplayRegistrationUrl(iossNumber)))
          .withHeader(AUTHORIZATION, equalTo("Bearer auth-token"))
          .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .willReturn(aResponse()
            .withStatus(GATEWAY_TIMEOUT)
            .withFixedDelay(fixedDelay))
      )

      running(app) {
        val connector = app.injector.instanceOf[RegistrationConnector]
        whenReady(connector.get(iossNumber), Timeout(Span(timeOutSpan, Seconds))) { exp =>
          exp.isLeft mustBe true
          exp.left.toOption.get mustBe a[ErrorResponse]
        }
      }
    }
  }

  ".createRegistration" - {

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
              EtmpEnrolmentResponse(now, formBundleNumber, vrn.vrn, iossReference, businessPartner)))))

      )

      running(app) {
        val connector = app.injector.instanceOf[RegistrationConnector]
        val result = connector.createRegistration(etmpRegistrationRequest).futureValue

        result mustBe Right(EtmpEnrolmentResponse(now, formBundleNumber, vrn.vrn, iossReference, businessPartner))
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
        val result = connector.createRegistration(etmpRegistrationRequest).futureValue
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
        val result = connector.createRegistration(etmpRegistrationRequest).futureValue
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
        val result = connector.createRegistration(etmpRegistrationRequest).futureValue
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
            val result = connector.createRegistration(etmpRegistrationRequest).futureValue
            result mustBe Left(UnexpectedResponseStatus(error._1, s": Unexpected response from etmp registration, received status ${error._1}"))
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
        whenReady(connector.createRegistration(etmpRegistrationRequest), Timeout(Span(timeOutSpan, Seconds))) { exp =>
          exp.isLeft mustBe true
          exp.left.toOption.get mustBe a[ErrorResponse]
        }
      }
    }
  }

  ".amendRegistration" - {

    "must return Ok with an Amend Registration response when a valid payload is sent" in {

      val app = application

      val requestJson = Json.stringify(Json.toJson(etmpRegistrationRequest))

      server.stubFor(
        put(urlEqualTo(amendRegistrationUrl))
          .withHeader(AUTHORIZATION, equalTo("Bearer auth-token"))
          .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .withRequestBody(equalTo(requestJson))
          .willReturn(aResponse()
            .withStatus(OK)
            .withBody(Json.stringify(Json.toJson(amendRegistrationResponse)))
          )
      )

      running(app) {

        val connector = app.injector.instanceOf[RegistrationConnector]
        val result = connector.amendRegistration(etmpRegistrationRequest).futureValue

        result mustBe Right(amendRegistrationResponse)

      }
    }

    "should return not found when server responds with NOT_FOUND" in {

      val app = application

      val requestJson = Json.stringify(Json.toJson(registrationRequest))

      server.stubFor(
        put(urlEqualTo(amendRegistrationUrl))
          .withHeader(AUTHORIZATION, equalTo("Bearer auth-token"))
          .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .withRequestBody(equalTo(requestJson))
          .willReturn(aResponse()
            .withStatus(NOT_FOUND))
      )

      running(app) {

        val connector = app.injector.instanceOf[RegistrationConnector]
        val result = connector.amendRegistration(etmpRegistrationRequest).futureValue

        result mustBe Left(NotFound)
      }
    }

    "should return Error Response when server responds with Http Exception" in {

      val app = application

      server.stubFor(
        put(urlEqualTo(amendRegistrationUrl))
          .withHeader(AUTHORIZATION, equalTo("Bearer auth-token"))
          .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .willReturn(aResponse()
            .withStatus(GATEWAY_TIMEOUT)
            .withFixedDelay(fixedDelay))
      )

      running(app) {

        val connector = app.injector.instanceOf[RegistrationConnector]
        whenReady(connector.amendRegistration(etmpRegistrationRequest), Timeout(Span(timeOutSpan, Seconds))) { exp =>
          exp.isLeft mustBe true
          exp.left.toOption.get mustBe a[ErrorResponse]
        }
      }
    }
  }
}
