package uk.gov.hmrc.iossregistration.testutils

import org.scalacheck.Gen
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{OWrites, __}
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.binders.Format.dateTimeFormatter
import uk.gov.hmrc.iossregistration.models.etmp.*
import uk.gov.hmrc.iossregistration.models.Iban
import uk.gov.hmrc.iossregistration.testutils.RegistrationData.arbitraryEtmpCustomerIdentificationNew

import java.time.{LocalDate, LocalDateTime}

object DisplayRegistrationData extends BaseSpec {

  val iban: Iban = Iban("GB33BUKB20201555555555").toOption.get

  val arbitraryDisplayRegistration: EtmpDisplayRegistration =
    EtmpDisplayRegistration(
      customerIdentification = arbitraryEtmpCustomerIdentificationNew.arbitrary.sample.value,
      tradingNames = Gen.listOfN(3, arbitraryEtmpTradingName.arbitrary).sample.value,
      schemeDetails = arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value,
      bankDetails = Some(arbitraryEtmpBankDetails.arbitrary.sample.value),
      otherAddress = None,
      exclusions = Gen.listOfN(2, arbitraryEtmpExclusion.arbitrary.sample.value).sample.value,
      adminUse = arbitraryAdminUse.arbitrary.sample.value
    )

  val optionalDisplayRegistration: EtmpDisplayRegistration =
    EtmpDisplayRegistration(
      customerIdentification = arbitraryEtmpCustomerIdentificationNew.arbitrary.sample.value,
      tradingNames = Seq.empty,
      schemeDetails = EtmpDisplaySchemeDetails(
        commencementDate = LocalDate.of(2023, 1, 1).format(dateTimeFormatter),
        euRegistrationDetails = Seq.empty,
        previousEURegistrationDetails = Seq.empty,
        websites = Seq.empty,
        contactName = "Mr Test",
        businessTelephoneNumber = "1234567890",
        businessEmailId = "test@testEmail.com",
        unusableStatus = false,
        None,
        None
      ),
      bankDetails = Some(EtmpBankDetails(
        accountName = "Bank Account Name",
        None,
        iban
      )),
      otherAddress = None,
      adminUse = EtmpAdminUse(Some(LocalDateTime.now(stubClock))),
      exclusions = Seq.empty
    )

  implicit val writesEtmpSchemeDetails: OWrites[EtmpDisplaySchemeDetails] = {
    (
      (__ \ "commencementDate").write[String] and
        (__ \ "euRegistrationDetails").write[Seq[EtmpDisplayEuRegistrationDetails]] and
        (__ \ "previousEURegistrationDetails").write[Seq[EtmpPreviousEuRegistrationDetails]] and
        (__ \ "websites").write[Seq[EtmpWebsite]] and
        (__ \ "contactDetails" \ "contactNameOrBusinessAddress").write[String] and
        (__ \ "contactDetails" \ "businessTelephoneNumber").write[String] and
        (__ \ "contactDetails" \ "businessEmailAddress").write[String] and
        (__ \ "contactDetails" \ "unusableStatus").write[Boolean] and
        (__ \ "nonCompliantReturns").writeNullable[String] and
        (__ \ "nonCompliantPayments").writeNullable[String]
      )(etmpDisplaySchemeDetails => Tuple.fromProductTyped(etmpDisplaySchemeDetails))
  }

}
