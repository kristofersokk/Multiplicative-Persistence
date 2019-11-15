package com.timotheteus.multpersistance

import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File

@UseExperimental(UnstableDefault::class)
class JsonHandler {

    var jsonData: JsonData = JsonData()
    private var json = Json(JsonConfiguration.Default.copy(prettyPrint = false))
    private val file = File("computedData.json")

    init {
        if (file.exists()) {
            val data = file.readText()
            jsonData = Json.parse(JsonData.serializer(), data)
        } else {
            writeToFile()
        }
    }

    fun writeToFile() {
        file.writeText(json.stringify(JsonData.serializer(), jsonData))
    }
}
