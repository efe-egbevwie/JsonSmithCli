package targetLanguages


val enabledTargetLanguages: List<TargetLanguage> =
    listOf(TargetLanguage.Kotlin(), TargetLanguage.Java(), TargetLanguage.Go())


sealed interface TargetLanguage {
    val targetLanguageConfig:TargetLanguageConfig
    data class Kotlin(
        override var targetLanguageConfig: TargetLanguageConfig = KotlinConfigOptions(),
    ) : TargetLanguage {

        data class KotlinConfigOptions(
            override val className: String = "JsonClass",
            override val saveClassesAsSeparateFiles: Boolean = false,
            override val fileExtension: String = ".kt",
            val serializationFrameWork: SerializationFrameWork = KotlinSerializationFrameWorks.Kotlinx,
            val allPropertiesOptional: Boolean = true,
        ) : TargetLanguageConfig

        enum class KotlinSerializationFrameWorks : SerializationFrameWork {
            Kotlinx,
            Gson,
            Jackson
        }
    }


    data class Java(override var targetLanguageConfig: TargetLanguageConfig = JavaConfigOptions()) :
        TargetLanguage {
        data class JavaConfigOptions(
            override val saveClassesAsSeparateFiles: Boolean = true,
            override val className: String = "JsonClass",
            override val fileExtension: String = ".java",
            val useLombok: Boolean = false,
            val useArrays: Boolean = true,
            val serializationFrameWork: SerializationFrameWork? = JavaSerializationFrameWorks.Lombok,
        ) : TargetLanguageConfig

        enum class JavaSerializationFrameWorks : SerializationFrameWork {
            Records,
            Lombok,
            PlainTypes
        }
    }

    data class Go(override var targetLanguageConfig: TargetLanguageConfig = GoConfigOptions()) :
        TargetLanguage {
        data class GoConfigOptions(
            override val saveClassesAsSeparateFiles: Boolean = false,
            override val className: String = "JsonClass",
            override val fileExtension: String = ".go",
        ) : TargetLanguageConfig
    }

}


fun TargetLanguage.displayName(): String {
    return when (this) {
        is TargetLanguage.Java -> "Java"
        is TargetLanguage.Kotlin -> "Kotlin"
        is TargetLanguage.Go -> "Go"
    }
}

fun getTargetLanguage(arg: String): TargetLanguage? {
    return when (arg.lowercase()) {
        "kotlin", "kt" -> TargetLanguage.Kotlin()
        "java" -> TargetLanguage.Java()
        "go" -> TargetLanguage.Go()
        else -> null
    }
}
