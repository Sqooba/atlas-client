package io.sqooba.atlas

import io.sqooba.ExternalSpec
import io.sqooba.atlas.model._
import io.sqooba.conf.SqConf
import org.scalatest.{AsyncFlatSpec, Matchers}

class AtlasClientE2eSpec extends AsyncFlatSpec with Matchers {

  val aClient = new AtlasClient(new AtlasClientWrapper(), new SqConf().getString("atlas.baseUrl"))

  "dsl search entities" should "return SearchResult even if none are found" taggedAs (ExternalSpec) in {
    aClient.dslSearchEntities("kafka_topic", "name=\"toptoptop\"").map(res => {
      res.entities.length shouldBe 0
    })
  }

  "dsl search entities" should "return SearchResult when 1 is found" taggedAs (ExternalSpec) in {
    aClient.dslSearchEntities("kafka_topic", "name=\"kafka_metadata_example\"").map(res => {
      res.entities.length shouldBe 1
    })
  }

  "dsl search entity" should "return Some when nothing is found" taggedAs (ExternalSpec) in {
    aClient.dslSearchEntity("kafka_topic", "name=\"kafka_metadata_example\"").map(res => {
      res shouldBe defined
      res.get.typeName shouldBe "kafka_topic"
    })
  }

  "dsl search entity" should "return None when nothing is found" taggedAs (ExternalSpec) in {
    aClient.dslSearchEntity("kafka_topic", "name=\"toptoptop\"").map(res => {
      res should not be defined
    })
  }

  "basic search entity" should "return None when nothing is found" taggedAs (ExternalSpec) in {
    aClient.basicSearchEntity("kafka_topic", "name=\"toptoptop\"").map(res => {
      res should not be defined
    })
  }

  "basic search entity" should "return Some when entity is found" taggedAs (ExternalSpec) in {
    aClient.basicSearchEntity("kafka_topic", "name=\"kafka_metadata_example\"").map(res => {
      res shouldBe defined
      res.get.typeName shouldBe "kafka_topic"
    })
  }

  "basic search entities" should "return empty list when nothing is found" taggedAs (ExternalSpec) in {
    aClient.basicSearchEntities("kafka_topic", "name=\"toptoptop\"").map(res => {
      res.entities.length shouldBe 0
    })
  }

  "basic search entities" should "return list of entities when something is found" taggedAs (ExternalSpec) in {
    aClient.basicSearchEntities("kafka_topic", "name=\"kafka_metadata_example\"").map(res => {
      res.entities.length shouldBe 1
    })
  }

  "basic search entities" should "return long list of entities when many are found" taggedAs (ExternalSpec) in {
    aClient.basicSearchEntities("spark_application", "owner=\"pkettune\"").map(res => {
      res.entities.length should be > 10
    })
  }

  "get type definition" should "return type definition for given type" taggedAs (ExternalSpec) in {
    aClient.getTypeDefinitions("kafka_topic").map(res => {

      val head = res.get.head
      head.superTypes should contain ("DataSet")
      head.typeName shouldBe "kafka_topic"
      res shouldBe defined
    })
  }
  "create/update type definition" should "create new given type" taggedAs (ExternalSpec) in {

    val attr1 = AttributeDefinition("testAttr1", "string", "optional", false, true, false)
    val attr2 = AttributeDefinition("testAttr2", "string", "optional", false, true, false)
    val testType = AtlasTypeDefinition("testType5",
      "testType",
      "1.0",
      Seq(attr1, attr2),
      "metatypename yeah",
      Seq("DataSet"))

    aClient.saveTypeDefinition(testType).map(res => {
      res shouldBe true
    })
  }
}
