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

import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.iossregistration.logging.Logging
import uk.gov.hmrc.iossregistration.services.AccountService
import uk.gov.hmrc.iossregistration.utils.FutureSyntax.FutureOps
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait AuthAction extends ActionBuilder[AuthorisedRequest, AnyContent] with ActionFunction[Request, AuthorisedRequest]

class AuthActionImpl @Inject()(
                                override val authConnector: AuthConnector,
                                val parser: BodyParsers.Default,
                                accountService: AccountService
                              )
                              (implicit val executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions with Logging {

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised(
      AuthProviders(AuthProvider.GovernmentGateway) and
        (AffinityGroup.Individual or AffinityGroup.Organisation) and
        CredentialStrength(CredentialStrength.strong)
    ).retrieve(
      Retrievals.credentials and
      Retrievals.internalId and
        Retrievals.allEnrolments and
        Retrievals.affinityGroup and
        Retrievals.confidenceLevel and
        Retrievals.credentialRole) {

      case Some(credentials) ~ Some(internalId) ~ enrolments ~ Some(Organisation) ~ _ ~ Some(credentialRole) if credentialRole == User =>
        val maybeVrn = findVrnFromEnrolments(enrolments)
        val futureMaybeIossNumber = findIossFromEnrolments(enrolments, internalId)

        for {
          maybeIossNumber <- futureMaybeIossNumber
          result <- block(AuthorisedRequest(request, credentials, internalId, maybeVrn, maybeIossNumber))
        } yield result

      case _ ~ _ ~ _ ~ Some(Organisation) ~ _ ~ Some(credentialRole) if credentialRole == Assistant =>
        throw UnsupportedCredentialRole("Unsupported credential role")

      case Some(credentials) ~ Some(internalId) ~ enrolments ~ Some(Individual) ~ confidence ~ _ =>
        val maybeVrn = findVrnFromEnrolments(enrolments)
        if (confidence >= ConfidenceLevel.L200) {
          val futureMaybeIossNumber = findIossFromEnrolments(enrolments, internalId)
          for {
            maybeIossNumber <- futureMaybeIossNumber
            result <- block(AuthorisedRequest(request, credentials, internalId, maybeVrn, maybeIossNumber))
          } yield result
        } else {
          throw InsufficientConfidenceLevel("Insufficient confidence level")
        }
      case _ =>
        throw new UnauthorizedException("Unable to retrieve authorisation data")
    } recover {
      case e: AuthorisationException =>
        logger.info(e.getMessage, e)
        Unauthorized
    }
  }

  private def findVrnFromEnrolments(enrolments: Enrolments): Option[Vrn] =
    enrolments.enrolments.find(_.key == "HMRC-MTD-VAT")
      .flatMap {
        enrolment =>
          enrolment.identifiers.find(_.key == "VRN").map(e => Vrn(e.value))
      } orElse enrolments.enrolments.find(_.key == "HMCE-VATDEC-ORG")
      .flatMap {
        enrolment =>
          enrolment.identifiers.find(_.key == "VATRegNo").map(e => Vrn(e.value))
      }

  private def findIossFromEnrolments(enrolments: Enrolments, internalId: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    enrolments.enrolments.find(_.key == "HMRC-IOSS-ORG")
      .flatMap {
        enrolment =>
          enrolment.identifiers.find(_.key == "IOSSNumber").map(_.value)
      } match {
      case Some(_) =>
        accountService.getLatestAccount(internalId)
      case a => a.toFuture
    }
}
