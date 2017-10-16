package io.sqooba.atlas.model

case class SearchResult(queryType: String, queryText: String, entities: List[AtlasEntity])

