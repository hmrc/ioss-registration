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

package uk.gov.hmrc.iossregistration.services

import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.iossregistration.connectors.{GetVatInfoConnector, RegistrationConnector}
import uk.gov.hmrc.iossregistration.connectors.RegistrationHttpParser.CreateEtmpRegistrationResponse
import uk.gov.hmrc.iossregistration.logging.Logging
import uk.gov.hmrc.iossregistration.models.{EtmpException, RegistrationWrapper}
import uk.gov.hmrc.iossregistration.models.etmp.EtmpRegistrationRequest
import uk.gov.hmrc.iossregistration.models.etmp.amend.{AmendRegistrationResponse, EtmpAmendRegistrationRequest}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationService @Inject()(
                                     registrationConnector: RegistrationConnector,
                                     getVatInfoConnector: GetVatInfoConnector
                                   )(implicit ec: ExecutionContext) extends Logging {

  def createRegistration(etmpRegistrationRequest: EtmpRegistrationRequest): Future[CreateEtmpRegistrationResponse] =
    registrationConnector.createRegistration(etmpRegistrationRequest)

  def get(iossNumber: String, vrn: Vrn)(implicit headerCarrier: HeaderCarrier): Future[RegistrationWrapper] = {
    for {
      etmpDisplayRegistrationResponse <- registrationConnector.get(iossNumber)
      vatInfoResponse <- getVatInfoConnector.getVatCustomerDetails(vrn)
    } yield {
      (etmpDisplayRegistrationResponse, vatInfoResponse) match {
        case (Right(etmpDisplayRegistration), Right(vatInfo)) =>
          RegistrationWrapper(
            vatInfo,
            etmpDisplayRegistration
          )
        case (Left(displayError), Left(vatInfoError)) =>
          logger.error(s"There was an error getting Registration and vat info from ETMP: ${displayError.body} and ${vatInfoError.body}")
          throw EtmpException(s"There was an error getting Registration from ETMP: ${displayError.body} and ${vatInfoError.body}")
        case (Left(displayError), _) =>
          logger.error(s"There was an error getting Registration from ETMP: ${displayError.body}")
          throw EtmpException(s"There was an error getting Registration from ETMP: ${displayError.body}")
        case (_, Left(vatInfoError)) =>
          logger.error(s"There was an error getting vat info from ETMP: ${vatInfoError.body}")
          throw EtmpException(s"There was an error getting  vat info from ETMP: ${vatInfoError.body}")
      }
    }
  }

  def amendRegistration(etmpRegistrationRequest: EtmpAmendRegistrationRequest): Future[Either[Throwable, AmendRegistrationResponse]] = {
    registrationConnector.amendRegistration(etmpRegistrationRequest).flatMap {
      case Right(amendRegistrationResponse: AmendRegistrationResponse) =>
        logger.info(s"Successfully sent amend registration to ETMP at ${amendRegistrationResponse.processingDateTime} for vrn ${amendRegistrationResponse.vrn} and IOSS number ${amendRegistrationResponse.iossReference}")
        Future.successful(Right(amendRegistrationResponse))
      case Left(error) =>
        logger.error(s"An error occurred while amending registration ${error.getClass} ${error.body}")
        Future.successful(Left(EtmpException(s"There was an error amending Registration from ETMP: ${error.getClass} ${error.body}")))
    }
  }
}
