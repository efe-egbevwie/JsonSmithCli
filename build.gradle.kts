plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

group = "com.efe"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    val hostOsSuffix = when{
        hostOs == "Mac OS X" && isArm64 -> "macOsArm64"
        hostOs == "Mac OS X" && !isArm64 -> "macOsX64"
        hostOs == "Linux" && isArm64 -> "linuxArm64"
        hostOs == "Linux" && !isArm64 -> "linuxX64"
        isMingwX64 -> "windowsX64"
        else -> ""
    }
    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
                baseName = "JsonSmith-${hostOsSuffix}"
            }
        }
    }

    sourceSets {
        nativeMain.dependencies {
            implementation(libs.kotlinxSerializationJson)
            implementation(libs.okio)
        }
    }
}


