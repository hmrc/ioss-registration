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

package uk.gov.hmrc.iossregistration.base

import org.mockito.MockitoSugar
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.iossregistration.controllers.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.iossregistration.generators.Generators
import uk.gov.hmrc.iossregistration.models.core.Match.dateFormatter
import uk.gov.hmrc.iossregistration.models.{BankDetails, Bic, Country, Iban, Website}
import uk.gov.hmrc.iossregistration.models.etmp.{EtmpEuRegistrationDetails, EtmpPreviousEuRegistrationDetails, EtmpSchemeDetails, SchemeType}

import java.time.{Clock, LocalDate, ZoneId}

trait BaseSpec
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with MockitoSugar
    with Generators {

  protected val vrn: Vrn = Vrn("123456789")

  val stubClock: Clock = Clock.fixed(LocalDate.now.atStartOfDay(ZoneId.systemDefault).toInstant, ZoneId.systemDefault)

  protected def applicationBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(bind[AuthAction].to[FakeAuthAction])

  val userId: String = "12345-userId"

  def etmpEuRegistrationDetails: EtmpEuRegistrationDetails = EtmpEuRegistrationDetails(
    countryOfRegistration = arbitrary[Country].sample.value.code,
    traderId = arbitraryVatNumberTraderId.arbitrary.sample.value,
    tradingName = arbitraryEtmpTradingName.arbitrary.sample.value.tradingName,
    fixedEstablishmentAddressLine1 = arbitrary[String].sample.value,
    fixedEstablishmentAddressLine2 = Some(arbitrary[String].sample.value),
    townOrCity = arbitrary[String].sample.value,
    regionOrState = Some(arbitrary[String].sample.value),
    postcode = Some(arbitrary[String].sample.value)
  )

  def etmpEuPreviousRegistrationDetails: EtmpPreviousEuRegistrationDetails = EtmpPreviousEuRegistrationDetails(
    issuedBy = arbitrary[Country].sample.value.code,
    registrationNumber = arbitrary[String].sample.value,
    schemeType = arbitrary[SchemeType].sample.value,
    intermediaryNumber = Some(arbitrary[String].sample.value)
  )

  def etmpSchemeDetails: EtmpSchemeDetails = EtmpSchemeDetails(
    commencementDate = LocalDate.now.format(dateFormatter),
    euRegistrationDetails = Seq(etmpEuRegistrationDetails),
    previousEURegistrationDetails = Seq(etmpEuPreviousRegistrationDetails),
    websites = Seq(arbitrary[Website].sample.value),
    contactName = arbitrary[String].sample.value,
    businessTelephoneNumber = arbitrary[String].sample.value,
    businessEmailId = arbitrary[String].sample.value,
    nonCompliantReturns = Some(arbitrary[String].sample.value),
    nonCompliantPayments = Some(arbitrary[String].sample.value)
  )

  def genBankDetails: BankDetails = BankDetails(
    accountName = arbitrary[String].sample.value,
    bic = Some(arbitrary[Bic].sample.value),
    iban = arbitrary[Iban].sample.value
  )
}


