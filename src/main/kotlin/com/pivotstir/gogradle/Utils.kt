package com.pivotstir.gogradle

import org.gradle.api.Task
import java.net.URL
import kotlin.reflect.KClass

fun String.tokens(): List<String> = this.split("""\s+""".toRegex())

fun String.toURL(): URL = URL(this)

fun <T : Task> taskName(cls: KClass<T>): String = cls.simpleName!!.decapitalize()

@Target(AnnotationTarget.CLASS,
        AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class GradleSupport