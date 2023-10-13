package uk.gov.hmrc.iossregistration.services

import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.scalatest.BeforeAndAfterEach
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.connectors.{RegistrationConnector, WireMockHelper}
import uk.gov.hmrc.iossregistration.models.etmp.EtmpEnrolmentResponse
import uk.gov.hmrc.iossregistration.utils.FutureSyntax.FutureOps

import java.time.LocalDateTime

class RegistrationServiceSpec extends BaseSpec with WireMockHelper with BeforeAndAfterEach {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val registrationService = new RegistrationService(mockRegistrationConnector)

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
  }

  ".createRegistration" - {

    "must create registration request and return a successful ETMP enrolment response" in {

      val etmpEnrolmentResponse =
        EtmpEnrolmentResponse(
          processingDateTime = LocalDateTime.now(stubClock),
          formBundleNumber = Some("123456789"),
          vrn = vrn.vrn,
          iossReference = "test",
          businessPartner = "test businessPartner"
        )

      when(mockRegistrationConnector.create(etmpRegistrationRequest)) thenReturn Right(etmpEnrolmentResponse).toFuture

      val app = applicationBuilder
        .build()

      running(app) {

        registrationService.createRegistration(etmpRegistrationRequest).futureValue mustBe Right(etmpEnrolmentResponse)
        verify(mockRegistrationConnector, times(1)).create(eqTo(etmpRegistrationRequest))
      }
    }

//    "must return an ETMP Enrolment Error Response when there is an error creating registration" in {
//
//      val etmpEnrolmentErrorResponse = EtmpEnrolmentErrorResponse(
//        errors = EtmpErrorDetail(
//          processingDate = LocalDateTime.now(stubClock).format(eisDateTimeFormatter),
//          code = "123",
//          text = "error message test"
//        )
//      )
//
//      when(mockRegistrationConnector.create(etmpRegistrationRequest)) thenReturn
//        Left(EtmpEnrolmentError(etmpEnrolmentErrorResponse.errors.code, etmpEnrolmentErrorResponse.errors.text)).toFuture
//
//      val app = applicationBuilder
//        .build()
//
//      running(app) {
//
//        whenReady(registrationService.createRegistration(etmpRegistrationRequest).failed) {
//          exp => exp mustBe EtmpException("Registration not created")
//        }
//        verify(mockRegistrationConnector, times(1)).create(eqTo(etmpRegistrationRequest))
//      }
//    }
//
//    "must throw an ETMP exception when connector returns any other error" in {
//
//      when(mockRegistrationConnector.create(etmpRegistrationRequest)) thenReturn Left(ServiceUnavailable).toFuture
//
//      val app = applicationBuilder
//        .build()
//
//      running(app) {
//
//        whenReady(registrationService.createRegistration(etmpRegistrationRequest).failed) {
//          exp => exp mustBe EtmpException(s"There was an error creating Registration enrolment from ETMP: ${ServiceUnavailable.body}")
//        }
//        verify(mockRegistrationConnector, times(1)).create(eqTo(etmpRegistrationRequest))
//      }
//    }
  }
}
