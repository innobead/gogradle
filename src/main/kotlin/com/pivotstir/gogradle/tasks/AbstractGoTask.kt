package com.pivotstir.gogradle.tasks

import com.github.zafarkhaja.semver.Version
import com.pivotstir.gogradle.GoPluginExtension
import com.pivotstir.gogradle.taskName
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.SystemUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.TaskInstantiationException
import org.gradle.process.ExecSpec
import org.rauschig.jarchivelib.Archiver
import java.io.File
import java.net.URL
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties


abstract class AbstractGoTask<A : Any>(private val configClass: KClass<A>) : DefaultTask() {

    val pluginExtension: GoPluginExtension by lazy {
        project.extensions.findByType(GoPluginExtension::class.java)!!
    }

    val config: A by lazy {
        for (p in pluginExtension::class.declaredMemberProperties) {
            if (p.returnType.classifier == configClass) {
                @Suppress("UNCHECKED_CAST")
                p as KProperty1<GoPluginExtension, A>

                return@lazy p.get(pluginExtension)
            }
        }

        throw TaskInstantiationException("$configClass config class not found!")
    }

    fun downloadArtifact(artifactName: String, url: URL, destDir: File, archiver: Archiver): Pair<File, File> {
        logger.lifecycle("Starting to download $artifactName from $url, because $artifactName not found or matched locally")

        // download
        val downloadFile = File(pluginExtension.pluginConfig.cacheDir, FilenameUtils.getName(url.path))

        val request = Request.Builder().url(url.toString()).build()
        val response = OkHttpClient().newCall(request).execute()

        if (!response.isSuccessful) {
            logger.error("Failed to download $artifactName from $url")

            throw TaskExecutionException(this, RuntimeException("Message: ${response.message()}; Status Code: ${response.code()}"))
        }

        logger.lifecycle("Downloading $artifactName from $url")

        downloadFile.outputStream().use {
            response.body()?.byteStream()?.copyTo(it)
        }

        // extract
        try {
            logger.lifecycle("Extracting $artifactName ($downloadFile) to $destDir")

            archiver.extract(downloadFile, destDir)
        } catch (e: RuntimeException) {
            logger.error("Failed to extract $artifactName ($downloadFile)")

            throw TaskExecutionException(this, e)
        }

        return Pair(downloadFile, destDir)
    }

    fun checkVersion(versionType: String, version: String, minVersion: String) {
        logger.lifecycle("Checking $versionType version")

        val newVersion = if (version.count { it == '.' } == 1) "$version.0" else version
        val newMinVersion = if (minVersion.count { it == '.' } == 1) "$minVersion.0" else minVersion

        if (Version.valueOf(newVersion) < Version.valueOf(newMinVersion)) {
            throw TaskExecutionException(this, RuntimeException("$versionType version ($newVersion) not supported. At least >= minimal version ($newMinVersion)"))
        }
    }

    fun getOsArch(): Pair<String, String> {
        return if (SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_MAC_OSX) {
            "darwin" to SystemUtils.OS_ARCH
        } else if (SystemUtils.IS_OS_LINUX) {
            "linux" to SystemUtils.OS_ARCH
        } else {
            throw TaskExecutionException(
                    this,
                    RuntimeException("Found unsupported platform (${SystemUtils.OS_NAME}/${SystemUtils.OS_ARCH})")
            )
        }
    }

    @TaskAction
    open fun run() {
        pluginExtension.pluginConfig.apply {
            listOf(dir, cacheDir, envDir, reportDir, protoDir).forEach { it.mkdirs() }
        }
    }

    inline fun <reified T : Task> task(): T? = project.tasks.findByName(taskName(T::class)) as? T

    open fun goEnvs(envs: Map<String, Any>): Map<String, Any> = task<GoEnv>()?.goEnvs(envs) ?: envs
}

typealias ExecSpecCallback = (ExecSpec) -> Unit

fun DefaultTask.exec(args: List<String>, callback: ExecSpecCallback? = null) = project.exec {
    it.environment = System.getenv().toMap()
    it.commandLine(*args.toTypedArray())

    callback?.invoke(it)
}

fun DefaultTask.exec(cmd: String, callback: ExecSpecCallback? = null) = project.exec {
    it.environment = System.getenv().toMap()
    it.commandLine("bash", "-c", cmd)

    callback?.invoke(it)
}