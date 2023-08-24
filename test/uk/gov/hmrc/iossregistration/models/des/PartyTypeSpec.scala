package uk.gov.hmrc.iossregistration.models.des

import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsString, JsSuccess}
import uk.gov.hmrc.iossregistration.models.des.PartyType.{OtherPartyType, VatGroup}

class PartyTypeSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "PartyType" - {

    "must deserialise from the string `Z2` to VatGroup" in {

      JsString("Z2").validate[PartyType] mustBe JsSuccess(VatGroup)
    }

    "must deserialise from any string other than `Z2` to `OtherPartyType`" in {

      forAll(arbitrary[String]) {
        value =>
          whenever(value != "Z2") {
            JsString(value).validate[PartyType] mustBe JsSuccess(OtherPartyType)
          }
      }
    }
  }
}
