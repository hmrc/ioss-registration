package uk.gov.hmrc.iossregistration.services.cron


import org.mockito.ArgumentMatchers.any
import org.mockito.InOrder
import org.mockito.Mockito.{times, verify, when, inOrder as Ordered}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.iossregistration.models.RegistrationStatus
import uk.gov.hmrc.iossregistration.models.etmp.EtmpRegistrationStatus
import uk.gov.hmrc.iossregistration.repositories.RegistrationStatusRepository

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class CronServiceSpec extends AnyFreeSpec with MockitoSugar with Matchers {

  val repository: RegistrationStatusRepository = mock[RegistrationStatusRepository]
  val ec = ExecutionContext.Implicits.global
  val testCronService: CronService = CronService(repository)(ec)

  def makeRegistrationStatus(id: String): RegistrationStatus = {
    new RegistrationStatus(
      subscriptionId = id,
      status = EtmpRegistrationStatus.Pending,
      lastUpdated = Instant.now())
  }

  val findAllReturns: Future[Seq[RegistrationStatus]] = Future.successful(Seq(makeRegistrationStatus("1"), makeRegistrationStatus("2"), makeRegistrationStatus("3")))

  "this is a test" in {

    when(repository.findAll()).thenReturn(findAllReturns)

    when(repository.set(any())).thenReturn(Future.successful(makeRegistrationStatus("1")))

    val result = testCronService.fixExpiryDates().futureValue
    
    verify(repository, times(1)).findAll()
    verify(repository, times(3)).set(any())
    result mustBe findAllReturns.futureValue.size
  }
}
