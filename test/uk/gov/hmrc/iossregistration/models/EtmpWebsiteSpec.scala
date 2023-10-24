package uk.gov.hmrc.iossregistration.models

import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.etmp.EtmpWebsite

class EtmpWebsiteSpec extends BaseSpec  {

  "EtmpWebsite" - {

    "must serialise/deserialise to and from EtmpWebsite" in {

      val website = arbitrary[EtmpWebsite].sample.value

      val expectedJson = Json.obj(
        "websiteAddress" -> s"${website.websiteAddress}"
      )

      Json.toJson(website) mustBe expectedJson
      expectedJson.validate[EtmpWebsite] mustBe JsSuccess(website)
    }
  }
}
