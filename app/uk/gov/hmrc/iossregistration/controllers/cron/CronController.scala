/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.iossregistration.controllers.cron

import org.apache.pekko.actor.ActorSystem
import uk.gov.hmrc.iossregistration.config.AppConfig
import uk.gov.hmrc.iossregistration.logging.Logging
import uk.gov.hmrc.iossregistration.services.cron.CronService

import javax.inject.*
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*

@Singleton
class CronController @Inject()(
                                system: ActorSystem,
                                cronService: CronService,
                                appConfig: AppConfig,
                                initialDelay: FiniteDuration = 10.seconds,
                              )(implicit ec: ExecutionContext) extends Logging {

  system.scheduler.scheduleOnce(
    delay = initialDelay
  ) {
    if (appConfig.lastUpdatedFeatureSwitch) {
      cronService.fixExpiryDates().map { entriesChanged =>
        logger.info(s"Implementing TTL: $entriesChanged documents were read as last updated Instant.now and set to current date & time.")
      }
    } else {
      logger.info("ExpiryScheduler disabled; not starting.")
    }
  }
}