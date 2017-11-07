package io.sqooba.atlas.model

case class AtlasEntity(typeName: String,
                       guid: Option[String] = None,
                       status: Option[AtlasStatus.AtlasStatus] = None,
                       classificationNames: List[Any] = List(),
                       attributes: Map[String, Any])

case class AtlasReferenceId(typeName: String, guid: String)
