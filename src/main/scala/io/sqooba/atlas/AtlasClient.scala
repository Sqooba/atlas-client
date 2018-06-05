package io.sqooba.atlas

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global

import com.typesafe.scalalogging.Logger
import dispatch._
import io.sqooba.atlas.model._
import io.sqooba.conf.SqConf
import org.json4s.{DefaultFormats, Formats, JValue}
import org.json4s.JsonAST.JNothing
import org.json4s.ext.EnumNameSerializer
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write

class AtlasClient(client: AtlasClientWrapper, val atlasBaseUrl: String) {

  def this() = this(new AtlasClientWrapper(), new SqConf().getString("atlas.baseUrl"))
  implicit val jsonFormats: Formats = DefaultFormats + new EnumNameSerializer(AtlasStatus)

  val logger = Logger(this.getClass)
  val dslSearchUrl = s"$atlasBaseUrl/api/atlas/v2/search/dsl"
  val basicSearchUrl = s"$atlasBaseUrl/api/atlas/v2/search/basic"
  val entityUrl = s"$atlasBaseUrl/api/atlas/v2/entity"
  val typeDefUrl = s"$atlasBaseUrl/api/atlas/types"

  def ccToMap(cc: AnyRef): Map[String, Any] = (Map[String, Any]() /: cc.getClass.getDeclaredFields) {
    (a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(cc))
  }

  def getTypeDefinitions(typeName: String): Future[Option[List[AtlasTypeDefinition]]] = {
    val req = url(s"$typeDefUrl/$typeName").GET
    client.queryAtlas(req).map {
      case Some(jsonRes) => {
        val typeDef = jsonRes \ "definition" \ "classTypes"
        Some(typeDef.extract[List[AtlasTypeDefinition]])
      }
      case None => None
    }
  }

  def saveTypeDefinition(typeDef: AtlasTypeDefinition): Future[Boolean] = {

    val typeDefs = AtlasTypeDefinitions(classTypes = Seq(typeDef))
    val initialReq = url(typeDefUrl).setBody(write(typeDefs))
    getTypeDefinitions(typeDef.typeName).map {
      case Some(x) => {
        logger.info(s"TypeDefinition found for ${typeDef.typeName}, updating.")
        initialReq.PUT
      }
      case None => {
        logger.info(s"No TypeDefinition found for ${typeDef.typeName}, creating new.")
        initialReq.POST
      }
    }.flatMap(req => {
      client.queryAtlas(req).map {
        case Some(res) => true
        case None => false
      }
    })
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

  def dslSearchEntity(dslQuery: String): Future[Option[AtlasEntity]] = {
    val req = url(dslSearchUrl).GET <<? Map("query" -> dslQuery)

    client.queryAtlas(req).map {
      case Some(jsonRes) => {
        val entitiesJson = (jsonRes \ "entities")
        val entities: List[AtlasEntity] = entitiesJson.extract[List[AtlasEntity]]
        if (entities.length >= 1) Some(entities.head) else None
      }
      case _ => None
    }
  }

  def dslSearchEntity(typeName: String, dslQuery: String): Future[Option[AtlasEntity]] = {
    val req = url(dslSearchUrl).GET <<? Map("query" -> s"$typeName where $dslQuery")

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

  def basicSearchEntity(typeName: String, dslQuery: String): Future[Option[AtlasEntity]] = {
    val req = url(basicSearchUrl).GET <<? Map("typeName" -> typeName, "query" -> dslQuery)

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

  def dslSearchEntities(typeName: String, dslQuery: String): Future[SearchResult] = {
    val req = url(dslSearchUrl).GET <<? Map("query" -> s"$typeName where $dslQuery")
    client.queryAtlas(req).map {
      case Some(jsonRes) => jsonRes.extract[SearchResult]
      case None => {
        logger.error("Invalid response from Atlas, returning empty SearchResult")
        SearchResult(typeName, dslQuery, List())
      }
    }
  }

  def basicSearchEntities(typeName: String, dslQuery: String): Future[SearchResult] = {
    val req = url(basicSearchUrl).GET <<? Map("typeName" -> typeName, "query" -> dslQuery)
    client.queryAtlas(req).map {
      case Some(jsonRes) => jsonRes.extract[SearchResult]
      case None => {
        logger.error("Invalid response from Atlas, returning empty SearchResult")
        SearchResult(typeName, dslQuery, List())
      }
    }
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

  val logger = Logger(this.getClass)
  def this() = this(Http.withConfiguration(_.setConnectTimeout(AtlasClientWrapper.timeoutInMs)
    .setRequestTimeout(AtlasClientWrapper.timeoutInMs)))

  def queryAtlas(req: Req): Future[Option[JValue]] = {
    val queryWithHeaders = req.setHeader("Content-Type", "application/json").as_!(AtlasClientWrapper.username,
      AtlasClientWrapper.password)
    client(queryWithHeaders).map(res => {
      res.getStatusCode match {
        case 200 | 201 => {
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

object AtlasClientWrapper {
  val config = new SqConf(Some("atlas-credentials.conf"))
  val username: String = config.getString("atlas.username")
  val password: String = config.getString("atlas.password")
  val timeoutInMs: Int = new SqConf().getInt("atlasClient.timeoutInMs")
}
