package com.pivotstir.gogradle.tasks

import com.pivotstir.gogradle.GoPlugin
import com.pivotstir.gogradle.GradleSupport
import org.gradle.api.Project

class GoCleanConfig(
        val project: Project
)

@GradleSupport
class GoClean : AbstractGoTask<GoCleanConfig>(GoCleanConfig::class) {

    init {
        group = GoPlugin.NAME
        description = "Clean Go project"
    }

    override fun run() {
        super.run()

        logger.lifecycle("Deleting ${pluginExtension.pluginConfig.dir}")

        pluginExtension.pluginConfig.dir.deleteRecursively()
    }
}