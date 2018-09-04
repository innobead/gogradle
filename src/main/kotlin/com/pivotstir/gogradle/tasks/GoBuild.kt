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

        val osArches = config.osArches.toMutableSet()
        if (osArches.isEmpty()) {
            osArches.add("")
        }

        for (osArch in osArches) {
            val osArchTokens = osArch.split("/")

            var outputPath = listOf(
                    pluginExtension.pluginConfig.dir.canonicalPath,
                    pluginExtension.pluginConfig.modulePath
            ).joinToString(File.separator)

            if (osArchTokens.isNotEmpty()) {
                outputPath += osArchTokens.joinToString("-")

                if (osArchTokens[0] == "windows") {
                    outputPath += ".exe"
                }
            }

            ("go build -o $outputPath".tokens() + config.cmdArgs).joinToString(" ").let {
                logger.lifecycle("Building Go packages for $osArch. Cmd: $it")

                // go build [-o output] [-i] [build flags] [packages]
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