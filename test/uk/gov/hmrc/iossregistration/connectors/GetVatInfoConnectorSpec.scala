package uk.gov.hmrc.iossregistration.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalacheck.Gen
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.DesAddress
import uk.gov.hmrc.iossregistration.models.core._
import uk.gov.hmrc.iossregistration.models.des.VatCustomerInfo

import java.time.LocalDate

class GetVatInfoConnectorSpec extends BaseSpec with WireMockHelper {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.get-vat-info.host" -> "127.0.0.1",
        "microservice.services.get-vat-info.port" -> server.port,
        "microservice.services.get-vat-info.authorizationToken" -> "auth-token",
        "microservice.services.get-vat-info.environment" -> "test-environment"
      )
      .build()

  private val desUrl = s"/ioss-registration-stub/vat/customer/vrn/${vrn.value}/information"

  "getVatCustomerInfo" - {

    "must return a Right(VatCustomerInfo) when the server returns OK and a recognised payload" in {

      val app = application

      val responseJson =
        """{
          |  "approvedInformation": {
          |    "customerDetails": {
          |      "effectiveRegistrationDate": "2000-01-01",
          |      "partyType": "Z2",
          |      "organisationName": "Foo",
          |      "singleMarketIndicator": false
          |    },
          |    "PPOB": {
          |      "address": {
          |        "line1": "line 1",
          |        "line2": "line 2",
          |        "postCode": "AA11 1AA",
          |        "countryCode": "GB"
          |      }
          |    }
          |  }
          |}""".stripMargin

      server.stubFor(
        get(urlEqualTo(desUrl))
          .withHeader("Authorization", equalTo("Bearer auth-token"))
          .withHeader("Environment", equalTo("test-environment"))
          .willReturn(ok(responseJson))
      )

      running(app) {
        val connector = app.injector.instanceOf[GetVatInfoConnector]
        val result = connector.getVatCustomerDetails(vrn).futureValue

        val expectedResult = VatCustomerInfo(
          registrationDate = Some(LocalDate.of(2000, 1, 1)),
          desAddress = DesAddress("line 1", Some("line 2"), None, None, None, Some("AA11 1AA"), "GB"),
          partOfVatGroup = true,
          organisationName = Some("Foo"),
          singleMarketIndicator = Some(false),
          individualName = None,
          deregistrationDecisionDate = None
        )

        result mustBe Right(expectedResult)
      }
    }

    "must return Left(NotFound) when the server returns NOT_FOUND" in {

      val app = application

      server.stubFor(
        get(urlEqualTo(desUrl))
          .willReturn(notFound())
      )

      running(app) {

        val connector = app.injector.instanceOf[GetVatInfoConnector]
        val result = connector.getVatCustomerDetails(vrn).futureValue

        result mustBe Left(NotFound)
      }
    }

    "must return Left(InvalidVrn) when the server returns BAD_REQUEST" in {

      val app = application

      server.stubFor(
        get(urlEqualTo(desUrl))
          .willReturn(badRequest())
      )

      running(app) {

        val connector = app.injector.instanceOf[GetVatInfoConnector]
        val result = connector.getVatCustomerDetails(vrn).futureValue

        result mustBe Left(InvalidVrn)
      }
    }

    "must return Left(ServiceUnavailable) when the server returns SERVICE_UNAVAILABLE" in {

      val app = application

      server.stubFor(
        get(urlEqualTo(desUrl))
          .willReturn(serviceUnavailable())
      )

      running(app) {

        val connector = app.injector.instanceOf[GetVatInfoConnector]
        val result = connector.getVatCustomerDetails(vrn).futureValue

        result mustBe Left(ServiceUnavailable)
      }
    }

    "must return Left(ServerError) when the server returns INTERNAL_SERVER_ERROR" in {

      val app = application

      server.stubFor(
        get(urlEqualTo(desUrl))
          .willReturn(serverError())
      )

      running(app) {

        val connector = app.injector.instanceOf[GetVatInfoConnector]
        val result = connector.getVatCustomerDetails(vrn).futureValue

        result mustBe Left(ServerError)
      }
    }

    "must return Left(InvalidJson) when the server returns OK with a payload that cannot be parsed" in {

      val app = application

      val responseJson = """{ "foo": "bar" }"""

      server.stubFor(
        get(urlEqualTo(desUrl))
          .willReturn(ok(responseJson))
      )

      running(app) {

        val connector = app.injector.instanceOf[GetVatInfoConnector]
        val result = connector.getVatCustomerDetails(vrn).futureValue

        result mustBe Left(InvalidJson)
      }
    }

    "must return Left(UnexpectedResponse) when the server returns an unexpected response code" in {

      val app = application

      val status = Gen.oneOf(401, 402, 403, 501, 502).sample.value

      val errorResponseJson =
        s"""{
           |  "error": "$status",
           |  "errorMessage": "Error"
           |}""".stripMargin

      server.stubFor(
        get(urlEqualTo(desUrl))
          .willReturn(aResponse()
            .withStatus(status)
            .withBody(errorResponseJson))
      )

      running(app) {

        val connector = app.injector.instanceOf[GetVatInfoConnector]
        val result = connector.getVatCustomerDetails(vrn).futureValue

        result mustBe Left(UnexpectedResponseStatus(status, s"Unexpected response from DES, received status $status with body $errorResponseJson"))
      }
    }

    "must return Left(GatewayTimeout) when the server returns a GatewayTimeoutException" in {

      val app = application

      server.stubFor(
        get(urlEqualTo(desUrl))
          .willReturn(aResponse()
            .withStatus(504)
            .withFixedDelay(21000))
      )

      running(app) {

        val connector = app.injector.instanceOf[GetVatInfoConnector]
        whenReady(connector.getVatCustomerDetails(vrn), Timeout(Span(30, Seconds))) { exp =>
          exp mustBe Left(GatewayTimeout)
        }
      }
    }
  }
}
