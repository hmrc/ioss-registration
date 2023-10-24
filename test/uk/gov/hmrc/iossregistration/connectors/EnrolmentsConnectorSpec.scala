package uk.gov.hmrc.iossregistration.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, JsValue}
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.TaxEnrolmentErrorResponse

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

}
