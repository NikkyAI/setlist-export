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
//                baseName = "mixxx-history-exporter"
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
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:_")
            implementation("io.github.smyrgeorge:sqlx4k-sqlite:_")

            implementation("com.squareup.okio:okio:_")
            implementation("com.saveourtool.okio-extras:okio-extras:_")
        }
        mingwMain.dependencies {
//            implementation("io.ktor:ktor-client-winhttp:_")
        }
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