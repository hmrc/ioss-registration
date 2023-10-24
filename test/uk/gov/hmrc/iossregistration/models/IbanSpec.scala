package uk.gov.hmrc.iossregistration.models

import org.scalatest.EitherValues
import play.api.libs.json.{JsError, JsString, JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

class IbanSpec extends BaseSpec with EitherValues {

  ".apply" - {

    "must create an Iban given known valid inputs" in {

      val inputs = Set(
        "GB94BARC10201530093459",
        "GB33BUKB20201555555555",
        "DE29100100100987654321",
        "GB24BKEN10000031510604",
        "GB27BOFI90212729823529",
        "GB17BOFS80055100813796",
        "GB92BARC20005275849855",
        "GB66CITI18500812098709",
        "GB15CLYD82663220400952",
        "GB26MIDL40051512345674",
        "GB76LOYD30949301273801",
        "GB25NWBK60080600724890",
        "GB60NAIA07011610909132",
        "GB29RBOS83040210126939",
        "GB79ABBY09012603367219",
        "GB21SCBL60910417068859",
        "GB42CPBK08005470328725"
      )

      for (input <- inputs) {
        Iban(input).value.toString mustBe input
      }
    }

    "must not create an Iban from inputs in the wrong format" in {
      val inputs = Set(
        "G94BARC10201530093459",
        "GB33BUKB20153"
      )

      for (input <- inputs) {
        Iban(input) mustBe Left(IbanError.InvalidFormat)
      }
    }

    "must not create an Iban from inputs with an incorrect checksum" in {
      val inputs = Set(
        "GB01BARC20714583608387",
        "GB00HLFX11016111455365"
      )

      for (input <- inputs) {
        Iban(input) mustBe Left(IbanError.InvalidChecksum)
      }
    }
  }

  "must serialise and deserialise to / from an Iban" in {

    val iban = Iban("GB94BARC10201530093459").value
    val json = JsString("GB94BARC10201530093459")

    Json.toJson(iban) mustBe json
    json.validate[Iban] mustBe JsSuccess(iban)
  }

  "must return JsError when reading invalid IBAN format" in {

    val json = JsString("G294BARC10201530093459")

    json.validate[Iban] mustBe JsError("IBAN is not in the correct format")
  }

  "must return JsError when reading invalid IBAN checksum" in {

    val json = JsString("GB00BARC10201530093459")

    json.validate[Iban] mustBe JsError("Invalid checksum")
  }

  "must return JsError when reading invalid IBAN json" in {

    val json = Json.obj("something" -> "GB00BARC10201530093459")

    json.validate[Iban] mustBe JsError("IBAN is not in the correct format")
  }
}
