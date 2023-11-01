package uk.gov.hmrc.iossregistration.testutils

import org.scalacheck.Gen
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.etmp._
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{__, OWrites}
import uk.gov.hmrc.iossregistration.models.binders.Format.dateTimeFormatter
import uk.gov.hmrc.iossregistration.models.Iban

import java.time.{LocalDate, LocalDateTime}

object DisplayRegistrationData extends BaseSpec {

  val iban: Iban = Iban("GB33BUKB20201555555555").toOption.get

  val arbitraryDisplayRegistration: EtmpDisplayRegistration =
    EtmpDisplayRegistration(
      tradingNames = Gen.listOfN(3, arbitraryEtmpTradingName.arbitrary).sample.value,
      schemeDetails = arbitraryEtmpSchemeDetails.arbitrary.sample.value,
      bankDetails = arbitraryEtmpBankDetails.arbitrary.sample.value,
      exclusions = Gen.listOfN(2, arbitraryEtmpExclusion.arbitrary.sample.value).sample.value,
      adminUse = arbitraryAdminUse.arbitrary.sample.value
    )

  val optionalDisplayRegistration: EtmpDisplayRegistration =
    EtmpDisplayRegistration(
      tradingNames = Seq.empty,
      schemeDetails = EtmpSchemeDetails(
        commencementDate = LocalDate.of(2023, 1, 1).format(dateTimeFormatter),
        euRegistrationDetails = Seq.empty,
        previousEURegistrationDetails = Seq.empty,
        websites = Seq.empty,
        contactName = "Mr Test",
        businessTelephoneNumber = "1234567890",
        businessEmailId = "test@testEmail.com",
        None,
        None
      ),
      bankDetails = EtmpBankDetails(
        accountName = "Bank Account Name",
        None,
        iban
      ),
      adminUse = EtmpAdminUse(Some(LocalDateTime.now(stubClock))),
      exclusions = Seq.empty
    )

  implicit val writesEtmpSchemeDetails: OWrites[EtmpSchemeDetails] = {
    (
      (__ \ "commencementDate").write[String] and
        (__ \ "euRegistrationDetails").write[Seq[EtmpEuRegistrationDetails]] and
        (__ \ "previousEURegistrationDetails").write[Seq[EtmpPreviousEuRegistrationDetails]] and
        (__ \ "websites").write[Seq[EtmpWebsite]] and
        (__ \ "contactDetails" \ "contactNameOrBusinessAddress").write[String] and
        (__ \ "contactDetails" \ "businessTelephoneNumber").write[String] and
        (__ \ "contactDetails" \ "businessEmailAddress").write[String] and
        (__ \ "nonCompliantReturns").writeNullable[String] and
        (__ \ "nonCompliantPayments").writeNullable[String]
      )(unlift(EtmpSchemeDetails.unapply))
  }

}
