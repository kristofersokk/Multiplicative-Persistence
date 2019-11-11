package com.timotheteus.multpersistance

import org.jetbrains.exposed.dao.IntIdTable

object Candidates : IntIdTable() {
    val value = text("value")
}

object NextCandidates : IntIdTable() {
    val candidate_id = entityId("candidate_id", Candidates).references(Candidates.id)
    val value = text("value")
}

object CollectMaps : IntIdTable() {
    val candidate_id = entityId("candidate_id", Candidates).references(Candidates.id)
    val map = varchar("map", 200)
}

object MapCandidates : IntIdTable() {
    val map_id = entityId("map_id", CollectMaps).references(CollectMaps.id)
    val value = text("value")
}
