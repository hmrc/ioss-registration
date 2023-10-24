package uk.gov.hmrc.iossregistration.services.external

import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.config.AppConfig
import uk.gov.hmrc.iossregistration.models.external.{ExternalEntry, ExternalRequest, ExternalResponse}
import uk.gov.hmrc.iossregistration.repositories.ExternalEntryRepository

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ExternalEntryServiceSpec
  extends BaseSpec with BeforeAndAfterEach {

  private val mockRepository = mock[ExternalEntryRepository]
  private val mockConfig = mock[AppConfig]

  override def beforeEach(): Unit = {
    reset(mockRepository)
    super.beforeEach()
  }

  "getExternalResponse" - {
    val userId = "user-1234"
    val responseUrl = "/pay-vat-on-goods-sold-to-eu/northern-ireland-register"
    val welshResponseUrl = "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/no-more-welsh"
    when(mockConfig.externalEntryJourneyStartReturnUrl) thenReturn responseUrl
    when(mockConfig.externalEntryNoMoreWelshReturnUrl) thenReturn welshResponseUrl

    "must return ExternalResponse and save url in session when language is not Welsh" in {
      val externalRequest = ExternalRequest("BTA", "/business-account")
      val externalEntry = ExternalEntry(userId, responseUrl, Instant.now(stubClock))
      when(mockRepository.set(any())) thenReturn Future.successful(externalEntry)
      val service = new ExternalEntryService(mockRepository, mockConfig, stubClock)
      service.getExternalResponse(externalRequest, userId).futureValue mustBe ExternalResponse(responseUrl)
      verify(mockRepository, times(1)).set(any())
    }

    "must return ExternalResponse and save url in session when language is Welsh" in {
      val externalRequest = ExternalRequest("BTA", "/business-account")
      val externalEntry = ExternalEntry(userId, welshResponseUrl, Instant.now(stubClock))
      when(mockRepository.set(any())) thenReturn Future.successful(externalEntry)
      val service = new ExternalEntryService(mockRepository, mockConfig, stubClock)
      service.getExternalResponse(externalRequest, "id", Some("cy")).futureValue mustBe ExternalResponse(welshResponseUrl)
      verify(mockRepository, times(1)).set(any())
    }

    "must return ExternalResponse when session repository throws exception" in {
      val externalRequest = ExternalRequest("BTA", "/business-account")
      when(mockRepository.set(any())) thenReturn Future.failed(new Exception("error"))
      val service = new ExternalEntryService(mockRepository, mockConfig, stubClock)
      service.getExternalResponse(externalRequest, "id").futureValue mustBe ExternalResponse(responseUrl)
      verify(mockRepository, times(1)).set(any())
    }
  }

  "getSavedResponseUrl" - {
    val userId = "user-1234"
    val responseUrl = "/pay-vat-on-goods-sold-to-eu/northern-ireland-register"

    "must return url when response is Some" in {
      val externalEntry = ExternalEntry(userId, responseUrl, Instant.now(stubClock))
      when(mockRepository.get(any())) thenReturn Future.successful(Some(externalEntry))
      val service = new ExternalEntryService(mockRepository, mockConfig, stubClock)
      service.getSavedResponseUrl(userId).futureValue mustBe Some(responseUrl)
      verify(mockRepository, times(1)).get(any())
    }

    "must return None when None is returned by repo" in {
      when(mockRepository.get(any())) thenReturn Future.successful(None)
      val service = new ExternalEntryService(mockRepository, mockConfig, stubClock)
      service.getSavedResponseUrl(userId).futureValue mustBe None
      verify(mockRepository, times(1)).get(any())
    }
  }
}

