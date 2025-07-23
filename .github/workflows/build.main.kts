#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("io.github.typesafegithub:github-workflows-kt:3.5.0")

@file:Repository("https://bindings.krzeminski.it")

@file:DependsOn("actions:cache:v4")
@file:DependsOn("actions:checkout:v4")
@file:DependsOn("actions:setup-java:v3")
@file:DependsOn("softprops:action-gh-release:v2.0.6")
@file:DependsOn("joutvhu:/create-release:v1.0.1")
@file:DependsOn("gradle:actions__setup-gradle:v3")
@file:DependsOn("jimeh:update-tags-action:v1.0.1")
@file:DependsOn("msys2:setup-msys2:v2.28.0")

import io.github.typesafegithub.workflows.actions.actions.Cache
import io.github.typesafegithub.workflows.actions.actions.Checkout
import io.github.typesafegithub.workflows.actions.gradle.ActionsSetupGradle
import io.github.typesafegithub.workflows.actions.jimeh.UpdateTagsAction_Untyped
import io.github.typesafegithub.workflows.actions.softprops.ActionGhRelease
import io.github.typesafegithub.workflows.actions.msys2.SetupMsys2_Untyped
import io.github.typesafegithub.workflows.domain.RunnerType
import io.github.typesafegithub.workflows.domain.triggers.Push
import io.github.typesafegithub.workflows.dsl.expressions.Contexts.hashFiles
import io.github.typesafegithub.workflows.dsl.expressions.expr
import io.github.typesafegithub.workflows.dsl.workflow
import io.github.typesafegithub.workflows.yaml.ConsistencyCheckJobConfig
import kotlin.script.experimental.jvmhost.JvmScriptEvaluationConfigurationBuilder.Companion.append

workflow(
    name = "build",
    on = listOf(
        Push(branches = listOf("main", "mistress"))
    ),
    sourceFile = __FILE__,
    consistencyCheckJobConfig = ConsistencyCheckJobConfig.Configuration(
        condition = null,
        env = emptyMap(),
        additionalSteps = {

        },
        useLocalBindingsServerAsFallback = false
    )
) {
    job(id = "build_and_package", runsOn = RunnerType.Windows2022) {
        uses(name = "Check out", action = Checkout())

        uses(
            name = "setup gradle",
            action = ActionsSetupGradle()
        )

        uses(
            name = "setup msys2",
            action = SetupMsys2_Untyped(
                msystem_Untyped = "MINGW64",
                update_Untyped = "true",
                // unsure which of the packages is necessary
                install_Untyped = "git mingw-w64-x86_64-toolchain libsqlite"
            )
        )

        uses(
            name = "cache gradle",
            action = Cache(
                path = listOf("~/.gradle/caches"),
                key = "${expr { runner.os }}-gradle-${ expr(hashFiles("*.gradle.kts", quote = true) )}",
                restoreKeys = listOf(
                    "${expr { runner.os }}-gradle-"
                )
            )
        )

        uses(
            name = "cache konan",
            action = Cache(
                path = listOf("~/.konan"),
                key = "${expr { runner.os }}-konan-${ expr(hashFiles("*.gradle.kts", quote = true) )}",
                restoreKeys = listOf(
                    "${expr { runner.os }}-konan-"
                )
            )
        )

        run(command = "echo \"c:\\msys64\\mingw64\\bin\" >> \$GITHUB_PATH")

        run(command = "./gradlew packageZip copyExecutables --no-daemon")

        uses(
            name = "update tag",
            action = UpdateTagsAction_Untyped(
                tags_Untyped = "nightly"
            )
        )

        uses(
            name = "create release",
            action = ActionGhRelease(
                body = "Nightly Build",
                draft = false,
                prerelease = false,
                files = listOf(
                    "build/dist.zip",
                    "build/*.exe"
                ),
                name = "Latest Build",
                tagName = "nightly",
                failOnUnmatchedFiles = true,
//                token = expr { github.token },
                generateReleaseNotes = true,
                // drafts and prereleases cannot use makeLatest
                makeLatest = ActionGhRelease.MakeLatest.True,
            )
        )
    }
}

