package uk.gov.hmrc.iossregistration.controllers.external

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import play.api.http.Status.NO_CONTENT
import play.api.inject
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.external.{Event, EventData}
import uk.gov.hmrc.iossregistration.services.ChannelPreferenceService

import java.util.UUID
import scala.concurrent.Future

class EventControllerSpec extends BaseSpec {

  private val enrolmentString = s"HMRC-IOSS-ORG~IOSSNumber~$iossNumber"
  private val eventRequest = Event(UUID.randomUUID(), "subject", "groupId", EventData("email@email.com", Map("enrolment" -> enrolmentString)))

  "EventController" - {
    "accept an event and reply NoContent when successful" in {

      val mockChannelPreferenceService = mock[ChannelPreferenceService]

      when(mockChannelPreferenceService.updatePreferences(any())(any())) thenReturn Future.successful(true)

      val application = applicationBuilder
        .overrides(inject.bind[ChannelPreferenceService].toInstance(mockChannelPreferenceService))
        .build()

      running(application) {

        val request = FakeRequest(POST, routes.EventController.processBouncedEmailEvent().url)
          .withJsonBody(
            Json.toJson(eventRequest)
          )

        val result = route(application, request).value
        status(result) mustBe NO_CONTENT

        verify(mockChannelPreferenceService, times(1)).updatePreferences(eqTo(eventRequest))(any())
      }
    }

    "Reply BadRequest when there's a payload error" in {
      val mockChannelPreferenceService = mock[ChannelPreferenceService]

      when(mockChannelPreferenceService.updatePreferences(any())(any())) thenReturn Future.successful(false)

      val application = applicationBuilder
        .overrides(inject.bind[ChannelPreferenceService].toInstance(mockChannelPreferenceService))
        .build()

      running(application) {

        val request = FakeRequest(POST, routes.EventController.processBouncedEmailEvent().url)
          .withJsonBody(
            Json.toJson("""{"invalidJson": ""}""")
          )

        val result = route(application, request).value
        status(result) mustBe BAD_REQUEST

        verifyZeroInteractions(mockChannelPreferenceService)
      }
    }

    "Reply InternalServerError when there's an error from the downstream" in {
      val mockChannelPreferenceService = mock[ChannelPreferenceService]

      when(mockChannelPreferenceService.updatePreferences(any())(any())) thenReturn Future.successful(false)

      val application = applicationBuilder
        .overrides(inject.bind[ChannelPreferenceService].toInstance(mockChannelPreferenceService))
        .build()

      running(application) {

        val request = FakeRequest(POST, routes.EventController.processBouncedEmailEvent().url)
          .withJsonBody(
            Json.toJson(eventRequest)
          )

        val result = route(application, request).value
        status(result) mustBe INTERNAL_SERVER_ERROR

        verify(mockChannelPreferenceService, times(1)).updatePreferences(eqTo(eventRequest))(any())
      }
    }
  }

}
