package languageParsers

import targetLanguages.TargetLanguage
import targetLanguages.TargetLanguage.Kotlin.KotlinConfigOptions
import targetLanguages.TargetLanguage.Kotlin.KotlinSerializationFrameWorks.*
import util.capitalize
import util.isSnakeCase
import util.toCamelCase
import kotlinx.serialization.json.*

fun parseJsonToKotlinClass(
    json: String,
    className: String = "JsonClass",
    kotlinConfig: KotlinConfigOptions,
    parsedClasses: LinkedHashMap<String, String> = LinkedHashMap()
): ParsedType? {
    val jsonTrimmed = json
    return try {
        when (val jsonElement = Json.parseToJsonElement(jsonTrimmed)) {
            is JsonObject -> parseJsonObject(
                jsonObject = jsonElement,
                className = className,
                kotlinConfig = kotlinConfig,
                parsedClasses = parsedClasses
            )

            is JsonArray -> parseJsonArray(
                jsonArray = jsonElement,
                className = className,
                kotlinConfig = kotlinConfig,
                parsedClasses = parsedClasses
            )

            else -> null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun parseJsonObject(
    jsonObject: JsonObject,
    className: String,
    kotlinConfig: KotlinConfigOptions,
    parsedClasses: LinkedHashMap<String, String>,
): ParsedType {
    val properties = mutableListOf<String>()
    val kotlinSerializationFramework =
        (kotlinConfig.serializationFrameWork) as TargetLanguage.Kotlin.KotlinSerializationFrameWorks

    jsonObject.entries.forEach { (key, value) ->
        val type = determineType(
            value = value,
            className = key.capitalize(),
            kotlinConfig = kotlinConfig,
            nestedClasses = parsedClasses
        )
        val propertyAnnotation =
            when (kotlinSerializationFramework) {
                Kotlinx -> "@SerialName(\"$key\")"
                Gson -> """@SerializedName("$key")"""
                Jackson -> """@JsonProperty("$key")"""
            }
        val property = buildString {
            if (key.isSnakeCase()) {
                append("    $propertyAnnotation\n    val ${key.toCamelCase()}: $type")
            } else {
                append("    val $key: $type")
            }
        }.let { baseProperty ->
            val nullNotation = if (kotlinConfig.allPropertiesOptional) "? = null" else ""
            "$baseProperty$nullNotation,"
        }

        properties.add(property)
    }

    val classAnnotation = when (kotlinConfig.serializationFrameWork) {
        Kotlinx -> "@Serializable"
        Gson -> null
        Jackson -> null
    }
    val currentClass = buildString {
        if (classAnnotation != null) {
            appendLine(classAnnotation)
        }
        appendLine("data class $className(")
        properties.forEach { property ->
            appendLine(property)
        }
        appendLine(")")
    }
    parsedClasses[className] = currentClass

    val imports = when (kotlinConfig.serializationFrameWork) {
        Kotlinx -> {
            buildString {
                appendLine("import kotlinx.serialization.Serializable")
                if (jsonObject.entries.any { it.key.isSnakeCase() }) {
                    appendLine("import kotlinx.serialization.SerialName")
                }
            }
        }

        Gson -> {
            buildString {
                if (
                    jsonObject.entries.any {
                        it.key.isSnakeCase()
                    }
                ) {
                    appendLine("import com.google.gson.annotations.SerializedName")
                }
            }
        }

        Jackson -> {
            buildString {
                if (jsonObject.entries.any { it.key.isSnakeCase() }) {
                    appendLine("import com.fasterxml.jackson.annotation.JsonProperty")
                }
            }
        }
    }


    val finalType = buildString {
        append(imports)
        appendLine("\n")
        appendLine(parsedClasses.values.reversed().joinToString("\n"))
    }

    val finalParsedClass = parsedClasses.entries.reversed().map {parsedClass ->
        ParsedClass(
            className = parsedClass.key,
            classBody = parsedClass.value,
        )
    }
    return ParsedType(
        fileName = className,
        imports = imports,
        parsedClasses = finalParsedClass,
        stringRepresentation = finalType
    )
}

private fun parseJsonArray(
    jsonArray: JsonArray,
    className: String,
    kotlinConfig: KotlinConfigOptions,
    parsedClasses: LinkedHashMap<String, String>
): ParsedType {
    return parseJsonObject(
        jsonObject = jsonArray.first().jsonObject,
        className = className,
        kotlinConfig = kotlinConfig,
        parsedClasses = parsedClasses
    )
}

private fun determineType(
    value: JsonElement,
    className: String,
    kotlinConfig: KotlinConfigOptions,
    nestedClasses: LinkedHashMap<String, String>
): String {
    return when {
        value is JsonPrimitive && value.isString -> "String"
        value is JsonPrimitive && value.booleanOrNull != null -> "Boolean"
        value is JsonPrimitive && value.longOrNull != null -> "Long"
        value is JsonPrimitive && value.doubleOrNull != null -> "Double"
        value is JsonObject -> {
            parseJsonObject(
                jsonObject = value,
                className = className,
                kotlinConfig = kotlinConfig,
                parsedClasses = nestedClasses
            )
            className
        }

        value is JsonArray -> {
            val firstElement = value.firstOrNull()
            if (firstElement is JsonObject) {
                parseJsonObject(
                    jsonObject = firstElement,
                    className = className,
                    kotlinConfig = kotlinConfig,
                    parsedClasses = nestedClasses
                )
                "List<$className>"
            } else if (firstElement != null) {
                val elementType =
                    determineType(
                        value = firstElement,
                        className = className,
                        kotlinConfig = kotlinConfig,
                        nestedClasses = nestedClasses
                    )
                "List<$elementType>"
            } else {
                "List<Any?>"
            }
        }

        else -> "Any?"
    }
}
