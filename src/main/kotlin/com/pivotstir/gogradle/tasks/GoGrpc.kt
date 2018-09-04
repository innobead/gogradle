package com.pivotstir.gogradle.tasks

import com.pivotstir.gogradle.GoPlugin
import com.pivotstir.gogradle.GradleSupport
import com.pivotstir.gogradle.taskName
import com.pivotstir.gogradle.tokens
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.io.File

class GoGrpcConfig(
        val project: Project,
        @Input @Optional var protoDir: File = File("proto")
)

@GradleSupport
class GoGrpc : AbstractGoTask<GoGrpcConfig>(GoGrpcConfig::class) {

    init {
        group = GoPlugin.NAME
        description = "Generate gRPC and Protobuf code"

        dependsOn(
                taskName(GoDep::class)
        )
    }

    override fun run() {
        super.run()

        val pkgFilePaths = mutableMapOf<String, MutableList<String>>()

        config.protoDir.walkTopDown().forEach {
            if (it.isFile) {
                val result = """package (\S+);""".toRegex().find(it.readText())

                if (result != null) {
                    val (pkgName) = result.destructured

                    if (pkgName !in pkgFilePaths) {
                        pkgFilePaths[pkgName] = mutableListOf()
                    }

                    pkgFilePaths[pkgName]!!.add(it.path)
                }
            }
        }

        val protocFile = (project.tasks.findByName(taskName(GoDep::class)) as GoDep).protocFile
        val generatedDir = project.projectDir
        val gopathDir = task<GoEnv>()!!.goPathDir

        pkgFilePaths.forEach { _, filePaths ->
            val cmd = """$protocFile
                -I${config.protoDir}
                -I$gopathDir/src
                -I$gopathDir/src/github.com/grpc-ecosystem/grpc-gateway/third_party/googleapis
                --go_out=plugins=grpc:$generatedDir
                --grpc-gateway_out=logtostderr=true:$generatedDir
                --swagger_out=logtostderr=true:$generatedDir
                ${filePaths.joinToString(" ")}""".trimIndent()

            logger.lifecycle("Generating gRPC stub files. Cmd: $cmd")

            exec(cmd.tokens()) { spec ->
                spec.environment.putAll(this.goEnvs(spec.environment))
            }
        }
    }
}