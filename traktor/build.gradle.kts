plugins {
    kotlin("multiplatform")
}

group = "moe.nikky"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
kotlin {
    mingwX64 {
        binaries {
            executable() {
                entryPoint = "main"
//                baseName = "traktor-history-converter"
                runTaskProvider?.get()?.also { runTask ->
                    val args = providers.gradleProperty("runArgs")
                    runTask.argumentProviders.add {
                        args.orNull?.let { listOf(it) }/*?.split(' ')*/ ?: emptyList()
                    }
                }
            }
        }
    }
    sourceSets {
        mingwMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:_")
            implementation("com.fleeksoft.ksoup:ksoup:_")
            implementation("com.squareup.okio:okio:_")
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}