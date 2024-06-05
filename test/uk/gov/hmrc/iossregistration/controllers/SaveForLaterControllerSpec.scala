package uk.gov.hmrc.iossregistration.controllers

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.Status.{CREATED, NOT_FOUND, OK, UNAUTHORIZED}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, POST, contentAsJson, defaultAwaitTimeout, route, running, status, writeableOf_AnyContentAsEmpty, writeableOf_AnyContentAsJson}
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.controllers.actions.FakeFailingAuthConnector
import uk.gov.hmrc.iossregistration.generators.Generators
import uk.gov.hmrc.iossregistration.models.SavedUserAnswers
import uk.gov.hmrc.iossregistration.models.requests.{SaveForLaterRequest, SaveForLaterResponse}
import uk.gov.hmrc.iossregistration.services.SaveForLaterService

import scala.concurrent.Future

class SaveForLaterControllerSpec
  extends BaseSpec
    with ScalaCheckPropertyChecks
    with Generators {

  ".post" - {

    val s4lRequest = arbitrary[SaveForLaterRequest].sample.value
    val savedAnswers = arbitrary[SavedUserAnswers].sample.value

    lazy val request =
      FakeRequest(POST, routes.SaveForLaterController.post().url)
        .withJsonBody(Json.toJson(s4lRequest))

    "must save a VAT return and respond with Created" in {
      val mockSaveForLaterService = mock[SaveForLaterService]

      when(mockSaveForLaterService.saveAnswers(any()))
        .thenReturn(Future.successful(savedAnswers))

      val app =
        applicationBuilder
          .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
          .build()

      running(app) {

        val result = route(app, request).value

        status(result) mustEqual CREATED
        contentAsJson(result) mustBe Json.toJson(savedAnswers)
        verify(mockSaveForLaterService, times(1)).saveAnswers(eqTo(s4lRequest))
      }
    }

    "must respond with Unauthorized when the user is not authorised" in {

      val app =
        new GuiceApplicationBuilder()
          .overrides(bind[AuthConnector].toInstance(new FakeFailingAuthConnector(new MissingBearerToken)))
          .build()

      running(app) {

        val result = route(app, request).value
        status(result) mustEqual UNAUTHORIZED
      }
    }
  }

  ".get" - {
    val savedAnswers = arbitrary[SaveForLaterResponse].sample.value
    lazy val request =
      FakeRequest(GET, routes.SaveForLaterController.get().url)

    "must return OK and a response when Saved User Answers are found for the vrn and period" in {
      val mockSaveForLaterService = mock[SaveForLaterService]

      when(mockSaveForLaterService.get(any())(any()))
        .thenReturn(Future.successful(Some(savedAnswers)))

      val app =
        applicationBuilder
          .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
          .build()

      running(app) {

        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsJson(result) mustBe Json.toJson(savedAnswers)
        verify(mockSaveForLaterService, times(1)).get(ArgumentMatchers.eq(vrn))(any())
      }
    }

    "must return NOT_FOUND when no answers are found" in {
      val mockSaveForLaterService = mock[SaveForLaterService]

      when(mockSaveForLaterService.get(any())(any()))
        .thenReturn(Future.successful(None))

      val app =
        applicationBuilder
          .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
          .build()

      running(app) {

        val result = route(app, request).value

        status(result) mustEqual NOT_FOUND
        verify(mockSaveForLaterService, times(1)).get(ArgumentMatchers.eq(vrn))(any())
      }
    }
  }

  ".delete" - {
    lazy val request =
      FakeRequest(GET, routes.SaveForLaterController.delete().url)

    "must return OK" in {
      val mockSaveForLaterService = mock[SaveForLaterService]

      when(mockSaveForLaterService.delete(any()))
        .thenReturn(Future.successful(true))

      val app =
        applicationBuilder
          .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
          .build()

      running(app) {

        val result = route(app, request).value

        status(result) mustEqual OK
        contentAsJson(result) mustBe Json.toJson(true)
        verify(mockSaveForLaterService, times(1)).delete(any())
      }
    }
  }


}

