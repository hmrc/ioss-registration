package uk.gov.hmrc.iossregistration.models.etmp

import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

class EtmpBankDetailsSpec extends BaseSpec {

  private val accountName = arbitrary[String].sample.value
  private val bic = arbitraryBic.arbitrary.sample.value
  private val iban = arbitraryIban.arbitrary.sample.value

  "must deserialise/serialise to and from EtmpBankDetails" - {

    "when all optional values are present" in {

      val json = Json.obj(
        "accountName" -> accountName,
        "bic" -> bic,
        "iban" -> iban
      )

      val expectedResult = EtmpBankDetails(
        accountName = accountName,
        bic = Some(bic),
        iban = iban
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpBankDetails] mustBe JsSuccess(expectedResult)
    }

    "when all optional values are absent" in {

      val json = Json.obj(
        "accountName" -> accountName,
        "iban" -> iban
      )

      val expectedResult = EtmpBankDetails(
        accountName = accountName,
        bic = None,
        iban = iban
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpBankDetails] mustBe JsSuccess(expectedResult)
    }
  }
}

