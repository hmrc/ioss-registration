package uk.gov.hmrc.iossregistration.services

import akka.actor.ActorSystem
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.RegistrationStatus
import uk.gov.hmrc.iossregistration.models.etmp.EtmpRegistrationStatus
import uk.gov.hmrc.iossregistration.repositories.RegistrationStatusRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetryServiceSpec extends BaseSpec with BeforeAndAfterEach {

  private val registrationStatusRepository = mock[RegistrationStatusRepository]
  private val actorSystem = mock[ActorSystem]

  private val retryService = new RetryService(registrationStatusRepository, actorSystem)

  override def beforeEach(): Unit = {
    reset(registrationStatusRepository)
    reset(actorSystem)
    super.beforeEach()
  }

  ".getEtmpRegistrationStatus" - {

    "returns status as EtmpRegistrationStatus Success when remaining time is > 0" in {

      when(registrationStatusRepository.get(anyString())) thenReturn Future.successful(Some(RegistrationStatus(anyString(), EtmpRegistrationStatus.Success)))

      retryService.getEtmpRegistrationStatus(10,1000,"1000000001-id").futureValue mustEqual EtmpRegistrationStatus.Success
      verify(registrationStatusRepository, times(1)).get(anyString())

    }

    "returns status as EtmpRegistrationStatus Error when remaining time is = 1" in {

      when(registrationStatusRepository.get(anyString())) thenReturn Future.successful(Some(RegistrationStatus(anyString(), EtmpRegistrationStatus.Pending)))

      retryService.getEtmpRegistrationStatus(1, 1000, "1000000001-id").futureValue mustEqual EtmpRegistrationStatus.Error
      verify(registrationStatusRepository, times(1)).get(anyString())

    }

    "returns status as EtmpRegistrationStatus Error when Error is returned from Registration status Repository" in {

      when(registrationStatusRepository.get(anyString())) thenReturn Future.successful(Some(RegistrationStatus(anyString(), EtmpRegistrationStatus.Error)))

      retryService.getEtmpRegistrationStatus(10, 1000, "1000000001-id").futureValue mustEqual EtmpRegistrationStatus.Error
      verify(registrationStatusRepository, times(1)).get(anyString())

    }

    "returns status as EtmpRegistrationStatus Error when remaining time is = 0" in {

      retryService.getEtmpRegistrationStatus(0, 1000, "1000000001-id").futureValue mustEqual EtmpRegistrationStatus.Error

    }
  }

}
