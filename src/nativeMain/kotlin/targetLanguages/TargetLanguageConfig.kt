package targetLanguages

sealed interface TargetLanguageConfig{
    val className: String
    val saveClassesAsSeparateFiles: Boolean
    val fileExtension: String
}
