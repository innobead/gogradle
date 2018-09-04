package com.pivotstir.gogradle

import com.pivotstir.gogradle.models.GoPackage
import com.pivotstir.gogradle.tasks.GoTest
import org.gradle.api.Project

class GoDependenciesExtension(val project: Project) {

    private val buildDependencies = mutableListOf<GoPackage>()
    private val testDependencies = mutableListOf<GoPackage>()

    fun build(path: String) {
        buildDependencies += GoPackage(path)
    }

    fun test(path: String) {
        testDependencies += GoPackage(path)
    }

    fun dependencies(): List<GoPackage> {
        val pkgs = mutableListOf<GoPackage>()

        project.gradle.taskGraph.apply {
            when {
                hasTask(taskName(GoTest::class)) -> {
                    pkgs.addAll(buildDependencies)
                    pkgs.addAll(testDependencies)
                }

                else -> {
                    pkgs.addAll(buildDependencies)
                }
            }
        }

        return pkgs
    }

}