package uk.gov.hmrc.iossregistration.models.enrolments

import play.api.libs.json.{JsError, JsString, JsSuccess, JsValue}
import uk.gov.hmrc.iossregistration.base.BaseSpec

class EnrolmentStatusSpec extends BaseSpec {

  "EnrolmentStatus" - {

    "deserialize JSON to EnrolmentStatus.Success" in {
      val json: JsValue = JsString("SUCCEEDED")

      val result = json.validate[EnrolmentStatus]

      result mustBe JsSuccess(EnrolmentStatus.Success)
    }

    "deserialize JSON to EnrolmentStatus.Failure when value is ERROR" in {
      val json: JsValue = JsString("ERROR")

      val result = json.validate[EnrolmentStatus]

      result mustBe JsSuccess(EnrolmentStatus.Failure)
    }

    "deserialize JSON to EnrolmentStatus.Failure when value is Enrolled" in {
      val json: JsValue = JsString("Enrolled")

      val result = json.validate[EnrolmentStatus]

      result mustBe JsSuccess(EnrolmentStatus.Failure)
    }

    "deserialize JSON to EnrolmentStatus.Failure when value is EnrolmentError" in {
      val json: JsValue = JsString("EnrolmentError")

      val result = json.validate[EnrolmentStatus]

      result mustBe JsSuccess(EnrolmentStatus.Failure)
    }

    "deserialize JSON to EnrolmentStatus.Failure when value is AuthRefreshed" in {
      val json: JsValue = JsString("AuthRefreshed")

      val result = json.validate[EnrolmentStatus]

      result mustBe JsSuccess(EnrolmentStatus.Failure)
    }

    "fail to deserialize invalid JSON" in {
      val json: JsValue = JsString("InvalidStatus")

      val result = json.validate[EnrolmentStatus]

      result mustBe a[JsError]
      result.asInstanceOf[JsError].errors.head._2.head.message mustBe "Unable to parse json InvalidStatus"
    }
  }
}
