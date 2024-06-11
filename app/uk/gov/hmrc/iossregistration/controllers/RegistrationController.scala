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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.config.AppConfig
import uk.gov.hmrc.iossregistration.connectors.EnrolmentsConnector
import uk.gov.hmrc.iossregistration.controllers.actions.{AuthenticatedControllerComponents, AuthorisedMandatoryVrnRequest}
import uk.gov.hmrc.iossregistration.logging.Logging
import uk.gov.hmrc.iossregistration.models.audit.{EtmpAmendRegistrationRequestAuditModel, EtmpRegistrationAuditType, EtmpRegistrationRequestAuditModel, SubmissionResult}
import uk.gov.hmrc.iossregistration.models.etmp.amend.{AmendRegistrationResponse, EtmpAmendRegistrationRequest}
import uk.gov.hmrc.iossregistration.models.etmp.{EtmpEnrolmentErrorResponse, EtmpEnrolmentResponse, EtmpRegistrationRequest, EtmpRegistrationStatus}
import uk.gov.hmrc.iossregistration.models.{EtmpEnrolmentError, EtmpException, RegistrationStatus}
import uk.gov.hmrc.iossregistration.repositories.RegistrationStatusRepository
import uk.gov.hmrc.iossregistration.services.{AuditService, RegistrationService, RetryService}
import uk.gov.hmrc.iossregistration.utils.FutureSyntax.FutureOps
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Clock, LocalDateTime}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

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
    implicit request: AuthorisedMandatoryVrnRequest[EtmpRegistrationRequest] =>
      registrationService.createRegistration(request.body).flatMap {
        case Right(etmpEnrolmentResponse) =>
          enrollRegistration(etmpEnrolmentResponse.formBundleNumber).map { etmpRegistrationStatus =>
            auditRegistrationEvent(
              formBundleNumber = etmpEnrolmentResponse.formBundleNumber,
              etmpEnrolmentResponse = etmpEnrolmentResponse,
              etmpRegistrationStatus = etmpRegistrationStatus,
              successResponse = Created(Json.toJson(etmpEnrolmentResponse)))
          }
        case Left(EtmpEnrolmentError(EtmpEnrolmentErrorResponse.alreadyActiveSubscriptionErrorCode, body)) =>
          attemptFallbackEnrolment(request.vrn).getOrElse {
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
          }
        case Left(error) =>
          attemptFallbackEnrolment(request.vrn).getOrElse {
            auditService.audit(EtmpRegistrationRequestAuditModel.build(
              EtmpRegistrationAuditType.CreateRegistration, request.body, None, Some(error.body), SubmissionResult.Failure)
            )
            logger.error(s"Internal server error ${error.body}")
            InternalServerError(Json.toJson(s"Internal server error ${error.body}")).toFuture
          }
      }
  }

  private def attemptFallbackEnrolment(vrn: Vrn)
                                      (implicit hc: HeaderCarrier, request: AuthorisedMandatoryVrnRequest[EtmpRegistrationRequest]): Option[Future[Result]] = {
    if (appConfig.fallbackEnrolmentsEnable) {
      val fallbackEnrolments = appConfig.fallbackEnrolments

      logger.warn("Attempting fallback enrolment")

      fallbackEnrolments.find(_.vrn == vrn).map { fallbackEnrolment =>
        logger.info("Fallback enrolment found")
        enrollRegistration(fallbackEnrolment.formBundleNumber).map { etmpRegistrationStatus =>
          logger.info("Successfully enrolled via fallback enrolment")
          val fallbackEtmpEnrolmentResponse = EtmpEnrolmentResponse(
            processingDateTime = LocalDateTime.now(clock),
            formBundleNumber = fallbackEnrolment.formBundleNumber,
            vrn = vrn.vrn,
            iossReference = fallbackEnrolment.iossNumber,
            businessPartner = "unknown"
          )
          auditRegistrationEvent(
            formBundleNumber = fallbackEnrolment.formBundleNumber,
            etmpEnrolmentResponse = fallbackEtmpEnrolmentResponse,
            etmpRegistrationStatus = etmpRegistrationStatus,
            successResponse = Created(Json.toJson(fallbackEtmpEnrolmentResponse)))
        }
      }
    } else {
      None
    }
  }

  private def enrollRegistration(formBundleNumber: String)
                                (implicit hc: HeaderCarrier): Future[EtmpRegistrationStatus] = {
    (for {
      _ <- registrationStatusRepository.delete(formBundleNumber)
      _ <- registrationStatusRepository.insert(RegistrationStatus(subscriptionId = formBundleNumber,
        status = EtmpRegistrationStatus.Pending))
      enrolmentResponse <- enrolmentsConnector.confirmEnrolment(formBundleNumber)
    } yield {
      val enrolmentResponseStatus = enrolmentResponse.status
      enrolmentResponseStatus match {
        case NO_CONTENT =>
          retryService.getEtmpRegistrationStatus(appConfig.maxRetryCount, appConfig.delay, formBundleNumber)
        case status =>
          logger.error(s"Failed to add enrolment - $status with body ${enrolmentResponse.body}")
          throw EtmpException(s"Failed to add enrolment - ${enrolmentResponse.body}")
      }
    }).flatten
  }

  private def auditRegistrationEvent(formBundleNumber: String,
                                     etmpEnrolmentResponse: EtmpEnrolmentResponse,
                                     etmpRegistrationStatus: EtmpRegistrationStatus,
                                     successResponse: Result)
                                    (implicit hc: HeaderCarrier, request: AuthorisedMandatoryVrnRequest[EtmpRegistrationRequest]): Result = {
    etmpRegistrationStatus match {
      case EtmpRegistrationStatus.Success =>
        auditService.audit(EtmpRegistrationRequestAuditModel.build(
          EtmpRegistrationAuditType.CreateRegistration, request.body, Some(etmpEnrolmentResponse), None, SubmissionResult.Success)
        )
        logger.info("Successfully created registration and enrolment")
        successResponse
      case registrationStatus: EtmpRegistrationStatus =>
        logger.error(s"Failed to add enrolment, got registration status $registrationStatus")
        registrationStatusRepository.set(RegistrationStatus(subscriptionId = formBundleNumber, status = EtmpRegistrationStatus.Error))
        throw EtmpException(s"Failed to add enrolment, got registration status $registrationStatus")
    }
  }

  def get(): Action[AnyContent] = cc.authAndRequireIoss().async {
    implicit request =>
      getRegistrationAndReturnResult(request.iossNumber, request.vrn)
  }

  def getRegistration(iossNumber: String): Action[AnyContent] = cc.authAndRequireIoss().async {
    implicit request =>
      getRegistrationAndReturnResult(iossNumber, request.vrn)
  }

  private def getRegistrationAndReturnResult(iossNumber: String, vrn: Vrn)(implicit hc: HeaderCarrier): Future[Result] = {
    registrationService.get(iossNumber, vrn).map { registration =>
      Ok(Json.toJson(registration))
    }.recover {
      case exception =>
        logger.error(exception.getMessage, exception)
        InternalServerError(exception.getMessage)
    }
  }

  def amend(): Action[EtmpAmendRegistrationRequest] = cc.authAndRequireVat()(parse.json[EtmpAmendRegistrationRequest]).async {
    implicit request =>
      val etmpAmendRegistrationRequest: EtmpAmendRegistrationRequest = request.body
      registrationService
        .amendRegistration(etmpAmendRegistrationRequest)
        .flatMap {
          case Right(amendRegistrationResponse: AmendRegistrationResponse) =>
            def auditCall(): Unit = auditService.audit(EtmpAmendRegistrationRequestAuditModel.build(
              EtmpRegistrationAuditType.AmendRegistration,
              etmpAmendRegistrationRequest,
              None,
              None,
              SubmissionResult.Success
            ))

            if (etmpAmendRegistrationRequest.changeLog.reRegistration) {
              enrollRegistration(amendRegistrationResponse.formBundleNumber).map {
                case EtmpRegistrationStatus.Success =>
                  auditCall()
                  Ok(Json.toJson(amendRegistrationResponse))
                case registrationStatus =>
                  logger.error(s"Failed to add enrolment, got registration status $registrationStatus")
                  registrationStatusRepository.set(
                    RegistrationStatus(subscriptionId = amendRegistrationResponse.formBundleNumber, status = EtmpRegistrationStatus.Error)
                  )
                  throw EtmpException(s"Failed to add enrolment, got registration status $registrationStatus")
              }
            } else {
              auditCall()
              Future.successful(Ok(Json.toJson(amendRegistrationResponse)))
            }

          case Left(_) =>
            auditService.audit(EtmpAmendRegistrationRequestAuditModel.build(
              EtmpRegistrationAuditType.AmendRegistration,
              etmpAmendRegistrationRequest,
              None,
              None,
              SubmissionResult.Failure
            ))
            Future.successful(InternalServerError(Json.toJson(s"Internal server error when amending")))
        }
  }

  def getAccounts: Action[AnyContent] = cc.authAndRequireIoss().async {
    implicit request =>
      enrolmentsConnector.es2(request.credentials.providerId).map {
        case Right(enrolments) => Ok(Json.toJson(enrolments))
        case Left(e) => InternalServerError(e.body)
      }
  }

  def getAccountsForCredId(credId: String): Action[AnyContent] = Action.async {
    implicit request =>
      enrolmentsConnector.es2(credId).map {
        case Right(enrolments) => Ok(Json.toJson(enrolments))
        case Left(e) => InternalServerError(e.body)
      }
  }
}
