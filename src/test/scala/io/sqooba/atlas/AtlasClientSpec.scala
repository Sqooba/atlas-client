package io.sqooba.atlas

import java.util.{Date, UUID}

import scala.concurrent.Future

import dispatch.Req
import io.sqooba.atlas.model.{AtlasEntity, AtlasStatus}
import org.json4s.jackson.JsonMethods._
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.mockito.MockitoSugar

class AtlasClientSpec extends AsyncFlatSpec with Matchers with MockitoSugar with BeforeAndAfter {

  val clientWrapper: AtlasClientWrapper = mock[AtlasClientWrapper]
  val atlasClient: AtlasClient = new AtlasClient(clientWrapper)

  val createResponseJsonAsString: String = """ {
    "mutatedEntities":{
      "CREATE":[{
        "typeName":"kafka_topic",
        "attributes":{
          "qualifiedName":"testTopic-1507817463271",
          "topic":"testTopic-1507817463271"
        },
        "guid":"c4c2460d-a8bf-4f1b-9466-3a25d9889774",
        "status":"ACTIVE"
      }]
    },
    "guidAssignments":{
       "-2584647665032095":"c4c2460d-a8bf-4f1b-9466-3a25d9889774"
    }
  } """.stripMargin

  val findByUuidAsString: String = """ {
    "referredEntities":{},
    "entity":{
      "typeName":"kafka_topic",
      "attributes":{
        "owner":null,
        "qualifiedName":"/tmp/testpath/1504876586550.txt",
        "name":"test_topic_1",
        "description":null,
        "topic":"test_topic_1",
        "uri":"this is a uri"
      },
      "guid":"c18978aa-2699-4865-b23d-7a6ef6e87892",
      "status":"ACTIVE",
      "createdBy":"pkettune",
      "updatedBy":"pkettune",
      "createTime":1507647212387,
      "updateTime":1507647212387,
      "version":0,
      "classifications":[]
    }
  } """.stripMargin

  val searchResultJsonString: String = """ {
    "queryType":"DSL",
    "queryText":"`kafka_topic` topic=\"test_topic_1\" ",
    "entities":[{
        "typeName":"kafka_topic",
        "attributes":{
          "owner":null,
          "qualifiedName":"/tmp/testpath/1504876586550.txt",
          "name":"test_topic_1",
          "topic":"test_topic_1",
          "description":null
      },
      "guid":"c18978aa-2699-4865-b23d-7a6ef6e87892",
      "status":"ACTIVE",
      "displayText":"test_topic_1",
      "classificationNames":[]
    }]
  } """.stripMargin

  // {"queryType":"DSL","queryText":"`kafka_topic` topic=\"this does not exist\" "}
  val createResponseJson = parse(createResponseJsonAsString, true)
  val findByUuidJson = parse(findByUuidAsString, true)
  val searchResultJson = parse(searchResultJsonString, true)

  "creating a new topic" should "update topic fields with fields from response" in {
    val ts = new Date().getTime
    val topicName = s"testTopic-$ts"
    val attr: Map[String, Any] = Map("name" -> topicName,
      "qualifiedName" -> topicName,
      "topic" -> topicName,
      "uri" -> s"uri:$topicName")

    when(clientWrapper.queryAtlas(any[Req])).thenReturn(Future.successful(Some(createResponseJson)))
    val atlasEntity = AtlasEntity("kafka_topic", attributes = attr)
    atlasClient.pushEntityToAtlas(atlasEntity).map(res => {
      res shouldBe defined
      res.get.guid shouldBe defined
      res.get.status shouldBe defined
      res.get.status.get shouldBe AtlasStatus.Active
    })
  }

  "creating a new topic" should "return None when fields are missing" in {
    val ts = new Date().getTime
    val topicName = s"testTopic-$ts"
    val attr: Map[String, Any] = Map("name" -> topicName,
      "qualifiedName" -> topicName,
      "uri" -> s"uri:$topicName")

    val atlasEntity = AtlasEntity("kafka_topic", attributes = attr)

    when(clientWrapper.queryAtlas(any[Req])).thenReturn(Future.successful(None))
    atlasClient.pushEntityToAtlas(atlasEntity).map(res => {
      res should not be defined
    })
  }

  "find kafka_topic via uuid" should "return valid AtlasEntity" in {
    when(clientWrapper.queryAtlas(any[Req])).thenReturn(Future.successful(Some(findByUuidJson)))
    atlasClient.findByUuid(UUID.randomUUID()).map(res => {
      res shouldBe defined
      res.get.status shouldBe defined
      res.get.status.get shouldBe AtlasStatus.Active
    })
  }

  "find kafka_topic via nonexistent uuid" should "return valid None" in {
    when(clientWrapper.queryAtlas(any[Req])).thenReturn(Future.successful(None))
    atlasClient.findByUuid(UUID.randomUUID()).map(res => {
      res should not be defined
    })
  }

  "find kafka_topic with dsl query" should "return valid item" in {
    when(clientWrapper.queryAtlas(any[Req])).thenReturn(Future.successful(Some(searchResultJson)))
    atlasClient.searchEntity("kafka_topic", "topic=topic").map(res => {
      res shouldBe defined
      res.get shouldBe a [AtlasEntity]
      res.get.status shouldBe defined
    })
  }

}
