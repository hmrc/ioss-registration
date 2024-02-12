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

package uk.gov.hmrc.iossregistration.controllers

import play.api.libs.json.JsValue
import play.api.mvc.Action
import uk.gov.hmrc.iossregistration.controllers.actions.AuthenticatedControllerComponents
import play.api.Logging
import uk.gov.hmrc.iossregistration.models.enrolments.EnrolmentStatus
import uk.gov.hmrc.iossregistration.models.RegistrationStatus
import uk.gov.hmrc.iossregistration.models.etmp.EtmpRegistrationStatus
import uk.gov.hmrc.iossregistration.repositories.RegistrationStatusRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentsSubscriptionController @Inject()(
                                                  cc: AuthenticatedControllerComponents,
                                                  registrationStatusRepository: RegistrationStatusRepository,
                                                )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def authoriseEnrolment(subscriptionId: String): Action[JsValue] =
    Action.async(parse.json) {
      implicit request =>
        val enrolmentStatus = (request.body \ "state").as[EnrolmentStatus]
        if (enrolmentStatus == EnrolmentStatus.Success) {
          logger.info(s"Enrolment complete for $subscriptionId, enrolment state = $enrolmentStatus")
          registrationStatusRepository.set(RegistrationStatus(subscriptionId,
            status = EtmpRegistrationStatus.Success))
        } else {
          logger.error(s"Enrolment failure for $subscriptionId, enrolment state = $enrolmentStatus ${request.body}")
          registrationStatusRepository.set(RegistrationStatus(subscriptionId,
            status = EtmpRegistrationStatus.Error))
        }
        Future.successful(NoContent)
    }

}
