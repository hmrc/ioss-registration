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

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Action
import uk.gov.hmrc.iossregistration.controllers.actions.AuthenticatedControllerComponents
import uk.gov.hmrc.iossregistration.models.EtmpEnrolmentError
import uk.gov.hmrc.iossregistration.models.etmp.{EtmpEnrolmentErrorResponse, EtmpRegistrationRequest}
import uk.gov.hmrc.iossregistration.services.RegistrationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.ExecutionContext

case class RegistrationController @Inject()(
                                             cc: AuthenticatedControllerComponents,
                                             registrationService: RegistrationService,
                                             clock: Clock
                                           )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  // TODO - > Need to create Audit in reg service
  def createRegistration(): Action[EtmpRegistrationRequest] = cc.authAndRequireVat()(parse.json[EtmpRegistrationRequest]).async {
    implicit request =>
      registrationService.createRegistration(request.body).map {
        case Right(response) =>
          Created(Json.toJson(response))
        case Left(EtmpEnrolmentError(EtmpEnrolmentErrorResponse.alreadyActiveSubscriptionErrorCode, body)) =>
          logger.error(
            s"Business Partner already has an active IOSS Subscription for this regime with error code ${EtmpEnrolmentErrorResponse.alreadyActiveSubscriptionErrorCode}" +
            s"with message body $body"
          )
          Conflict(Json.toJson(
            s"Business Partner already has an active IOSS Subscription for this regime with error code ${EtmpEnrolmentErrorResponse.alreadyActiveSubscriptionErrorCode}" +
              s"with message body $body"
          ))
        case Left(error) =>
          logger.error(s"Internal server error ${error.body}")
          InternalServerError(Json.toJson(s"Internal server error ${error.body}"))
      }
  }
}
