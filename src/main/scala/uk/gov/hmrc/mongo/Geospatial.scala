/*
 * Copyright 2014 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.hmrc.mongo

import scala.concurrent.Future
import play.api.libs.json._
import scala.Tuple2
import scala.Some
import reactivemongo.api.QueryOpts
import play.api.libs.json.Json.JsValueWrapper
import reactivemongo.bson.BSONDocument


trait GeoJSON[A] {
  val `type` : String
  val coordinates : A
}

object GeoJSON {

  import play.api.libs.json._
  import play.api.libs.json.Reads._

  case class Point(coordinates : Tuple2[Double, Double], `type` : String = "Point") extends GeoJSON[Tuple2[Double, Double]]

  implicit val pointCoordinateFormat = TupleFormats.tuple2Format[Double, Double]

  implicit val pointFormat = Json.format[Point]
}

/*
 * The 2d index supports data stored as legacy coordinate pairs and is intended for use in MongoDB 2.2 and earlier.
 *
 * see http://docs.mongodb.org/manual/applications/geospatial-indexes/
 */
trait LegacyGeospatial[A, ID] {
  self: ReactiveRepository[A, ID] =>

  import reactivemongo.api.indexes.IndexType.Geo2D
  import reactivemongo.api.indexes.Index

  lazy val LocationField = "loc"

  override def ensureIndexes(): Future[_] = self.collection.indexesManager.ensure(Index(Seq((LocationField, Geo2D)), Some("geo2DIdx")))

  def near(lon: Double, lat: Double, limit: Int = 100) = {
    val s = Json.obj(LocationField -> Json.obj("$near" -> Json.arr(lon, lat)))
    collection.find(s).options(QueryOpts(batchSizeN = limit)).cursor[A].collect[List]()
  }
}