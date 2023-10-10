package uk.gov.hmrc.iossregistration.models

import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

class WebsiteSpec extends BaseSpec  {

  "Website" - {

    "must serialise/deserialise to and from Website" in {

      val website = arbitrary[Website].sample.value

      val expectedJson = Json.obj(
        "websiteAddress" -> s"${website.websiteAddress}"
      )

      Json.toJson(website) mustBe expectedJson
      expectedJson.validate[Website] mustBe JsSuccess(website)
    }
  }
}
