import de.fayard.refreshVersions.core.versionFor
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
//    id("app.cash.sqldelight")
//    id("com.gradleup.shadow")
//    application
}

group = "moe.nikky"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
//    jvm() {
//
//    }
    mingwX64 {
        binaries {
            executable() {
                this.entryPoint = "main"
//                baseName = "rekordbox-history-exporter"
                runTaskProvider?.get()?.also { runTask ->
                    val args = providers.gradleProperty("runArgs")
                    runTask.argumentProviders.add {
                        args.orNull?.let { listOf(it) }/*?.split(' ')*/ ?: emptyList()
                    }
                }
            }
        }
    }.also { target ->
//        configInterop(target)
    }
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:_")
            implementation("io.github.smyrgeorge:sqlx4k-sqlite:_")

            implementation("io.ktor:ktor-client-core:_")
            implementation("io.ktor:ktor-serialization-kotlinx-json:_")

            implementation("com.squareup.okio:okio:_")
            implementation("com.saveourtool.okio-extras:okio-extras:_")


            implementation("org.jetbrains.kotlinx:kotlinx-io-core:_")
            implementation("org.jetbrains.kotlinx:kotlinx-io-okio:_")
        }
        mingwMain.dependencies {
            implementation("io.ktor:ktor-client-winhttp:_")
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