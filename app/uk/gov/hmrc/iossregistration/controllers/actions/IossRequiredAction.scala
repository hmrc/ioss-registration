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

package uk.gov.hmrc.iossregistration.controllers.actions

import play.api.mvc.{ActionRefiner, Result}
import play.api.mvc.Results.Unauthorized
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.config.AppConfig
import uk.gov.hmrc.iossregistration.connectors.IntermediaryRegistrationConnector
import uk.gov.hmrc.iossregistration.logging.Logging
import uk.gov.hmrc.iossregistration.utils.FutureSyntax.FutureOps
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IossRequiredAction(
                          intermediaryRegistrationConnector: IntermediaryRegistrationConnector,
                          config: AppConfig,
                          maybeIossNumber: Option[String]
                        )(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[AuthorisedMandatoryVrnRequest, AuthorisedMandatoryIossRequest] with Logging {

  override protected def refine[A](request: AuthorisedMandatoryVrnRequest[A]): Future[Either[Result, AuthorisedMandatoryIossRequest[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    request.iossNumber match {
      case None =>
        if (config.intermediaryEnabled) {
          (request.intermediaryNumber, maybeIossNumber) match {
            case (Some(intermediaryNumber), Some(iossNumber)) =>
              intermediaryRegistrationConnector.get(intermediaryNumber).map { intermediaryRegistrationWrapper =>
                val availableIossNumbers = intermediaryRegistrationWrapper.etmpDisplayRegistration.clientDetails.map(_.clientIossID)
                if(availableIossNumbers.contains(iossNumber)) {
                  Right(AuthorisedMandatoryIossRequest(request.request, request.credentials, request.userId, request.vrn, iossNumber))
                } else {
                  logger.info(s"Intermediary ${intermediaryNumber} doesn't have access to ioss number $iossNumber")
                  Left(Unauthorized)
                }
              }
            case _ =>
              logger.info("insufficient IOSS enrolments")
              Left(Unauthorized).toFuture
          }
        } else {
          logger.info("Intermediary is disabled and there was no ioss number in the enrolments")
          Left(Unauthorized).toFuture
        }
      case Some(iossNumber) =>
        Right(AuthorisedMandatoryIossRequest(request.request, request.credentials, request.userId, request.vrn, iossNumber)).toFuture
    }
  }
}

class IossRequiredActionProvider @Inject()(
                                            intermediaryRegistrationConnector: IntermediaryRegistrationConnector,
                                            config: AppConfig
                                          )
                                          (implicit ec: ExecutionContext) {

  def apply(maybeIossNumber: Option[String]): IossRequiredAction =
    new IossRequiredAction(intermediaryRegistrationConnector, config, maybeIossNumber)
}

