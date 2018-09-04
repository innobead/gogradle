package com.pivotstir.gogradle.tasks

import com.pivotstir.gogradle.GradleSupport
import com.pivotstir.gogradle.tokens
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File


@GradleSupport
class Go : DefaultTask() {

    val cmds = sortedMapOf<String, Map<String, Any>>()

    var stdoutFile: File? = null

    fun go(cmd: String, envs: Map<String, Any> = emptyMap()) {
        cmds["go $cmd"] = envs
    }

    @TaskAction
    fun run() {
        cmds.forEach { cmd, envs ->
            logger.lifecycle("Running Cmd: $cmd")

            exec(cmd.tokens()) {
                it.environment.putAll(envs)

                if (stdoutFile != null) {
                    it.standardOutput = stdoutFile!!.outputStream()
                }
            }
        }
    }

}