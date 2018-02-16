package io.sqooba.atlas.model

case class AtlasEntity(typeName: String,
                       guid: Option[String] = None,
                       status: Option[AtlasStatus.AtlasStatus] = None,
                       classificationNames: List[Any] = List(),
                       attributes: Map[String, Any])

case class AtlasReferenceId(typeName: String, guid: String)

case class AtlasTypeDefinition(typeName: String,
                               typeDescription: String,
                               typeVersion: String,
                               attributeDefinitions: Seq[AttributeDefinition],
                               hierarchicalMetaTypeName: String,
                               superTypes: Seq[String])

case class AttributeDefinition(name: String,
                               dataTypeName: String,
                               multiplicity: String, // "required",
                               isComposite: Boolean,
                               isUnique: Boolean,
                               isIndexable: Boolean,
                               reverseAttributeName: Option[Boolean] = None,
                               defaultValue: Option[String] = None,
                               description: Option[String] = None)

case class TypeDefinitionQuery(enumTypes: Seq[AtlasTypeDefinition] = Seq(),
                               structTypes: Seq[AtlasTypeDefinition] = Seq(),
                               traitTypes: Seq[AtlasTypeDefinition] = Seq(),
                               classTypes: Seq[AtlasTypeDefinition] = Seq())