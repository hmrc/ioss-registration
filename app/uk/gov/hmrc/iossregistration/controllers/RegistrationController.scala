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
import uk.gov.hmrc.iossregistration.config.AppConfig
import uk.gov.hmrc.iossregistration.connectors.EnrolmentsConnector
import uk.gov.hmrc.iossregistration.controllers.actions.AuthenticatedControllerComponents
import uk.gov.hmrc.iossregistration.models.EtmpEnrolmentError
import uk.gov.hmrc.iossregistration.models.audit.{EtmpRegistrationAuditType, EtmpRegistrationRequestAuditModel, SubmissionResult}
import uk.gov.hmrc.iossregistration.models.etmp.{EtmpEnrolmentErrorResponse, EtmpRegistrationRequest}
import uk.gov.hmrc.iossregistration.services.{AuditService, RegistrationService}
import uk.gov.hmrc.iossregistration.models.{EtmpEnrolmentError, EtmpException, RegistrationStatus}
import uk.gov.hmrc.iossregistration.models.etmp.{EtmpEnrolmentErrorResponse, EtmpRegistrationRequest, EtmpRegistrationStatus}
import uk.gov.hmrc.iossregistration.repositories.RegistrationStatusRepository
import uk.gov.hmrc.iossregistration.services.{RegistrationService, RetryService}
import uk.gov.hmrc.iossregistration.utils.FutureSyntax.FutureOps
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.ExecutionContext

case class RegistrationController @Inject()(
                                             cc: AuthenticatedControllerComponents,
                                             enrolmentsConnector: EnrolmentsConnector,
                                             registrationService: RegistrationService,
                                             auditService: AuditService,
                                             registrationStatusRepository: RegistrationStatusRepository,
                                             retryService: RetryService,
                                             appConfig: AppConfig,
                                             clock: Clock
                                           )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def createRegistration(): Action[EtmpRegistrationRequest] = cc.authAndRequireVat()(parse.json[EtmpRegistrationRequest]).async {
    implicit request =>
      registrationService.createRegistration(request.body).flatMap {
        case Right(response) =>
          (for {
            _ <- registrationStatusRepository.delete(response.formBundleNumber)
            _ <- registrationStatusRepository.insert(RegistrationStatus(subscriptionId = response.formBundleNumber,
              status = EtmpRegistrationStatus.Pending))
            enrolmentResponse <- enrolmentsConnector.confirmEnrolment(response.formBundleNumber)
          } yield {
            enrolmentResponse.status match {
              case NO_CONTENT =>
                retryService.getEtmpRegistrationStatus(appConfig.maxRetryCount, appConfig.delay, response.formBundleNumber).map {
                  case EtmpRegistrationStatus.Success =>
                    auditService.audit(EtmpRegistrationRequestAuditModel.build(
                      EtmpRegistrationAuditType.CreateRegistration, request.body, Some(response), None, SubmissionResult.Success)
                    )
                    logger.info("Successfully created registration and enrolment")
                    Created(Json.toJson(response))
                  case registrationStatus =>
                    logger.error(s"Failed to add enrolment, got registration status $registrationStatus")
                    registrationStatusRepository.set(RegistrationStatus(subscriptionId = response.formBundleNumber, status = EtmpRegistrationStatus.Error))
                    throw EtmpException(s"Failed to add enrolment, got registration status $registrationStatus")
                }
              case status =>
                logger.error(s"Failed to add enrolment - $status with body ${enrolmentResponse.body}")
                throw EtmpException(s"Failed to add enrolment - ${enrolmentResponse.body}")
            }
          }).flatten
        case Left(EtmpEnrolmentError(EtmpEnrolmentErrorResponse.alreadyActiveSubscriptionErrorCode, body)) =>
          auditService.audit(EtmpRegistrationRequestAuditModel.build(
            EtmpRegistrationAuditType.CreateRegistration, request.body, None, Some(body), SubmissionResult.Duplicate)
          )
          logger.error(
            s"Business Partner already has an active IOSS Subscription for this regime with error code ${EtmpEnrolmentErrorResponse.alreadyActiveSubscriptionErrorCode}" +
            s"with message body $body"
          )
          Conflict(Json.toJson(
            s"Business Partner already has an active IOSS Subscription for this regime with error code ${EtmpEnrolmentErrorResponse.alreadyActiveSubscriptionErrorCode}" +
              s"with message body $body"
          )).toFuture
        case Left(error) =>
          auditService.audit(EtmpRegistrationRequestAuditModel.build(
            EtmpRegistrationAuditType.CreateRegistration, request.body, None, Some(error.body), SubmissionResult.Failure)
          )
          logger.error(s"Internal server error ${error.body}")
          InternalServerError(Json.toJson(s"Internal server error ${error.body}")).toFuture
      }
  }
}
