package com.pivotstir.gogradle

import com.pivotstir.gogradle.tasks.*
import org.gradle.api.Action
import org.gradle.api.Project

@GradleSupport
class GoPluginExtension(project: Project) {

    // plugin
    val pluginConfig = GoPluginConfig(project)

    // tasks
    val buildConfig = GoBuildConfig(project)
    val cleanConfig = GoCleanConfig(project)
    val depConfig = GoDepConfig(project)
    val envConfig = GoEnvConfig(project)
    val grpcConfig = GoGrpcConfig(project)
    val swagConfig = GoSwagConfig(project)
    val testConfig = GoTestConfig(project)

    // misc
    val dependenciesConfig = GoDependenciesExtension(project)

    fun build(action: Action<GoBuildConfig>) {
        action.execute(buildConfig)
    }

    fun clean(action: Action<GoCleanConfig>) {
        action.execute(cleanConfig)
    }

    fun dep(action: Action<GoDepConfig>) {
        action.execute(depConfig)
    }

    fun env(action: Action<GoEnvConfig>) {
        action.execute(envConfig)
    }

    fun grpc(action: Action<GoGrpcConfig>) {
        action.execute(grpcConfig)
    }

    fun swag(action: Action<GoSwagConfig>) {
        action.execute(swagConfig)
    }

    fun test(action: Action<GoTestConfig>) {
        action.execute(testConfig)
    }

    fun dependencies(action: Action<GoDependenciesExtension>) {
        action.execute(dependenciesConfig)
    }
}