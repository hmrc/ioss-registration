package uk.gov.hmrc.iossregistration.services

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.scalatest.BeforeAndAfterEach
import org.mockito.Mockito.*
import play.api.test.Helpers.running
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.connectors.{GetVatInfoConnector, RegistrationConnector}
import uk.gov.hmrc.iossregistration.models.etmp.EtmpIdType.FTR
import uk.gov.hmrc.iossregistration.models.etmp.{EtmpCustomerIdentificationNew, EtmpEnrolmentResponse}
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
      verify(mockGetVatInfoConnector, times(1)).getVatCustomerDetails(Vrn(displayRegistration.customerIdentification.asInstanceOf[EtmpCustomerIdentificationNew].idValue))
    }

    "must return registration wrapper with vat information when both connectors return right" in {
      when(mockRegistrationConnector.get(any())) thenReturn Right(displayRegistration).toFuture
      when(mockGetVatInfoConnector.getVatCustomerDetails(any())(any())) thenReturn Right(vatCustomerInfo).toFuture
      registrationService.get(iossNumber, vrn).futureValue mustBe registrationWrapper
      verify(mockRegistrationConnector, times(1)).get(iossNumber)
      verify(mockGetVatInfoConnector, times(1)).getVatCustomerDetails(Vrn(displayRegistration.customerIdentification.asInstanceOf[EtmpCustomerIdentificationNew].idValue))
    }

    "must return registration wrapper without vat information when a client does not have vat information" in {
      val nonVatDisplayReg = displayRegistration
        .copy(customerIdentification = EtmpCustomerIdentificationNew(FTR, "ForeignTaxRef"))
      val nonVatRegWrapper = registrationWrapper.copy(vatInfo = None, registration = nonVatDisplayReg)

      when(mockRegistrationConnector.get(any())) thenReturn Right(nonVatDisplayReg).toFuture
      registrationService.get(iossNumber, vrn).futureValue mustBe nonVatRegWrapper
      verify(mockRegistrationConnector, times(1)).get(iossNumber)
      verify(mockGetVatInfoConnector, times(0)).getVatCustomerDetails(Vrn(displayRegistration.customerIdentification.asInstanceOf[EtmpCustomerIdentificationNew].idValue))
    }

    "must return an exception when no customer VAT details are found" in {
      when(mockRegistrationConnector.get(any())) thenReturn Right(displayRegistration).toFuture
      when(mockGetVatInfoConnector.getVatCustomerDetails(any())(any())) thenReturn Future.successful(Left(NotFound))
      whenReady(registrationService.get(iossNumber, vrn).failed) {
        exp => exp mustBe a[Exception]
      }
      verify(mockRegistrationConnector, times(1)).get(iossNumber)
      verify(mockGetVatInfoConnector, times(1)).getVatCustomerDetails(Vrn(displayRegistration.customerIdentification.asInstanceOf[EtmpCustomerIdentificationNew].idValue))

    }

    "must return an ETMP Exception when the Registration Connector returns Left(error)" in {
      when(mockRegistrationConnector.get(any())) thenReturn Future.failed(EtmpException("Error occurred"))
      whenReady(registrationService.get(iossNumber, vrn).failed) {
        exp => exp mustBe EtmpException(s"Error occurred")
      }
      verify(mockRegistrationConnector, times(1)).get(iossNumber)
    }
    
    "must throw an EtmpException when the Registration Connector returns Left error" in {
      val displayError = EtmpEnrolmentError("400", "Display error body")
      
      when(mockRegistrationConnector.get(any())) thenReturn Future.successful(Left(displayError))
      
      whenReady(registrationService.get(iossNumber, vrn).failed) { exception =>
        exception mustBe EtmpException(s"There was an error getting Registration from ETMP: ${displayError.body}")
      }

      verify(mockRegistrationConnector, times(1)).get(iossNumber)
      verify(mockGetVatInfoConnector, times(0)).getVatCustomerDetails(Vrn(displayRegistration.customerIdentification.asInstanceOf[EtmpCustomerIdentificationNew].idValue))
    }

    "must throw an EtmpException when the VAT Info Connector returns Left error" in {
      
      val vatInfoError = DisplayRegistrationNotFound("404", "Vat info error body")
      
      when(mockRegistrationConnector.get(any())) thenReturn Future.successful(Right(displayRegistration))
      when(mockGetVatInfoConnector.getVatCustomerDetails(any())(any())) thenReturn Future.successful(Left(vatInfoError))
      
      whenReady(registrationService.get(iossNumber, vrn).failed) { exception =>
        exception mustBe EtmpException(s"There was an error retrieving the VAT information from ETMP: ${vatInfoError.body}")
      }

      verify(mockRegistrationConnector, times(1)).get(iossNumber)
      verify(mockGetVatInfoConnector, times(1)).getVatCustomerDetails(Vrn(displayRegistration.customerIdentification.asInstanceOf[EtmpCustomerIdentificationNew].idValue))
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
