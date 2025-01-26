package targetLanguages

sealed interface SerializationFrameWork

fun SerializationFrameWork.displayName(): String {
    return when (this) {
        is TargetLanguage.Kotlin.KotlinSerializationFrameWorks -> this.name
        TargetLanguage.Java.JavaSerializationFrameWorks.Records -> "Records"
        TargetLanguage.Java.JavaSerializationFrameWorks.Lombok -> "Lombok"
        TargetLanguage.Java.JavaSerializationFrameWorks.PlainTypes -> "Plain Types"
    }
}