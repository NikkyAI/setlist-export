#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("io.github.typesafegithub:github-workflows-kt:3.5.0")

@file:Repository("https://bindings.krzeminski.it")

@file:DependsOn("actions:checkout:v4")
@file:DependsOn("actions:setup-java:v3")
@file:DependsOn("softprops:action-gh-release:v2.0.6")
@file:DependsOn("joutvhu:/create-release:v1.0.1")
@file:DependsOn("gradle:actions__setup-gradle:v3")
@file:DependsOn("jimeh:update-tags-action:v1.0.1")

import io.github.typesafegithub.workflows.actions.actions.Checkout
import io.github.typesafegithub.workflows.actions.actions.SetupJava
import io.github.typesafegithub.workflows.actions.gradle.ActionsSetupGradle
import io.github.typesafegithub.workflows.actions.jimeh.UpdateTagsAction_Untyped
import io.github.typesafegithub.workflows.actions.softprops.ActionGhRelease
import io.github.typesafegithub.workflows.domain.RunnerType
import io.github.typesafegithub.workflows.domain.RunnerType.UbuntuLatest
import io.github.typesafegithub.workflows.domain.triggers.Push
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

