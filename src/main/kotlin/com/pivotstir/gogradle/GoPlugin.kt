package com.pivotstir.gogradle

import com.pivotstir.gogradle.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File
import kotlin.reflect.KClass


class GoPluginConfig(
        val project: Project,
        val dir: File = File(".${GoPlugin.NAME}"),
        val cacheDir: File = File(dir, "caches"),
        val envDir: File = File(dir, "env"),
        val reportDir: File = File(dir, "reports"),
        val protoDir: File = File(dir, "proto"),
        var modulePath: String = project.name
)

class GoPlugin : Plugin<Project> {

    companion object {
        const val NAME = "gogradle"
    }

    override fun apply(target: Project) {
        target.extensions.create("go", GoPluginExtension::class.java, target)

        target.afterEvaluate {
            data class TaskInfo(
                    val cls: KClass<*>,
                    val args: List<Any> = emptyList()
            )

            val tasks = listOf(
                    TaskInfo(GoBuild::class),
                    TaskInfo(GoClean::class),
                    TaskInfo(GoDep::class),
                    TaskInfo(GoEnv::class),
                    TaskInfo(GoSwag::class),
                    TaskInfo(GoGrpc::class),
                    TaskInfo(GoTest::class)
            )

            tasks.forEach { taskInfo ->
                @Suppress("UNCHECKED_CAST")
                taskInfo.cls as KClass<Task>

                target.tasks.create(taskName(taskInfo.cls), taskInfo.cls.java, *taskInfo.args.toTypedArray())
            }
        }
    }
}

