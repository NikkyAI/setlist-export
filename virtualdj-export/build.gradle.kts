import de.fayard.refreshVersions.core.versionFor
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    mingwX64 {
        binaries {
            executable() {
                entryPoint = "main"
                if(System.getenv("CI") == null) {
                    baseName = "virtualdj-export-testbuild"
                }
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
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation("io.github.pdvrieze.xmlutil:serialization:_")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:_")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:_")

            implementation("com.squareup.okio:okio:_")
            implementation("com.saveourtool.okio-extras:okio-extras:_")
        }
        mingwMain.dependencies {
//            implementation("io.ktor:ktor-client-winhttp:_")
        }
//        all {
//            languageSettings.enableLanguageFeature("WhenGuards")
//        }
    }

}


//sqldelight {
//    databases {
//        create("Database") {
////            dialect("sqlite3")
//            packageName = "rekordbox"
////            this.srcDirs.from("")
//
//
//            val sqlDelightVersion = versionFor("version.app.cash.sqldelight")
//            dialect("app.cash.sqldelight:sqlite-3-38-dialect:$sqlDelightVersion")
//        }
//    }
//    linkSqlite = false
//}

//dependencies {
////    testImplementation(kotlin("test"))
//    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
//    implementation("com.fleeksoft.ksoup:ksoup:0.2.2")
////    implementation("com.mohamedrejeb.ksoup:ksoup-html:0.5.0")
//}

//tasks.test {
//    useJUnitPlatform()
//}
kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}