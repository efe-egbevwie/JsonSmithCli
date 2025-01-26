package languageParsers

import targetLanguages.TargetLanguage.Go.GoConfigOptions
import util.capitalize
import util.toClassNameCamelCase
import kotlinx.serialization.json.*

fun parseJsonToGoStruct(
    json: String,
    structName: String = "JsonClass",
    parsedStructs: LinkedHashMap<String, String> = LinkedHashMap(),
    goConfig: GoConfigOptions
): ParsedType? {
    val trimmedJson = json.trim()
    return try {
        when (val jsonElement = Json.parseToJsonElement(trimmedJson)) {
            is JsonObject -> {
                parseGoJsonObject(
                    jsonObject = jsonElement,
                    structName = structName,
                    parsedStructs = parsedStructs,
                    config = goConfig
                )
            }

            is JsonArray -> parseGoJsonArray(
                jsonArray = jsonElement,
                structName = structName,
                parsedStructs = parsedStructs,
                config = goConfig
            )


            else -> null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

}


private fun parseGoJsonObject(
    jsonObject: JsonObject,
    structName: String = "JsonClass",
    parsedStructs: LinkedHashMap<String, String> = LinkedHashMap(),
    config: GoConfigOptions
): ParsedType {
    val members = mutableListOf<GoStructMember>()
    jsonObject.entries.forEach { (key, value) ->
        val type = determineType(
            jsonElement = value,
            elementName = key.capitalize().toClassNameCamelCase(),
            parsedStructs = parsedStructs,
            config = config
        )
        val jsonTag = """`json:"$key"`"""
        val memberName = key.capitalize().toClassNameCamelCase()
        members.add(
            GoStructMember(
                memberName = memberName,
                dataType = type,
                jsonTag = jsonTag
            )
        )
    }

    val longestMemberLength = members.maxBy { it.memberName.length }.memberName.length
    val longestMemberTypeLength = members.maxBy { it.dataType.length }.dataType.length

    val goStruct = buildString {
        appendLine("type $structName struct {")
        members.forEach { goStructMember ->
            appendLine("    ${goStructMember.memberName.padEnd(longestMemberLength, ' ')} ${goStructMember.dataType.padEnd(longestMemberTypeLength, ' ')} ${goStructMember.jsonTag}")
        }
        appendLine("}")
    }

    parsedStructs[structName] = goStruct

    val stringRepresentation = buildString {
        appendLine(parsedStructs.values.reversed().joinToString("\n"))
    }
    val finalParsedStructs = parsedStructs.entries.reversed().map { parsedStruct ->
        ParsedClass(
            className = parsedStruct.key,
            classBody = parsedStruct.value
        )
    }
    return ParsedType(
        fileName = structName,
        parsedClasses = finalParsedStructs,
        stringRepresentation = stringRepresentation,
    )
}



private fun parseGoJsonArray(
    jsonArray: JsonArray,
    structName: String,
    parsedStructs: LinkedHashMap<String, String>,
    config: GoConfigOptions
): ParsedType {
    return parseGoJsonObject(
        jsonObject = jsonArray.first().jsonObject,
        structName = structName,
        parsedStructs = parsedStructs,
        config = config
    )

}


private fun determineType(
    jsonElement: JsonElement,
    elementName: String,
    parsedStructs: LinkedHashMap<String, String>,
    config: GoConfigOptions
): String {
    return when {
        jsonElement is JsonPrimitive && jsonElement.isString -> "string"
        jsonElement is JsonPrimitive && jsonElement.booleanOrNull != null -> "bool"
        jsonElement is JsonPrimitive && jsonElement.intOrNull != null -> "int64"
        jsonElement is JsonPrimitive && jsonElement.longOrNull != null -> "int64"
        jsonElement is JsonPrimitive && jsonElement.floatOrNull != null -> "float64"
        jsonElement is JsonObject -> {
            parseGoJsonObject(
                jsonObject = jsonElement,
                structName = elementName,
                parsedStructs = parsedStructs,
                config = config
            )
            elementName
        }

        jsonElement is JsonArray -> {
            val firstElement = jsonElement.first().jsonObject
            parseGoJsonObject(
                jsonObject = firstElement,
                structName = elementName,
                parsedStructs = parsedStructs,
                config = config
            )
            "[]$elementName"
        }

        else -> "interface{}"
    }
}

private data class GoStructMember(
    val memberName: String,
    val dataType: String,
    val jsonTag: String
)
