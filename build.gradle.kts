import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
    kotlin("jvm") version "1.2.70"
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.10.0"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.2.70"
    maven
}

group = "com.pivotstir"
version = "1.0.17"

repositories {
    jcenter()
    mavenLocal()
    mavenCentral()
}

dependencies {
    compile(gradleApi())

    compile(kotlin("stdlib"))
    compile(kotlin("reflect"))
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.25.0")

    compile("org.rauschig:jarchivelib:0.8.0")
    compile("com.github.zafarkhaja:java-semver:0.9.0")
    compile("org.apache.commons:commons-lang3:3.7")
    compile("commons-io:commons-io:2.6")
    compile("com.squareup.okhttp3:okhttp:3.11.0")
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

gradlePlugin {
    plugins {
        create("goPlugin") {
            id = "$group.gogradle"
            displayName = "gogradle, Golang plugin"
            description = "A Golang plugin for building, testing, dependency management and popular frameworks (gRPC, Gin, Swagger, ...) supported"
            implementationClass = "com.pivotstir.gogradle.GoPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/innobead/gogradle"
    vcsUrl = "https://github.com/innobead/gogradle.git"
    tags = listOf("go", "grpc", "protobuf")

    (plugins) {
        "goPlugin" {}
    }
}

allOpen {
    annotation("com.pivotstir.gogradle.GradleSupport")
}

afterEvaluate {
    val publishKey = System.getenv("GRADLE_PUBLISH_KEY")
    val publishSecret = System.getenv("GRADLE_PUBLISH_SECRET")

    if (publishKey != null && publishSecret != null) {
        System.setProperty("gradle.publish.key", publishKey)
        System.setProperty("gradle.publish.secret", publishSecret)
    }
}
