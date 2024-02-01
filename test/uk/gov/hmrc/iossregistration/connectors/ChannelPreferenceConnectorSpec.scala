package uk.gov.hmrc.iossregistration.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.etmp.channelPreference.ChannelPreferenceRequest

class ChannelPreferenceConnectorSpec extends BaseSpec with WireMockHelper {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  ".updatePreferences" - {

    val basePath = "channel-preference/"

    def application: Application =
      new GuiceApplicationBuilder()
        .configure(
          "microservice.services.channel-preference.host" -> "127.0.0.1",
          "microservice.services.channel-preference.port" -> server.port,
          "microservice.services.channel-preference.authorizationToken" -> "auth-token",
          "microservice.services.channel-preference.basePath" -> basePath
        )
        .build()

    val url = s"/${basePath}income-tax/customer/IOSS/contact-preference"
    val channelPreferenceRequest = ChannelPreferenceRequest("IOSS", "IM9001234567", "email@email.com", unusableStatus = true)

    "must return an HttpResponse with status NoContent when the server returns NoContent" in {
      val app = application

      server.stubFor(
        put(urlEqualTo(url))
          .willReturn(aResponse().withStatus(OK))
      )

      running(app) {
        val connector = app.injector.instanceOf[ChannelPreferenceConnector]
        val result = connector.updatePreferences(channelPreferenceRequest).futureValue

        result.status mustEqual OK
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
            val connector = app.injector.instanceOf[ChannelPreferenceConnector]

            val result = connector.updatePreferences(channelPreferenceRequest).futureValue

            result.status mustEqual status
          }
        }
    }

  }

}
