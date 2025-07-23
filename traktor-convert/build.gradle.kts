plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    mingwX64 {
        binaries {
            executable() {
                entryPoint = "main"
//                baseName = "traktor-history-converter"
                runTaskProvider?.get()?.also { runTask ->
                    val args = providers.gradleProperty("runArgs")
                    runTask.workingDir = file("run").also { it.mkdirs() }
                    runTask.argumentProviders.add {
                        args.orNull?.let { listOf(it) }/*?.split(' ')*/ ?: emptyList()
                    }
                }
            }
        }
    }
    sourceSets {
        mingwMain.dependencies {
            implementation(project(":shared"))
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