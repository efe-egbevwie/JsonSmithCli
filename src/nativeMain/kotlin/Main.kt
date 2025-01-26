import languageParsers.ParsedType
import languageParsers.parseJsonToGoStruct
import languageParsers.parseJsonToJavaClass
import languageParsers.parseJsonToKotlinClass
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import targetLanguages.TargetLanguage
import targetLanguages.displayName
import targetLanguages.enabledTargetLanguages
import targetLanguages.getTargetLanguage

fun main(args: Array<String>) {
    if (args.isEmpty() || args.contains("--help") || args.contains("-h")) {
        println("Usage: JsonSmith --file='<file-path>' --language=<language> --output='<output-file=path>'")
        return
    }
    if (args.contains("--man")) {
        println(man)
        return
    }

    try {
        val arguments: Map<String, String> = args.associate {
            val (key, value) = it.split("=")
            key to value
        }

        val filePath: String? = arguments["--file"] ?: arguments["-f"]
        val language: String? = arguments["--language"] ?: arguments["-l"]
        val outputPath: String? = arguments["--output"] ?: arguments["-o"]

        if (language.isNullOrBlank()) {
            println("--language is required")
            return
        }

        val file: Path? = filePath?.toPath()
        if (file == null) {
            println("invalid json file path: $filePath")
            return
        }

        val json = FileSystem.SYSTEM.read(file) {
            readUtf8()
        }

        val targetLanguage: TargetLanguage? = getTargetLanguage(language)
        if (targetLanguage == null) {
            println("unsupported language, supported languages are:")
            enabledTargetLanguages.forEach { println(it.displayName()) }
            return
        }

        val result: ParsedType? = when (targetLanguage) {
            is TargetLanguage.Kotlin -> parseJsonToKotlinClass(
                json = json,
                kotlinConfig = TargetLanguage.Kotlin.KotlinConfigOptions()
            )

            is TargetLanguage.Java -> parseJsonToJavaClass(
                json = json,
                javaConfig = TargetLanguage.Java.JavaConfigOptions()
            )

            is TargetLanguage.Go -> parseJsonToGoStruct(json = json, goConfig = TargetLanguage.Go.GoConfigOptions())
        }

        if (result == null) {
            println("Error: Failed to parse Json, invalid Json provided")
            return
        }
        val outputFilePath: String = buildString {
            outputPath?.let { append(it) }
            append(result.fileName)
            append(targetLanguage.targetLanguageConfig.fileExtension)
        }

        FileSystem.SYSTEM.write(outputFilePath.toPath()) {
            writeUtf8(result.stringRepresentation)
        }
    }
    catch (e:IOException){
        println("Invalid output path specified")
    }
    catch (e: Exception) {
        println("Unable to complete operation: ${e.message}")
    }
}

val man = """
     NAME
            JsonSmith - A CLI tool to generate class/struct definitions from JSON for multiple programming languages.
        
        SYNOPSIS
            JsonSmith --file='<file-path>' --language=<language>
            JsonSmith -f='<file-path>' -l=<language>
        
        DESCRIPTION
            JsonSmith is a command-line tool that takes a JSON file as input and generates class or struct definitions 
            in a specified target programming language. Supported target languages include Kotlin, Java, and Go.
        
        OPTIONS
            --file or -f
                Path to the JSON file. This is a required argument. The path should point to a valid JSON file.
            
            --language or -l
                Target programming language for the generated class or struct. This is a required argument.
                Supported languages are:
                  - Kotlin
                  - Java
                  - Go
            
            --output or -o
                Path to save the generated file (optional). If not provided, the file will be saved in the same 
                directory as the input file.
            
            --help or -h
                Display the usage information for the tool.
                
            --man
                Display the full manual page for the tool.
            
            --version
                Show version information.
        
        EXAMPLES
            Generate a Kotlin class from a JSON file:
                JsonSmith --file='example.json' --language=kotlin
            
            Generate a Go struct from a JSON file:
                JsonSmith -f='example.json' -l=go
            
            Save the output to a custom directory:
                JsonSmith -f='example.json' -l=kotlin -o='./output'
         AUTHOR
            Written by Efe Egbevwie.
        
        REPORTING BUGS
            Report bugs to efe1705@gmail.com.
        
        COPYRIGHT
            This is free and open-source software. Use and modify as per the license.            
""".trimIndent()
