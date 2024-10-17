/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.iossregistration.repositories

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import play.api.libs.json.Format
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.iossregistration.config.AppConfig
import uk.gov.hmrc.iossregistration.crypto.SavedUserAnswersEncryptor
import uk.gov.hmrc.iossregistration.logging.Logging
import uk.gov.hmrc.iossregistration.models.{EncryptedSavedUserAnswers, LegacyEncryptedSavedUserAnswers, NewEncryptedSavedUserAnswers, SavedUserAnswers}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SaveForLaterRepository @Inject()(
                                        val mongoComponent: MongoComponent,
                                        encryptor: SavedUserAnswersEncryptor,
                                        appConfig: AppConfig
                                      )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[EncryptedSavedUserAnswers](
    collectionName = "saved-user-answers",
    mongoComponent = mongoComponent,
    domainFormat = EncryptedSavedUserAnswers.format,
    replaceIndexes = true,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("vrn"),
        IndexOptions()
          .name("userAnswersRefIndex")
          .unique(true)
      ),
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(appConfig.cacheTtl, TimeUnit.DAYS)
      )
    )
  ) with Logging {

  import uk.gov.hmrc.mongo.play.json.Codecs.toBson

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def byVrn(vrn: Vrn): Bson =
    Filters.equal("vrn", toBson(vrn))

  def set(savedUserAnswers: SavedUserAnswers): Future[SavedUserAnswers] = {

    val encryptedAnswers = encryptor.encryptAnswers(savedUserAnswers, savedUserAnswers.vrn)

    collection
      .replaceOne(
        filter = byVrn(savedUserAnswers.vrn),
        replacement = encryptedAnswers,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => savedUserAnswers)
  }

  def get(vrn: Vrn): Future[Option[SavedUserAnswers]] =
    collection
      .find(
        byVrn(vrn)
      ).headOption()
      .map(_.map {
        case l: LegacyEncryptedSavedUserAnswers =>
          encryptor.decryptLegacyAnswers(l, l.vrn)
        case n: NewEncryptedSavedUserAnswers =>
          encryptor.decryptAnswers(n, n.vrn)
      })

  def clear(vrn: Vrn): Future[Boolean] =
    collection
      .deleteOne(byVrn(vrn))
      .toFuture()
      .map(_ => true)
}

