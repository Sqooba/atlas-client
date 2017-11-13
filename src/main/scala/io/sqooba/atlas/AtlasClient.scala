package io.sqooba.atlas

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import dispatch._
import io.sqooba.atlas.model.{AtlasEntity, AtlasStatus, SearchResult}
import io.sqooba.conf.SqConf
import org.json4s.{DefaultFormats, Formats, JValue}
import org.json4s.JsonAST.JNothing
import org.json4s.ext.EnumNameSerializer
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write

class AtlasClient(client: AtlasClientWrapper) {

  def this() = this(new AtlasClientWrapper())

  val config = new SqConf()

  implicit val jsonFormats: Formats = DefaultFormats + new EnumNameSerializer(AtlasStatus)

// val username: String = ConfigFactory.load("atlas-credentials.conf").getString("atlas.username")
// val password: String = ConfigFactory.load("atlas-credentials.conf").getString("atlas.password")

  val logger = Logger(this.getClass)
  val atlasBaseUrl = config.getString("atlas.baseUrl")
  val dslSearchUrl = s"$atlasBaseUrl/api/atlas/v2/search/dsl"
  val entityUrl = s"$atlasBaseUrl/api/atlas/v2/entity"

  def ccToMap(cc: AnyRef): Map[String, Any] = (Map[String, Any]() /: cc.getClass.getDeclaredFields) {
    (a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(cc))
  }

  def pushEntityToAtlas(entity: AtlasEntity): Future[Option[AtlasEntity]] = {
    val jsonBody = s"""{ "entity":${write(entity)}}"""
    val req = url(entityUrl).setBody(jsonBody).POST
    client.queryAtlas(req).map {
      case Some(json) => {
        val mutatedEntities = (json \ "mutatedEntities")
        val updated = mutatedEntities \ "UPDATE"
        val created = mutatedEntities \ "CREATE"
        logger.debug(s"Pushed entities: updated: ${updated.children.length}, created: ${created.children.length}")
        val source = if (updated != JNothing) updated else created
        val newEntity = source.extract[List[AtlasEntity]].head
        Some(entity.copy(status = newEntity.status, guid = newEntity.guid))
      }
      case _ => None
    }
  }

  def searchEntity(typeName: String, dslQuery: String): Future[Option[AtlasEntity]] = {
    val req = url(dslSearchUrl).GET <<? Map("typeName" -> typeName, "query" -> dslQuery)

    client.queryAtlas(req).map {
      case Some(jsonRes) => {
        // jsonRes.extract[SearchResult]
        val entitiesJson = (jsonRes \ "entities")
        val entities: List[AtlasEntity] = entitiesJson.extract[List[AtlasEntity]]
        if (entities.length >= 1) Some(entities.head) else None
      }
      case _ => None
    }
  }

  // todo: handle errors!!!! -
  def searchEntities(typeName: String, dslQuery: String): Future[SearchResult] = {
    val req = url(dslSearchUrl).GET <<? Map("typeName" -> typeName, "query" -> dslQuery)
    client.queryAtlas(req).map(jsonRes => {
      jsonRes.get.extract[SearchResult]
    })
  }

  def findByUuid(guid: String): Future[Option[AtlasEntity]] = {
    val uuidUrl: String = s"$entityUrl/guid/${guid}"
    val req = url(uuidUrl).GET
    client.queryAtlas(req).map {
      case Some(res) => Some((res \ "entity").extract[AtlasEntity])
      case _ => None
    }
  }

  def findByUuid(guid: UUID): Future[Option[AtlasEntity]] = findByUuid(guid.toString)

  def doRequest(req: Req): Future[Option[JValue]] = client.queryAtlas(req)
}

class AtlasClientWrapper(client: Http) {

  val config = new SqConf("atlas-credentials.conf")
  val username: String = config.getString("atlas.username")
  val password: String = config.getString("atlas.password")

  val logger = Logger(this.getClass)
  def this() = this(Http.withConfiguration(_.setConnectTimeout(7500).setRequestTimeout(7500)))

  def queryAtlas(req: Req): Future[Option[JValue]] = {
    val queryWithHeaders = req.setHeader("Content-Type", "application/json").as_!(username, password)
    client(queryWithHeaders).map(res => {
      res.getStatusCode match {
        case 200 => {
          logger.debug(s"Ok response for: ${req.url}")
          Some(parse(res.getResponseBody, true))
        }
        case _ => {
          logger.error(s"Failed: ${res.getStatusCode} - ${res.getStatusText}: ${res.getResponseBody}")
          None
        }
      }
    })
  }
}
