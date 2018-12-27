package com.pivotstir.gogradle.tasks

import com.pivotstir.gogradle.GoPlugin
import com.pivotstir.gogradle.GradleSupport
import com.pivotstir.gogradle.toURL
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.rauschig.jarchivelib.ArchiveFormat
import org.rauschig.jarchivelib.ArchiverFactory
import org.rauschig.jarchivelib.CompressionType
import java.io.ByteArrayOutputStream
import java.io.File

class GoEnvConfig(
        val project: Project,
        var useSandbox: Boolean = true,
        val minVersion: String = "1.11.4", // need "go module" which starts from 1.11
        @Input @Optional var version: String = minVersion
)

@GradleSupport
class GoEnv : AbstractGoTask<GoEnvConfig>(GoEnvConfig::class) {
    companion object {
        fun downloadGoUrl(version: String, platform: String, arch: String) = "https://dl.google.com/go/go$version.$platform-$arch.tar.gz".toURL()
    }

    init {
        group = GoPlugin.NAME
        description = "Setup Go environment"
    }

    val goDir: File = File(pluginExtension.pluginConfig.envDir, "go")
    var goLocalDir: File? = null
    val goExec: File = File(goDir, listOf("bin", "go").joinToString(File.separator))
    val goPathDir: File = File(goDir, "gopath").also { it.mkdirs() }

    override fun run() {
        super.run()

        // check version compatible
        checkVersion("Go", config.version, config.minVersion)

        // check if there is go exec locally to match the version
        if (!isGoAvailable()) {
            downloadGo()
        }
    }

    override fun goEnvs(envs: Map<String, Any>): Map<String, Any> {
        val newEnvs = envs.toMutableMap()

        newEnvs["GOROOT"] = if (config.useSandbox) {
            goDir.canonicalPath
        } else {
            goLocalDir?.canonicalPath ?: ""
        }

        newEnvs["GOPATH"] = goPathDir.canonicalPath
        newEnvs["GOBIN"] = newEnvs["GOPATH"].toString() + File.separator + "bin"

        if (goDir.exists()) {
            if ("PATH" in newEnvs) {
                newEnvs["PATH"] = listOf(
                        goExec().parentFile.canonicalPath,
                        File(goPathDir, "bin").canonicalPath,
                        newEnvs["PATH"]
                ).joinToString(File.pathSeparator)
            }
        }

        logger.debug("Environment: $newEnvs")

        return newEnvs
    }

    private fun downloadGo() {
        if (goDir.exists()) {
            logger.lifecycle("Deleting cached downloaded Go files in $goDir")
            goDir.deleteRecursively()
        }

        // download
        var (platform, arch) = getOsArch()

        if (arch == "x86_64") {
            arch = "amd64"
        }

        downloadArtifact(
                "Go archive",
                downloadGoUrl(pluginExtension.envConfig.version, platform, arch),
                pluginExtension.pluginConfig.envDir,
                ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
        )
    }

    private fun isGoAvailable(): Boolean {
        val out = ByteArrayOutputStream()

        val cmd = when {
            (goExec.exists() && goExec.canExecute()) -> "$goExec version"

            !config.useSandbox -> "go version"

            else -> return false
        }

        val process = this.exec(cmd) {
            it.environment.putAll(goEnvs(it.environment))
            it.standardOutput = out
        }

        return process.exitValue == 0 && """go version go${config.version}""".toRegex().containsMatchIn(out.toString()).also {
            if (!it) {
                logger.lifecycle("Go found locally, but not matched version (${config.version})\n'${out.toString().trim()}'")
            } else {
                logger.lifecycle("Go found locally (${config.version})\n${out.toString().trim()}")

                if (!config.useSandbox && cmd == "go version") {
                    out.reset()

                    exec("go env GOROOT") {
                        it.environment.putAll(goEnvs(it.environment))
                        it.standardOutput = out
                    }

                    goLocalDir = File(out.toString().trim())
                }
            }
        }
    }

    private fun goExec(): File = if (!config.useSandbox && goLocalDir != null) {
        File(goLocalDir, listOf("bin", "go").joinToString(File.separator))
    } else {
        this.goExec
    }
}