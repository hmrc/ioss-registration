package uk.gov.hmrc.iossregistration.models

import play.api.libs.json.{JsError, JsString, JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

class BicSpec extends BaseSpec {

  ".apply" - {

    "must create a BIC from a valid input" in {

      Bic("ABCDEF2A") mustBe defined
      Bic("ABCDEF99") mustBe defined
      Bic("ZZZZGBN1234") mustBe defined
    }

    "must create a BIC from strings with spaces" in {

      Bic("AB CD EF 2A ") mustBe defined
    }

    "must convert lowercase letters to uppercase" in {

      Bic("abcdef2a").value.toString mustEqual "ABCDEF2A"
    }

    "must not create a BIC" - {

      "with either 0 or 1 in position 7" in {

        Bic("ABCDEF0A") must not be defined
        Bic("ABCDEF1A") must not be defined
      }

      "with letter O in the 8th position" in {

        Bic("ABCDEF2O") must not be defined
      }
    }
  }

  "must serialise and deserialise" in {

    val json = JsString("ABCDEF2A")
    val bic = Bic("ABCDEF2A").value

    Json.toJson(bic) mustEqual json
    json.validate[Bic] mustEqual JsSuccess(bic)
  }

  "must return JsError when BIC is in invalid format" in {

    val json = JsString("A1CDEF2A")

    json.validate[Bic] mustEqual JsError("BIC is not in the correct format")
  }

  "must return JsError for invalid json" in {

    val json = Json.obj("test" -> "ABCDEF2A")

    json.validate[Bic] mustEqual JsError("BIC is not in the correct format")
  }
}
