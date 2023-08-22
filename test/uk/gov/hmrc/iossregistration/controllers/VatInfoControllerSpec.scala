package uk.gov.hmrc.iossregistration.controllers

import org.mockito.ArgumentMatchers.any
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.connectors.GetVatInfoConnector
import uk.gov.hmrc.iossregistration.models.DesAddress
import uk.gov.hmrc.iossregistration.models.des.VatCustomerInfo
import uk.gov.hmrc.iossregistration.utils.FutureSyntax.FutureOps

import java.time.LocalDate

class VatInfoControllerSpec extends BaseSpec {

  ".get" - {

    "must return OK and vat information when the connector returns vat info" in {

      val vatInfo = VatCustomerInfo(
        registrationDate = Some(LocalDate.now),
        address = DesAddress("line1", None, None, None, None, Some("AA11 1AA"), "GB"),
        partOfVatGroup = false,
        organisationName = Some("Foo"),
        singleMarketIndicator = Some(false),
        individualName = None
      )

      val mockConnector = mock[GetVatInfoConnector]
      when(mockConnector.getVatCustomerDetails(any())(any())) thenReturn Right(vatInfo).toFuture

      val app = applicationBuilder
        .overrides(bind[GetVatInfoConnector].toInstance(mockConnector))
        .build()

      running(app) {

        val request = FakeRequest(GET, routes.VatInfoController.get().url)
        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(vatInfo)
      }
    }
  }

}
