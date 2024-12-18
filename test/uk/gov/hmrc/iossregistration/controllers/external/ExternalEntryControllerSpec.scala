package uk.gov.hmrc.iossregistration.controllers.external

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject
import play.api.libs.json.{JsNull, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.generators.Generators
import uk.gov.hmrc.iossregistration.models.external.{ExternalEntryUrlResponse, ExternalRequest, ExternalResponse}
import uk.gov.hmrc.iossregistration.services.external.ExternalEntryService
import uk.gov.hmrc.iossregistration.services.AuditService

import scala.concurrent.Future

class ExternalEntryControllerSpec
  extends BaseSpec
    with ScalaCheckPropertyChecks
    with Generators {

  private val externalRequest = ExternalRequest("BTA", "exampleurl")

  ".onExternal" - {

    "when correct ExternalRequest is posted" - {
      "must respond with OK(IndexController.onPageLoad().url)" in {
        val mockExternalService = mock[ExternalEntryService]
        val mockAuditService = mock[AuditService]
        val url = "/pay-vat-on-goods-sold-to-eu/northern-ireland-register"

        when(mockExternalService.getExternalResponse(any(), any(), any())) thenReturn
          Future.successful(ExternalResponse(url))
        doNothing.when(mockAuditService).audit(any())(any(), any())

        val application = applicationBuilder
          .overrides(inject.bind[ExternalEntryService].toInstance(mockExternalService))
          .overrides(inject.bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {
          val request = FakeRequest(POST, uk.gov.hmrc.iossregistration.controllers.external.routes.ExternalEntryController.onExternal().url)
            .withJsonBody(
              Json.toJson(externalRequest)
            )

          val result = route(application, request).value
          status(result) mustBe OK
          contentAsJson(result).as[ExternalResponse] mustBe ExternalResponse(url)
          verify(mockAuditService, times(1)).audit(any())(any(), any())
        }
      }

    }


    "must respond with BadRequest" - {
      "when no body provided" in {
        val application = applicationBuilder
          .build()

        running(application) {
          val request = FakeRequest(POST, uk.gov.hmrc.iossregistration.controllers.external.routes.ExternalEntryController.onExternal().url)
            .withJsonBody(JsNull)

          val result = route(application, request).value
          status(result) mustBe BAD_REQUEST
        }
      }

      "when malformed body provided" in {
        val application = applicationBuilder
          .build()

        running(application) {
          val request = FakeRequest(POST, uk.gov.hmrc.iossregistration.controllers.external.routes.ExternalEntryController.onExternal().url)
            .withJsonBody(Json.toJson("wrong body"))

          val result = route(application, request).value
          status(result) mustBe BAD_REQUEST
        }
      }
    }

  }

  ".getExternalEntry" - {

    "when correct request with authorization" - {
      "must respond with correct url when present" in {
        val mockExternalService = mock[ExternalEntryService]
        val url = "/pay-vat-on-goods-sold-to-eu/northern-ireland-register"

        when(mockExternalService.getSavedResponseUrl(any())) thenReturn
          Future.successful(Some(url))

        val application = applicationBuilder
          .overrides(inject.bind[ExternalEntryService].toInstance(mockExternalService))
          .build()

        running(application) {
          val request = FakeRequest(GET, uk.gov.hmrc.iossregistration.controllers.external.routes.ExternalEntryController.getExternalEntry().url)
          val result = route(application, request).value
          status(result) mustBe OK
          contentAsJson(result).as[ExternalEntryUrlResponse] mustBe ExternalEntryUrlResponse(Some(url))
        }
      }

      "must respond with none when no url present" in {
        val mockExternalService = mock[ExternalEntryService]

        when(mockExternalService.getSavedResponseUrl(any())) thenReturn
          Future.successful(None)

        val application = applicationBuilder
          .overrides(inject.bind[ExternalEntryService].toInstance(mockExternalService))
          .build()

        running(application) {
          val request = FakeRequest(GET, uk.gov.hmrc.iossregistration.controllers.external.routes.ExternalEntryController.getExternalEntry().url)

          val result = route(application, request).value
          status(result) mustBe OK
          contentAsJson(result).as[ExternalEntryUrlResponse] mustBe ExternalEntryUrlResponse(None)
        }
      }

    }

  }

}

