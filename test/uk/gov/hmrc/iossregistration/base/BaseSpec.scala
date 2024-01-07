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
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.iossregistration.controllers.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.iossregistration.generators.Generators
import uk.gov.hmrc.iossregistration.models.amend.EtmpAmendRegistrationChangeLog
import uk.gov.hmrc.iossregistration.models.des.VatCustomerInfo
import uk.gov.hmrc.iossregistration.models.{Bic, DesAddress, Iban}
import uk.gov.hmrc.iossregistration.models.etmp.{EtmpAdministration, EtmpAmendRegistrationRequest, EtmpBankDetails, EtmpCustomerIdentification, EtmpEuRegistrationDetails, EtmpMessageType, EtmpPreviousEuRegistrationDetails, EtmpRegistrationRequest, EtmpSchemeDetails, EtmpTradingName, EtmpWebsite, SchemeType, VatNumberTraderId}

import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDate, LocalDateTime, ZoneId}
import java.util.Locale

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

  protected val iossNumber: String = "IM9001234567"

  val stubClock: Clock = Clock.fixed(LocalDate.now.atStartOfDay(ZoneId.systemDefault).toInstant, ZoneId.systemDefault)

  protected def applicationBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(bind[AuthAction].to[FakeAuthAction])

  val userId: String = "12345-userId"

  val vatCustomerInfo: VatCustomerInfo =
    VatCustomerInfo(
      registrationDate = Some(LocalDate.now(stubClock)),
      desAddress = DesAddress("Line 1", None, None, None, None, Some("AA11 1AA"), "GB"),
      partOfVatGroup = false,
      organisationName = Some("Company name"),
      singleMarketIndicator = true,
      individualName = None,
      deregistrationDecisionDate = Some(LocalDate.now(stubClock)),
      overseasIndicator = false
    )

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    .withLocale(Locale.UK)
    .withZone(ZoneId.of("GMT"))

  val registrationRequest: EtmpRegistrationRequest = EtmpRegistrationRequest(
    administration = EtmpAdministration(EtmpMessageType.IOSSSubscriptionCreate),
    customerIdentification = EtmpCustomerIdentification(vrn),
    tradingNames = Seq(
      EtmpTradingName(tradingName = "Some Trading Name"),
      EtmpTradingName(tradingName = "Some Other Trading Name")
    ),
    schemeDetails = EtmpSchemeDetails(
      commencementDate = LocalDateTime.now().format(dateFormatter),
      euRegistrationDetails = Seq(
        EtmpEuRegistrationDetails(
          countryOfRegistration = "DE",
          traderId = VatNumberTraderId(vatNumber = "DE123456789"),
          tradingName = "Some Trading Name",
          fixedEstablishmentAddressLine1 = "Line 1",
          fixedEstablishmentAddressLine2 = Some("Line 2"),
          townOrCity = "Town",
          regionOrState = Some("Region"),
          postcode = Some("AB12 3CD")
        )
      ),
      previousEURegistrationDetails = Seq(
        EtmpPreviousEuRegistrationDetails(
          issuedBy = "DE",
          registrationNumber = "DE123",
          schemeType = SchemeType.IOSSWithIntermediary,
          intermediaryNumber = Some("IM123456789")
        )
      ),
      websites = Seq(
        EtmpWebsite(websiteAddress = "www.example-one.co.uk"),
        EtmpWebsite(websiteAddress = "www.example-two.co.uk")
      ),
      contactName = "Mr Test",
      businessTelephoneNumber = "0123 456789",
      businessEmailId = "mrtest@example.co.uk",
      nonCompliantReturns = Some("1"),
      nonCompliantPayments = Some("2")
    ),
    bankDetails = EtmpBankDetails(
      accountName = "Mr Test",
      bic = Some(Bic("ABCDEF2A").get),
      iban = Iban("GB33BUKB20201555555555").toOption.get
    )
  )

  val etmpAmendRegistrationRequest: EtmpAmendRegistrationRequest = EtmpAmendRegistrationRequest(
    administration = registrationRequest.administration.copy(messageType = EtmpMessageType.IOSSSubscriptionAmend),
    changeLog = EtmpAmendRegistrationChangeLog(
      tradingNames = true,
      fixedEstablishments = true,
      contactDetails = true,
      bankDetails = true,
      reRegistration = false
    ),
    customerIdentification = registrationRequest.customerIdentification,
    tradingNames = registrationRequest.tradingNames,
    schemeDetails = registrationRequest.schemeDetails,
    bankDetails = registrationRequest.bankDetails
  )
}


