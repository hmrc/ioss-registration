package uk.gov.hmrc.iossregistration.models.etmp

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.iossregistration.base.BaseSpec
import uk.gov.hmrc.iossregistration.models.binders.Format.eisDateTimeFormatter

import java.time.LocalDateTime

class EtmpEnrolmentErrorResponseSpec extends BaseSpec {

  private val processingDate = eisDateTimeFormatter.format(LocalDateTime.now())
  private val code = Gen.listOfN(3, Gen.numChar).map(_.mkString).sample.value
  private val text = arbitrary[String].sample.value

  "EtmpEnrolmentErrorResponse" - {

    "must serialise/deserialse to and from EtmpEnrolmentErrorResponse" in {

      val json = Json.obj(
        "errors" -> Json.obj(
          "processingDate" -> processingDate,
          "code" -> code,
          "text" -> text
        )
      )

      val expectedResult: EtmpEnrolmentErrorResponse = EtmpEnrolmentErrorResponse(
        EtmpErrorDetail(
          processingDate = processingDate,
          code = code,
          text = text
        )
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpEnrolmentErrorResponse] mustBe JsSuccess(expectedResult)
    }
  }
}

