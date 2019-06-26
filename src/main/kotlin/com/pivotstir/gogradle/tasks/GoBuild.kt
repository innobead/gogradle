package com.pivotstir.gogradle.tasks

import com.pivotstir.gogradle.GoPlugin
import com.pivotstir.gogradle.GradleSupport
import com.pivotstir.gogradle.taskName
import com.pivotstir.gogradle.tokens
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.io.File

class GoBuildConfig(
        val project: Project,
        @Input @Optional var cmdArgs: List<String> = emptyList(),
        @Input @Optional var envs: Map<String, Any> = emptyMap(), // CGO_ENABLED=0
        @Input @Optional var osArches: List<String> = emptyList(),
        @Input @Optional var packagePaths: List<String> = emptyList()
)

@GradleSupport
class GoBuild : AbstractGoTask<GoBuildConfig>(GoBuildConfig::class) {

    init {
        group = GoPlugin.NAME
        description = "Build Go project"

        dependsOn(
                taskName(GoDep::class),
                taskName(GoGrpc::class),
                taskName(GoTest::class)
        )
    }

    override fun run() {
        super.run()

        if (config.packagePaths.isEmpty()) {
            logger.lifecycle("No packagePaths specified to build")
            return
        }

        val osArches = config.osArches.toMutableSet()
        if (osArches.isEmpty()) {
            osArches.add("")
        }

        for (osArch in osArches) {
            val osArchTokens = osArch.split("/")

            for (pkg in config.packagePaths) {
                var outputPath = listOf(
                        pluginExtension.pluginConfig.dir.canonicalPath,
                        pkg.split(File.separator).last()
                ).joinToString(File.separator)

                if (osArchTokens.isNotEmpty()) {
                    outputPath += osArchTokens.joinToString("-")

                    if (osArchTokens[0] == "windows") {
                        outputPath += ".exe"
                    }
                }

                ("go build -o $outputPath".tokens() + config.cmdArgs + listOf(pkg)).joinToString(" ").let {
                    logger.lifecycle("Building Go packagePaths for $osArch. Cmd: $it")

                    // go build [-o output] [-i] [build flags] [packagePaths]
                    exec(it) { spec ->
                        spec.environment.putAll(this.goEnvs(spec.environment))
                        spec.environment.putAll(config.envs)

                        if (osArchTokens.size == 2) {
                            spec.environment["GOOS"] = osArchTokens[0]
                            spec.environment["GOARCH"] = osArchTokens[1]
                        }
                    }
                }
            }
        }
    }

}