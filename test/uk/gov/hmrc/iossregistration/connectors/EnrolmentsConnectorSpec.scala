package uk.gov.hmrc.iossregistration.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.enrolments.{EACDEnrolment, EACDEnrolments, EACDIdentifiers}
import uk.gov.hmrc.iossregistration.models.{TaxEnrolmentErrorResponse, UnexpectedResponseStatus}

import java.time.LocalDateTime

class EnrolmentsConnectorSpec extends BaseSpec with WireMockHelper {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  val errorResponseBody = "Error"
  val response: JsValue = Json.toJson(TaxEnrolmentErrorResponse(NOT_FOUND.toString, errorResponseBody))

  ".confirmEnrolment" - {

    val basePath = "tax-enrolments/"

    def application: Application =
      new GuiceApplicationBuilder()
        .configure(
          "microservice.services.enrolments.host" -> "127.0.0.1",
          "microservice.services.enrolments.port" -> server.port,
          "microservice.services.enrolments.authorizationToken" -> "auth-token",
          "microservice.services.enrolments.basePath" -> basePath
        )
        .build()

    val subscriptionId = "123456789"
    val url = s"/${basePath}subscriptions/$subscriptionId/subscriber"

    "must return an HttpResponse with status NoContent when the server returns NoContent" in {

      val app = application

      server.stubFor(
        put(urlEqualTo(url))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      running(app) {
        val connector = app.injector.instanceOf[EnrolmentsConnector]
        val result = connector.confirmEnrolment(subscriptionId).futureValue

        result.status mustEqual NO_CONTENT
      }
    }


    Seq(BAD_REQUEST, UNAUTHORIZED).foreach {
      status =>
        s"must return an Http response with $status when the server returns $status" in {

          val app = application

          server.stubFor(
            put(urlEqualTo(url))
              .willReturn(aResponse().withStatus(status))
          )

          running(app) {
            val connector = app.injector.instanceOf[EnrolmentsConnector]

            val result = connector.confirmEnrolment(subscriptionId).futureValue

            result.status mustEqual status
          }
        }
    }

  }

  ".es2" - {

    val basePath = "enrolment-store/"

    def application: Application =
      new GuiceApplicationBuilder()
        .configure(
          "microservice.services.enrolment-store-proxy.host" -> "127.0.0.1",
          "microservice.services.enrolment-store-proxy.port" -> server.port,
          "microservice.services.enrolment-store-proxy.authorizationToken" -> "auth-token",
          "microservice.services.enrolment-store-proxy.basePath" -> basePath
        )
        .build()

    val userId = "12345678902124"
    val url = s"/${basePath}enrolment-store/users/$userId/enrolments?service=HMRC-IOSS-ORG"

    "must return with the model from processing the json when the server returns OK" in {

      val expectedResponse = EACDEnrolments(Seq(EACDEnrolment("HMRC-IOSS-ORG", "Activated", Some(LocalDateTime.of(2024, 2, 21, 10, 47, 46)), Seq(EACDIdentifiers("IOSSNumber", "IM9001234567")))))

      val jsonString =
        """{
          |  "enrolments": [
          |    {
          |      "service": "HMRC-IOSS-ORG",
          |      "state": "Activated",
          |      "activationDate": "2024-02-21 10:47:46.02",
          |      "identifiers": [
          |        {
          |          "key": "IOSSNumber",
          |          "value": "IM9001234567"
          |        }
          |      ]
          |    }
          |  ]
          |}""".stripMargin

      val app = application

      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(aResponse()
            .withStatus(OK)
            .withBody(jsonString)
          )
      )

      running(app) {
        val connector = app.injector.instanceOf[EnrolmentsConnector]
        val result = connector.es2(userId).futureValue

        result mustBe Right(expectedResponse)
      }
    }

    "must return an empty enrolment when the server returns NO_CONTENT" in {

      val app = application

      server.stubFor(
        get(urlEqualTo(url))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      running(app) {
        val connector = app.injector.instanceOf[EnrolmentsConnector]
        val result = connector.es2(userId).futureValue

        result mustBe Right(EACDEnrolments(Seq.empty))
      }
    }

    Seq(BAD_REQUEST, UNAUTHORIZED).foreach {
      status =>
        s"must return an Http response with $status when the server returns $status" in {

          val app = application

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(status)
                .withBody("There was an error")
              )
          )

          running(app) {
            val connector = app.injector.instanceOf[EnrolmentsConnector]

            val result = connector.es2(userId).futureValue

            result mustBe Left(UnexpectedResponseStatus(status, "There was an error"))
          }
        }
    }

  }

}
