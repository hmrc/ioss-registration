/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.iossregistration.services.external

import uk.gov.hmrc.iossregistration.config.AppConfig
import play.api.Logging
import uk.gov.hmrc.iossregistration.models.external.{ExternalEntry, ExternalRequest, ExternalResponse}
import uk.gov.hmrc.iossregistration.repositories.ExternalEntryRepository

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExternalEntryService @Inject()(
                                 externalEntryRepository: ExternalEntryRepository,
                                 appConfig: AppConfig,
                                 clock: Clock
                               )(implicit executionContext: ExecutionContext) extends Logging {

  def getExternalResponse(externalRequest: ExternalRequest, userId: String, language: Option[String] = None): Future[ExternalResponse] = {
    for {
      _ <- saveReturnUrl(userId, externalRequest)
    } yield {
      if (language.contains("cy")) {
        ExternalResponse(appConfig.externalEntryNoMoreWelshReturnUrl)
      } else {
        ExternalResponse(appConfig.externalEntryJourneyStartReturnUrl)
      }

    }
  }

  def getSavedResponseUrl(userId: String): Future[Option[String]] = {
    externalEntryRepository.get(userId).map(_.map(_.returnUrl))
  }

  private def saveReturnUrl(userId: String, externalRequest: ExternalRequest): Future[Boolean] = {
    val externalEntry = ExternalEntry(userId, externalRequest.returnUrl, Instant.now(clock))

    externalEntryRepository.set(externalEntry).map { _ => true }.recover {
      case e: Exception =>
        logger.error(s"An error occurred while saving the external returnUrl in the session, ${e.getMessage}", e)
        false
    }
  }
}
