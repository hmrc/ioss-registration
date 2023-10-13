package uk.gov.hmrc.iossregistration.controllers

import org.mockito.ArgumentMatchers.any
import org.scalacheck.Gen
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.connectors.GetVatInfoConnector
import uk.gov.hmrc.iossregistration.models.des.VatCustomerInfo
import uk.gov.hmrc.iossregistration.models._
import uk.gov.hmrc.iossregistration.utils.FutureSyntax.FutureOps

import java.time.LocalDate

class VatInfoControllerSpec extends BaseSpec {

  private val mockConnector = mock[GetVatInfoConnector]

  ".get" - {

    "must return OK and vat information when the connector returns vat info" in {

      val vatInfo = VatCustomerInfo(
        registrationDate = Some(LocalDate.now),
        desAddress = DesAddress("line1", None, None, None, None, Some("AA11 1AA"), "GB"),
        partOfVatGroup = false,
        organisationName = Some("Foo"),
        singleMarketIndicator = false,
        individualName = None,
        deregistrationDecisionDate = None,
        overseasIndicator = false
      )

      when(mockConnector.getVatCustomerDetails(any())(any())) thenReturn Right(vatInfo).toFuture

      val app = applicationBuilder
        .overrides(bind[GetVatInfoConnector].toInstance(mockConnector))
        .build()

      running(app) {

        val request = FakeRequest(GET, routes.VatInfoController.get().url)
        val result = route(app, request).value

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(vatInfo)
      }
    }

    "must return NotFound when the connector returns Not Found" in {

      when(mockConnector.getVatCustomerDetails(any())(any())) thenReturn Left(NotFound).toFuture

      val app = applicationBuilder.overrides(bind[GetVatInfoConnector].toInstance(mockConnector)).build()

      running(app) {

        val request = FakeRequest(GET, routes.VatInfoController.get().url)
        val result = route(app, request).value

        status(result) mustBe NOT_FOUND
      }
    }

    "must return INTERNAL_SERVER_ERROR when the connector returns a failure other than Not Found" in {

      val response = Gen.oneOf(InvalidJson, ServerError, ServiceUnavailable, InvalidVrn).sample.value

      when(mockConnector.getVatCustomerDetails(any())(any())) thenReturn Left(response).toFuture

      val app = applicationBuilder.overrides(bind[GetVatInfoConnector].toInstance(mockConnector)).build()

      running(app) {

        val request = FakeRequest(GET, routes.VatInfoController.get().url)
        val result = route(app, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}

