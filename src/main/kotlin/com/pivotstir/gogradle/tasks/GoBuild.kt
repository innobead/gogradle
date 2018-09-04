package com.pivotstir.gogradle.tasks

import com.pivotstir.gogradle.GoPlugin
import com.pivotstir.gogradle.GradleSupport
import com.pivotstir.gogradle.taskName
import com.pivotstir.gogradle.tokens
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class GoBuildConfig(
        val project: Project,
        @Input @Optional var cmdArgs: List<String> = emptyList(),
        @Input @Optional var envs: Map<String, String> = emptyMap(),
        @Input @Optional var packages: List<String> = listOf("./...")
)

@GradleSupport
class GoBuild : AbstractGoTask<GoBuildConfig>(GoBuildConfig::class) {

    init {
        group = GoPlugin.NAME
        description = "Build Go project"

        dependsOn(
                taskName(GoDep::class),
                taskName(GoTest::class)
        )
    }

    override fun run() {
        super.run()

        val cmds = "go build".tokens() + config.cmdArgs + config.packages

        logger.lifecycle("Building Go packages\n ${cmds.joinToString(" ")}")

        // go build [-o output] [-i] [build flags] [packages]
        exec(cmds) {
            it.environment.putAll(config.envs)
        }
    }

}