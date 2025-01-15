package uk.gov.hmrc.iossregistration.models.etmp.amend

import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec


class EtmpAmendCustomerIdentificationSpec extends BaseSpec with Matchers {

  "EtmpAmendCustomerIdentification" - {

    "must deserialise/serialise to and from EtmpAmendCustomerIdentification" in {

      val json = Json.obj(
        "iossNumber" -> iossNumber
      )

      val expectedResult = EtmpAmendCustomerIdentification(
        iossNumber = iossNumber
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpAmendCustomerIdentification] mustBe JsSuccess(expectedResult)

    }
  }
}
