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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.after
import uk.gov.hmrc.iossregistration.models.etmp.EtmpRegistrationStatus
import uk.gov.hmrc.iossregistration.repositories.RegistrationStatusRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

@Singleton
class RetryService @Inject()(
                              registrationStatusRepository: RegistrationStatusRepository,
                              actorSystem: ActorSystem
                            )(implicit ec: ExecutionContext) {

  def getEtmpRegistrationStatus(remaining: Int, delay: Int, subscriptionId: String): Future[EtmpRegistrationStatus] = {

    if (remaining > 0) {
      registrationStatusRepository.get(subscriptionId).flatMap {
        case Some(registrationStatus) => registrationStatus.status match {

          case EtmpRegistrationStatus.Success => Future(EtmpRegistrationStatus.Success)

          case EtmpRegistrationStatus.Pending => if (remaining == 1) {
            Future(EtmpRegistrationStatus.Error)
          } else {
            after(delay.milliseconds, actorSystem.scheduler) {
              getEtmpRegistrationStatus(remaining - 1, delay, subscriptionId)
            }
          }
          case _ => Future(EtmpRegistrationStatus.Error)
        }
        case _ => Future(EtmpRegistrationStatus.Error)
      }
    } else {
      Future(EtmpRegistrationStatus.Error)
    }
  }

}