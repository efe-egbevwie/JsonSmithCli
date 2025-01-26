package languageParsers

import targetLanguages.TargetLanguage.Java.JavaConfigOptions
import targetLanguages.TargetLanguage.Java.JavaSerializationFrameWorks
import util.capitalize
import util.isSnakeCase
import util.toCamelCase
import kotlinx.serialization.json.*

fun parseJsonToJavaClass(
    json: String,
    className: String = "JsonClass",
    parsedClasses: LinkedHashMap<String, String> = LinkedHashMap(),
    javaConfig: JavaConfigOptions
): ParsedType? {
    val jsonTrimmed = json.trim()
    return try {
        when (val jsonElement = Json.parseToJsonElement(jsonTrimmed)) {
            is JsonObject -> parseJavaJsonObject(
                jsonObject = jsonElement,
                className = className,
                parsedClasses = parsedClasses,
                config = javaConfig
            )

            is JsonArray -> parseJavaJsonArray(
                jsonArray = jsonElement,
                className = className,
                parsedClasses = parsedClasses,
                config = javaConfig
            )

            else -> null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun parseJavaJsonObject(
    jsonObject: JsonObject,
    className: String,
    parsedClasses: LinkedHashMap<String, String>,
    config: JavaConfigOptions
): ParsedType {
    val properties = mutableListOf<String>()
    val methods = mutableListOf<String>()

    val javaSerializationFramework = (config.serializationFrameWork) as JavaSerializationFrameWorks
    val useLombok = javaSerializationFramework == JavaSerializationFrameWorks.Lombok
    val usePlainTypes = javaSerializationFramework == JavaSerializationFrameWorks.PlainTypes
    val useRecords = javaSerializationFramework == JavaSerializationFrameWorks.Records


    jsonObject.entries.forEachIndexed { index, (key, value) ->
        val type =
            determineJavaType(
                value = value,
                elementName = key.capitalize(),
                nestedClasses = parsedClasses,
                config = config
            )
        val fieldName = if (key.isSnakeCase()) key.toCamelCase() else key
        val isLastProperty = index == jsonObject.entries.size - 1

        val usePropertyAnnotation = key.isSnakeCase() && key != fieldName
        val propertyAnnotation = when {
            (useRecords || useLombok) && usePropertyAnnotation -> "@JsonProperty(\"$key\")"
            else -> null
        }
        val property = buildString {
            if (propertyAnnotation != null) {
                appendLine("    $propertyAnnotation")
            }
            if (useRecords) {
                if (isLastProperty) {
                    append("    $type $fieldName")
                } else {
                    append("    $type $fieldName,")
                }

            } else {
                append("    private $type $fieldName;")
            }
        }
        properties.add(property)

        if (usePlainTypes) {
            val getter = """
                |    public $type get${fieldName.capitalize()}() {
                |        return $fieldName;
                |    }
            """.trimMargin()
            val setter = """
                |    public void set${fieldName.capitalize()}($type $fieldName) {
                |        this.$fieldName = $fieldName;
                |    }
            """.trimMargin()
            methods.add(getter)
            methods.add(setter)
        }
    }

    val javaClass = buildString {
        if (useLombok) appendLine("@Data")
        val javaClassName = when {
            useRecords -> "public record $className ("
            useLombok || usePlainTypes -> "public class $className {"
            else -> "public class $className {"
        }
        appendLine(javaClassName)
        properties.forEach { appendLine(it) }
        when {
            usePlainTypes -> {
                appendLine()
                methods.forEach { appendLine(it) }
            }
        }
        if (useRecords) {
            appendLine("){}")
        } else {
            appendLine("}")
        }
    }

    parsedClasses[className] = javaClass
    val shouldAddJacksonImport = (useRecords || useLombok) && (parsedClasses.values.toList().any { it.isSnakeCase() })
    val imports = when {
        useLombok && shouldAddJacksonImport.not() -> "import lombok.Data;"
        useLombok && shouldAddJacksonImport -> "import lombok.Data;\nimport com.fasterxml.jackson.annotation.JsonProperty;"
        useRecords && shouldAddJacksonImport -> "import com.fasterxml.jackson.annotation.JsonProperty;"
        else -> null
    }


    val finalType = buildString {
        imports?.let { append(it) }
        appendLine("\n")
        appendLine(parsedClasses.values.reversed().joinToString(separator = "\n\n"))
    }
    val finalParsedClass = parsedClasses.entries.reversed().map { parsedClass ->
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

private fun parseJavaJsonArray(
    jsonArray: JsonArray,
    className: String,
    parsedClasses: LinkedHashMap<String, String>,
    config: JavaConfigOptions
): ParsedType {
    return parseJavaJsonObject(
        jsonObject = jsonArray.first().jsonObject,
        className = className,
        parsedClasses = parsedClasses,
        config = config
    )
}

private fun determineJavaType(
    value: JsonElement,
    elementName: String,
    nestedClasses: LinkedHashMap<String, String>,
    config: JavaConfigOptions
): String {
    return when {
        value is JsonPrimitive && value.isString -> "String"
        value is JsonPrimitive && value.booleanOrNull != null -> "boolean"
        value is JsonPrimitive && value.longOrNull != null -> "long"
        value is JsonPrimitive && value.doubleOrNull != null -> "double"
        value is JsonObject -> {
            parseJavaJsonObject(
                jsonObject = value,
                className = elementName,
                parsedClasses = nestedClasses,
                config = config
            )
            elementName
        }

        value is JsonArray -> {
            val firstElement = value.firstOrNull()
            if (firstElement is JsonObject) {
                val nestedClassName = elementName
                parseJavaJsonObject(
                    jsonObject = firstElement,
                    className = nestedClassName,
                    parsedClasses = nestedClasses,
                    config = config
                )
                if (config.useArrays) "$nestedClassName[]" else "List<$nestedClassName>"
            } else if (firstElement != null) {
                val elementType =
                    determineJavaType(
                        value = firstElement,
                        elementName = elementName,
                        nestedClasses = nestedClasses,
                        config = config
                    )
                if (config.useArrays) "$elementType[]" else "List<$elementType>"
            } else {
                if (config.useArrays) "Object[]" else "List<Object>"
            }
        }

        else -> "Object"
    }
}
