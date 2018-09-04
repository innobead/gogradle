package com.pivotstir.gogradle.tasks

import com.pivotstir.gogradle.GoPlugin
import com.pivotstir.gogradle.GradleSupport
import com.pivotstir.gogradle.taskName
import com.pivotstir.gogradle.tokens
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.io.File

class GoTestConfig(
        val project: Project,
        @Input @Optional var cmdArgs: List<String> = emptyList(),
        @Input @Optional var envs: Map<String, String> = emptyMap(),
        @Input @Optional var packages: List<String> = listOf("./...")
)

@GradleSupport
class GoTest : AbstractGoTask<GoTestConfig>(GoTestConfig::class) {

    init {
        group = GoPlugin.NAME
        description = "Test Go project"

        dependsOn(
                taskName(GoDep::class),
                taskName(GoGrpc::class)
        )
    }

    private val coverageReportFile: File by lazy {
        File(pluginExtension.pluginConfig.reportDir, "cover.out")
    }

    private val coverageJsonReportFile: File by lazy {
        File(pluginExtension.pluginConfig.reportDir, "cover.json")
    }

    private val coverageXmlReportFile: File by lazy {
        File(pluginExtension.pluginConfig.reportDir, "cover.xml")
    }

    override fun run() {
        super.run()

        ("go test -coverprofile $coverageReportFile".tokens() + config.cmdArgs + config.packages).let {
            logger.lifecycle("Testing Go packages. Cmd: ${it.joinToString(" ")}")

            // go test [build/test flags] [packages] [build/test flags & test binary flags]
            exec(it) { spec ->
                spec.environment.putAll(this.goEnvs(spec.environment))
                spec.environment.putAll(config.envs)
            }
        }

        "gocov convert $coverageReportFile".tokens().let {
            logger.lifecycle("Converting coverage report ($coverageReportFile) to json report ($coverageJsonReportFile)")

            exec(it) { spec ->
                spec.environment.putAll(this.goEnvs(spec.environment))
                spec.standardOutput = coverageJsonReportFile.outputStream()
            }
        }

        "gocov-xml $coverageJsonReportFile $coverageXmlReportFile".tokens().let {
            logger.lifecycle("Converting coverage Json report ($coverageJsonReportFile) to Cobertura XML report ($coverageXmlReportFile)")

            exec(it) { spec ->
                spec.environment.putAll(this.goEnvs(spec.environment))
                spec.standardInput = coverageJsonReportFile.inputStream()
                spec.standardOutput = coverageXmlReportFile.outputStream()
            }
        }
    }
}