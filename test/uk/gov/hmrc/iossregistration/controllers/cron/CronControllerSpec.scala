package uk.gov.hmrc.iossregistration.controllers.cron

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.apache.pekko.actor.ActorSystem
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.LoggerFactory
import uk.gov.hmrc.iossregistration.config.AppConfig
import uk.gov.hmrc.iossregistration.services.cron.CronService

import java.util
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

class CronControllerSpec extends AnyFreeSpec with MockitoSugar with Matchers {

  implicit val testSystem: ActorSystem = ActorSystem("test-system")
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val mockAppConfig: AppConfig = mock[AppConfig]

  "CronController" - {

    "should run twice and stop when the feature switch is true" in {
      when(mockAppConfig.lastUpdatedFeatureSwitch).thenReturn(true)

      val mockCronService = mock[CronService]
      when(mockCronService.fixExpiryDates()).thenReturn(Future.successful(20))
      val logger = LoggerFactory.getLogger("application.uk.gov.hmrc.iossregistration.controllers.cron.CronController")
        .asInstanceOf[ch.qos.logback.classic.Logger]

      val listAppender = new ListAppender[ILoggingEvent]()
      listAppender.start()
      logger.addAppender(listAppender)
      logger.setLevel(Level.INFO)
      val logs: util.List[ILoggingEvent] = listAppender.list

      new CronController(
        system = testSystem,
        cronService = mockCronService,
        initialDelay = 0.seconds,
        interval = 10.millis,
        mockAppConfig)

      Thread.sleep(1000)

      verify(mockCronService, times(2)).fixExpiryDates()
      logs must not be empty
      logs.get(0).getMessage mustEqual "Implementing TTL: 20 documents were read as last updated now and set to current date & time."
      logs.get(1).getMessage mustEqual "Implementing TTL: 20 documents were read as last updated now and set to current date & time."
      logs.get(2).getMessage mustEqual "The TTL updating job has run twice. Scheduler cancelled."

      logger.detachAppender(listAppender)
      testSystem.terminate()
    }

    "should run twice and stop when the feature switch is false" in {
      when(mockAppConfig.lastUpdatedFeatureSwitch).thenReturn(false)

      val mockCronService = mock[CronService]
      when(mockCronService.fixExpiryDates()).thenReturn(Future.successful(20))

      val logger = LoggerFactory.getLogger("application.uk.gov.hmrc.iossregistration.controllers.cron.CronController")
        .asInstanceOf[ch.qos.logback.classic.Logger]

      val listAppender = new ListAppender[ILoggingEvent]()
      listAppender.start()
      logger.addAppender(listAppender)
      logger.setLevel(Level.INFO)
      val logs: util.List[ILoggingEvent] = listAppender.list

      new CronController(
        system = testSystem,
        cronService = mockCronService,
        initialDelay = 0.seconds,
        interval = 10.millis,
        mockAppConfig)

      Thread.sleep(1000)

      verify(mockCronService, times(0)).fixExpiryDates()
      logs must not be empty
      logs.get(0).getMessage mustEqual "ExpiryScheduler disabled; not starting."

      logger.detachAppender(listAppender)
      testSystem.terminate()
    }
  }

}
