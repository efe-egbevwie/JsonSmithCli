package util


fun String.toCamelCase(): String {
    val pattern = "_[a-z]".toRegex()
    return replace(pattern) { it.value.last().uppercase() }
}

fun String.isSnakeCase(): Boolean {
    return this.contains('_')
}

fun String.capitalize(): String {
    return replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
}

fun String.toClassNameCamelCase(): String {
    val words = this.split("_", " ", "-")
    val capitalizedWords = words.map { it.capitalize() }
    return capitalizedWords.first() + capitalizedWords.drop(1).joinToString("")
}
