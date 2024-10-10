package uk.gov.hmrc.iossregistration.models.etmp

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec

import java.time.LocalDateTime

class EtmpEnrolmentErrorResponseSpec extends BaseSpec {

  private val processingDate = LocalDateTime.now().toString
  private val errorCode = Gen.listOfN(3, Gen.numChar).map(_.mkString).sample.value
  private val errorMessage = arbitrary[String].sample.value

  "EtmpEnrolmentErrorResponse" - {

    "must serialise/deserialse to and from EtmpEnrolmentErrorResponse" in {

      val json = Json.obj(
        "errorDetail" -> Json.obj(
          "timestamp" -> processingDate,
          "errorCode" -> errorCode,
          "errorMessage" -> errorMessage
        )
      )

      val expectedResult: EtmpEnrolmentErrorResponse = EtmpEnrolmentErrorResponse(
        EtmpErrorDetail(
          timestamp = processingDate,
          errorCode = Some(errorCode),
          errorMessage = Some(errorMessage)
        )
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpEnrolmentErrorResponse] mustBe JsSuccess(expectedResult)
    }
  }
}

