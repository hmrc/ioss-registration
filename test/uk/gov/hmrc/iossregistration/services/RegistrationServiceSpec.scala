package uk.gov.hmrc.iossregistration.services

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.scalatest.BeforeAndAfterEach
import org.mockito.Mockito.*
import play.api.test.Helpers.running
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.connectors.{GetVatInfoConnector, RegistrationConnector}
import uk.gov.hmrc.iossregistration.models.etmp.EtmpEnrolmentResponse
import uk.gov.hmrc.iossregistration.models.etmp.amend.AmendRegistrationResponse
import uk.gov.hmrc.iossregistration.models.{DisplayRegistrationNotFound, ErrorResponse, EtmpEnrolmentError, EtmpException, NotFound}
import uk.gov.hmrc.iossregistration.testutils.RegistrationData.{displayRegistration, etmpRegistrationRequest, registrationWrapper}
import uk.gov.hmrc.iossregistration.utils.FutureSyntax.FutureOps

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationServiceSpec extends BaseSpec with BeforeAndAfterEach {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockGetVatInfoConnector: GetVatInfoConnector = mock[GetVatInfoConnector]
  private val registrationService = new RegistrationService(mockRegistrationConnector, mockGetVatInfoConnector)

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
    reset(mockGetVatInfoConnector)
  }

  ".createRegistration" - {

    "must create registration request and return a successful ETMP enrolment response" in {

      val etmpEnrolmentResponse =
        EtmpEnrolmentResponse(
          processingDateTime = LocalDateTime.now(stubClock),
          formBundleNumber = "123456789",
          vrn = vrn.vrn,
          iossReference = "test",
          businessPartner = "test businessPartner"
        )

      when(mockRegistrationConnector.createRegistration(etmpRegistrationRequest)) thenReturn Right(etmpEnrolmentResponse).toFuture

      val app = applicationBuilder
        .build()

      running(app) {

        registrationService.createRegistration(etmpRegistrationRequest).futureValue mustBe Right(etmpEnrolmentResponse)
        verify(mockRegistrationConnector, times(1)).createRegistration(eqTo(etmpRegistrationRequest))
      }
    }
  }

  ".get" - {

    "must return registration when both connectors return right" in {
      when(mockRegistrationConnector.get(any())) thenReturn Right(displayRegistration).toFuture
      when(mockGetVatInfoConnector.getVatCustomerDetails(any())(any())) thenReturn Future.successful(Right(vatCustomerInfo))
      registrationService.get(iossNumber, vrn).futureValue mustBe registrationWrapper
      verify(mockRegistrationConnector, times(1)).get(iossNumber)
      verify(mockGetVatInfoConnector, times(1)).getVatCustomerDetails(Vrn("123456789"))
    }

    "must return Some(registration) when both connectors return right" in {
      when(mockRegistrationConnector.get(any())) thenReturn Right(displayRegistration).toFuture
      when(mockGetVatInfoConnector.getVatCustomerDetails(any())(any())) thenReturn Right(vatCustomerInfo).toFuture
      registrationService.get(iossNumber, vrn).futureValue mustBe registrationWrapper
      verify(mockRegistrationConnector, times(1)).get(iossNumber)
      verify(mockGetVatInfoConnector, times(1)).getVatCustomerDetails(Vrn("123456789"))
    }

    "must return an exception when no customer VAT details are found" in {
      when(mockRegistrationConnector.get(any())) thenReturn Right(displayRegistration).toFuture
      when(mockGetVatInfoConnector.getVatCustomerDetails(any())(any())) thenReturn Future.successful(Left(NotFound))
      whenReady(registrationService.get(iossNumber, vrn).failed) {
        exp => exp mustBe a[Exception]
      }
      verify(mockRegistrationConnector, times(1)).get(iossNumber)
      verify(mockGetVatInfoConnector, times(1)).getVatCustomerDetails(Vrn("123456789"))

    }

    "must return an ETMP Exception when the Registration Connector returns Left(error)" in {
      when(mockRegistrationConnector.get(any())) thenReturn Future.failed(EtmpException("Error occurred"))
      whenReady(registrationService.get(iossNumber, vrn).failed) {
        exp => exp mustBe EtmpException(s"Error occurred")
      }
      verify(mockRegistrationConnector, times(1)).get(iossNumber)
    }

    "must throw an EtmpException when both connectors return Left errors" in {
      val displayError = EtmpEnrolmentError("400", "Display error body")
      val vatInfoError = DisplayRegistrationNotFound("404", "Vat info error body")

      when(mockRegistrationConnector.get(any())) thenReturn Future.successful(Left(displayError))
      when(mockGetVatInfoConnector.getVatCustomerDetails(any())(any())) thenReturn Future.successful(Left(vatInfoError))

      whenReady(registrationService.get(iossNumber, vrn).failed) { exception =>
        exception mustBe EtmpException(s"There was an error getting Registration from ETMP: ${displayError.body} and ${vatInfoError.body}")
      }

      verify(mockRegistrationConnector, times(1)).get(iossNumber)
      verify(mockGetVatInfoConnector, times(1)).getVatCustomerDetails(vrn)
    }

    "must throw an EtmpException when the Registration Connector returns Left error" in {
      val displayError = EtmpEnrolmentError("400", "Display error body")
      
      when(mockRegistrationConnector.get(any())) thenReturn Future.successful(Left(displayError))
      when(mockGetVatInfoConnector.getVatCustomerDetails(any())(any())) thenReturn Future.successful(Right(vatCustomerInfo))
      
      whenReady(registrationService.get(iossNumber, vrn).failed) { exception =>
        exception mustBe EtmpException(s"There was an error getting Registration from ETMP: ${displayError.body}")
      }

      verify(mockRegistrationConnector, times(1)).get(iossNumber)
      verify(mockGetVatInfoConnector, times(1)).getVatCustomerDetails(vrn)
    }

    "must throw an EtmpException when the VAT Info Connector returns Left error" in {
      
      val vatInfoError = DisplayRegistrationNotFound("404", "Vat info error body")
      
      when(mockRegistrationConnector.get(any())) thenReturn Future.successful(Right(displayRegistration))
      when(mockGetVatInfoConnector.getVatCustomerDetails(any())(any())) thenReturn Future.successful(Left(vatInfoError))
      
      whenReady(registrationService.get(iossNumber, vrn).failed) { exception =>
        exception mustBe EtmpException(s"There was an error getting vat info from ETMP: ${vatInfoError.body}")
      }

      verify(mockRegistrationConnector, times(1)).get(iossNumber)
      verify(mockGetVatInfoConnector, times(1)).getVatCustomerDetails(vrn)
    }

  }

  ".amendRegistration" - {

    "must create registration request and return a successful ETMP enrolment response" in {

      val amendRegistrationResponse =
        AmendRegistrationResponse(
          processingDateTime = LocalDateTime.now(stubClock),
          formBundleNumber = "123456789",
          vrn = vrn.vrn,
          iossReference = "test",
          businessPartner = "test businessPartner"
        )

      when(mockRegistrationConnector.amendRegistration(etmpAmendRegistrationRequest())) thenReturn Right(amendRegistrationResponse).toFuture

      val app = applicationBuilder
        .build()

      running(app) {

        registrationService.amendRegistration(etmpAmendRegistrationRequest()).futureValue mustBe  Right(amendRegistrationResponse)
        verify(mockRegistrationConnector, times(1)).amendRegistration(eqTo(etmpAmendRegistrationRequest()))
      }
    }
  }
}
