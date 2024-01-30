package uk.gov.hmrc.iossregistration.services

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.test.Helpers.running
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.connectors.ChannelPreferenceConnector
import uk.gov.hmrc.iossregistration.models.etmp.channelPreference.ChannelPreferenceRequest
import uk.gov.hmrc.iossregistration.models.external.{Event, EventData}
import uk.gov.hmrc.iossregistration.utils.FutureSyntax.FutureOps

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class ChannelPreferenceServiceSpec extends BaseSpec with BeforeAndAfterEach {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val enrolmentString = s"HMRC-IOSS-ORG~IOSSNumber~$iossNumber"
  private val email = "email@email.com"
  private val eventRequest = Event(UUID.randomUUID(), "subject", "groupId", EventData(email, Map("enrolment" -> enrolmentString)))

  private val mockChannelPreferenceConnector: ChannelPreferenceConnector = mock[ChannelPreferenceConnector]
  private val channelPreferenceService = new ChannelPreferenceService(mockChannelPreferenceConnector)

  override def beforeEach(): Unit = {
    reset(mockChannelPreferenceConnector)
  }

  "RegistrationServiceSpec#updatePreferences" - {

    "must call channel preference with correct IOSS number and reply when succesful" in {

      val mockedResponse = HttpResponse(OK, "")

      when(mockChannelPreferenceConnector.updatePreferences(any())(any())) thenReturn mockedResponse.toFuture

      val app = applicationBuilder
        .build()

      running(app) {

        channelPreferenceService.updatePreferences(eventRequest).futureValue mustBe true

        val expectedRequest = ChannelPreferenceRequest("IOSS", iossNumber, email, "true")
        verify(mockChannelPreferenceConnector, times(1)).updatePreferences(eqTo(expectedRequest))(any())
      }
    }

    "must reply with false when failure" in {

      val mockedResponse = HttpResponse(INTERNAL_SERVER_ERROR, "")

      when(mockChannelPreferenceConnector.updatePreferences(any())(any())) thenReturn mockedResponse.toFuture

      val app = applicationBuilder
        .build()

      running(app) {

        channelPreferenceService.updatePreferences(eventRequest).futureValue mustBe false

        val expectedRequest = ChannelPreferenceRequest("IOSS", iossNumber, email, "true")
        verify(mockChannelPreferenceConnector, times(1)).updatePreferences(eqTo(expectedRequest))(any())
      }
    }
  }

}
