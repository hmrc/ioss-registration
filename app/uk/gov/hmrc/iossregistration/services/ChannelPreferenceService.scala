/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.iossregistration.services

import play.api.http.Status.OK
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.connectors.ChannelPreferenceConnector
import uk.gov.hmrc.iossregistration.logging.Logging
import uk.gov.hmrc.iossregistration.models.etmp.channelPreference.ChannelPreferenceRequest
import uk.gov.hmrc.iossregistration.models.external.Event

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChannelPreferenceService @Inject()(channelPreferenceConnector: ChannelPreferenceConnector)
                                        (implicit ec: ExecutionContext) extends Logging {

  def updatePreferences(event: Event)(implicit hc: HeaderCarrier): Future[Boolean] = {

    val channelPreferenceRequest = ChannelPreferenceRequest(
      identifierType = "IOSS",
      identifier = getIossFromTags(event.event.tags),
      emailAddress = event.event.emailAddress,
      unusableStatus = true
    )

    channelPreferenceConnector.updatePreferences(channelPreferenceRequest).map { response =>
      response.status match {
        case OK =>
          true
        case status =>
          logger.error(s"Received unknown status $status from channel preference with body ${response.body}")
          false
      }

    }

  }

  private def getIossFromTags(tags: Map[String, String]): String = {

    val enrolmentMatcherString = "HMRC-IOSS-ORG~IOSSNumber~"

    tags.get("enrolment") match {
      case Some(enrolment) => enrolment.substring(enrolmentMatcherString.length)
      case _ =>
        val exception = new IllegalStateException(s"Unable to get enrolment from event with tags $tags")
        logger.error(exception.getMessage, exception)
        throw exception
    }

  }

}
