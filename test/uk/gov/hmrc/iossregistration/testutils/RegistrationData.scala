package uk.gov.hmrc.iossregistration.testutils

import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models._
import uk.gov.hmrc.iossregistration.models.core.Match.dateFormatter
import uk.gov.hmrc.iossregistration.models.etmp._

import java.time.LocalDate

object RegistrationData extends BaseSpec {

  val etmpEuRegistrationDetails: EtmpEuRegistrationDetails = EtmpEuRegistrationDetails(
    countryOfRegistration = arbitrary[Country].sample.value.code,
    traderId = arbitraryVatNumberTraderId.arbitrary.sample.value,
    tradingName = arbitraryEtmpTradingName.arbitrary.sample.value.tradingName,
    fixedEstablishmentAddressLine1 = arbitrary[String].sample.value,
    fixedEstablishmentAddressLine2 = Some(arbitrary[String].sample.value),
    townOrCity = arbitrary[String].sample.value,
    regionOrState = Some(arbitrary[String].sample.value),
    postcode = Some(arbitrary[String].sample.value)
  )

  val etmpEuPreviousRegistrationDetails: EtmpPreviousEuRegistrationDetails = EtmpPreviousEuRegistrationDetails(
    issuedBy = arbitrary[Country].sample.value.code,
    registrationNumber = arbitrary[String].sample.value,
    schemeType = arbitrary[SchemeType].sample.value,
    intermediaryNumber = Some(arbitrary[String].sample.value)
  )

  val etmpSchemeDetails: EtmpSchemeDetails = EtmpSchemeDetails(
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

  val genBankDetails: BankDetails = BankDetails(
    accountName = arbitrary[String].sample.value,
    bic = Some(arbitrary[Bic].sample.value),
    iban = arbitrary[Iban].sample.value
  )

  val etmpRegistrationRequest: EtmpRegistrationRequest = EtmpRegistrationRequest(
    administration = arbitrary[EtmpAdministration].sample.value,
    customerIdentification = arbitrary[EtmpCustomerIdentification].sample.value,
    tradingNames = Seq(arbitrary[EtmpTradingName].sample.value),
    schemeDetails = etmpSchemeDetails,
    bankDetails = genBankDetails
  )
}
