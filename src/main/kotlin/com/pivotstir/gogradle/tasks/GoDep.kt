package com.pivotstir.gogradle.tasks

import com.pivotstir.gogradle.*
import kotlinx.coroutines.experimental.*
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.process.internal.ExecException
import org.rauschig.jarchivelib.ArchiveFormat
import org.rauschig.jarchivelib.ArchiverFactory
import java.io.ByteArrayOutputStream
import java.io.File

class GoDepConfig(
        val project: Project,
        @Input @Optional var cmdArgs: List<String> = emptyList(),
        @Input @Optional var envs: Map<String, String> = emptyMap(),
        val minProtoVersion: String = "3.6.1",
        @Input @Optional var protoVersion: String = minProtoVersion
)

@GradleSupport
class GoDep : AbstractGoTask<GoDepConfig>(GoDepConfig::class) {

    companion object {
        fun downloadProtoUrl(version: String, platform: String, arch: String) =
                "https://github.com/google/protobuf/releases/download/v$version/protoc-$version-$platform-$arch.zip".toURL()
    }

    init {
        group = GoPlugin.NAME
        description = "Resolve Go project library and 3rd party tool dependencies"

        dependsOn(taskName(GoEnv::class))
    }

    val protocFile: File by lazy {
        File(pluginExtension.pluginConfig.protoDir, listOf("bin", "protoc").joinToString(File.separator))
    }

    override fun run() {
        super.run()

        checkVersion("Protoc", config.protoVersion, config.minProtoVersion)

        runBlocking {
            delay(3000)

            val errs = awaitAll(
                    downloadProtoTools(),
                    download3rdTools(),
                    downloadDependentLibs()
            )

            errs.forEach {
                if (it is RuntimeException) {
                    throw it
                }
            }
        }
    }

    private fun downloadProtoTools(): Deferred<Any?> = async {
        return@async try {
            if (protocFile.exists()) {
                val out = ByteArrayOutputStream()
                val process = exec("$protocFile --version".tokens()) {
                    it.standardOutput = out
                }

                if (process.exitValue == 0 && """libprotoc ${config.protoVersion}""".toRegex().containsMatchIn(out.toString())) {
                    logger.lifecycle("Protoc found locally (${config.protoVersion})\n'${out.toString().trim()}'")
                    return@async
                }
            }

            var (platform, arch) = getOsArch()

            if (platform == "darwin") {
                platform = "osx"
            }

            // download
            downloadArtifact(
                    "Proto archive",
                    downloadProtoUrl(config.protoVersion, platform, arch),
                    pluginExtension.pluginConfig.protoDir,
                    ArchiverFactory.createArchiver(ArchiveFormat.ZIP)
            )
        } catch (e: Throwable) {
            e
        }
    }

    private fun download3rdTools(): Deferred<Any?> = async {
        return@async try {
            val cmds = listOf(
                    "github.com/grpc-ecosystem/grpc-gateway/protoc-gen-grpc-gateway",
                    "github.com/grpc-ecosystem/grpc-gateway/protoc-gen-swagger",
                    "github.com/golang/protobuf/protoc-gen-go",
                    "google.golang.org/grpc",
                    "github.com/wadey/gocovmerge",
                    "github.com/axw/gocov/gocov",
                    "github.com/AlekSi/gocov-xml",
                    "github.com/swaggo/swag/cmd/swag"
            )

            cmds.forEach {
                logger.lifecycle("Starting to install $it to \$GOPATH/bin")
            }

            val cmd = "go get -u ${cmds.joinToString(" ")}"

            logger.lifecycle("Installing Go package dependencies:\n$cmd")

            exec(cmd.tokens()) { spec ->
                spec.environment.putAll(goEnvs(spec.environment))
                spec.environment["GO111MODULE"] = "off"
            }
        } catch (e: Throwable) {
            e
        }
    }

    private fun downloadDependentLibs(): Deferred<Any?> = async {
        return@async try {
            logger.lifecycle("Generating go.mod")

            val p = Runtime.getRuntime().exec("go mod init ${pluginExtension.pluginConfig.modulePath}").apply { waitFor() }
            when {
                p.exitValue() == 1 -> {
                    logger.lifecycle("go.mod already exists")
                }

                p.exitValue() != 0 -> {
                    throw ExecException("Error to execute go command: return code = ${p.exitValue()}, ${p.inputStream.bufferedReader().readText()}")
                }
            }

            /*
                download    download modules to local cache
                edit        edit go.mod from tools or scripts
                graph       print module requirement graph
                init        initialize new module in current directory
                tidy        add missing and remove unused modules
                vendor      make vendored copy of dependencies
                verify      verify dependencies have expected content
                why         explain why packages or modules are needed
             */

            logger.lifecycle("Updating Go library dependencies in go.mod")

            val pkgs = pluginExtension.dependenciesConfig.dependencies()

            if (pkgs.isEmpty()) {
                return@async
            }

            pkgs.map {
                logger.lifecycle("Starting to update ${it.path}")
            }

            val cmd = "go get -d ${config.cmdArgs} ${pkgs.map { it.path }.joinToString(" ") { it }}"

            logger.lifecycle("Updating Go package dependencies:\n$cmd")

            exec(cmd.tokens()) { spec ->
                spec.environment.putAll(goEnvs(spec.environment))
                spec.environment["GO111MODULE"] = "on"
            }
        } catch (e: Throwable) {
            e
        }
    }
}