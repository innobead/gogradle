[![Travis CI](https://travis-ci.org/innobead/gogradle.svg)](https://travis-ci.org/innobead/gogradle)

**Note:** Before using this plugin, please note that there is no test coverage right now, but it will be done in near future.

# What is gogradle
A Gradle plugin supports Go module/package build lifecycle, also integrate [go modules](https://github.com/golang/go/wiki/Modules) started from Go 1.11 for dependency management.

# Installation
https://plugins.gradle.org/plugin/com.pivotstir.gogradle

```
plugins {
  id "com.pivotstir.gogradle" version "1.0.17"
}
```

# Tasks
```
Gogradle tasks
--------------
goBuild - Build Go project
goClean - Clean Go project
goDep - Resolve Go project library and 3rd party tool dependencies
goEnv - Setup Go environment
goGrpc - Generate gRPC and Protobuf code
goSwag - Generate Swagger files for Gin project, supported by github.com/swaggo/gin-swagger
goTest - Test Go project
```

# Configurations
```
go {
    // Change module path from default project name
    pluginConfig.modulePath = ""

    build {
        // extra options/flags of `go build`
        cmdArgs = []
        
        // extra envirnment variables of `go build`
        envs = ["CGO_ENABLED": 0]
        
        // crossplatform build. Default: if empty or not specified, just build for local platform
        osArches = [
            'darwin/amd64', 
            'linux/amd64', 
            'windows/amd64'
        ]
    }
    
    dep {
        // extra options/flags of `go get` for downloading package dependencies
        cmdArgs = []
            
        // extra envirnment variables of `go get` for downloading package dependencies
        envs = ["CGO_ENABLED": 0]
        
        // Protobuf version for downloading protobuf toolset. Default: "3.6.1"
        protoVersion = "3.6.1"
        
        // version of https://github.com/swaggo/swag
        swaggoVersion = "1.3.2"
    }
    
    env {
        // go version. Default: "1.11"
        version = '1.11'
        
        // use project level go installation. Default: true
        useSandbox = true
    }
    
    grpc {
        // Protobuf schemas root folder. Default: file("proto")
        protoDir = file("proto")
        
        // Append the module path as prefix path of below packages when referenced in other protobuf gnerated stub go files
        referencePackages = []
    }

    test {
        // extra options/flags of `go test`
        cmdArgs = []
            
        // extra envirnment variables of `go test`
        envs = ["CGO_ENABLED": 0]
        
        // ignore folders for testing
        ignoredDirs = ['abc']
    }

    dependencies {
        build 'github.com/golang/protobuf@v1.2.0'
        build 'google.golang.org/grpc@v1.14.0'
        build 'golang.org/x/text'
        build 'golang.org/x/net'

        test 'github.com/stretchr/testify@v1.2.2'
    }
}
```

# Go Environment
`gralde goenv`, setup Go environment in .gogradle

# gRPC Support
`gralde godep`, besides resolving package dependencies, setup environments for popular go frameworks (gRPC, gRPC gateway, Gin-Swagger)

`gralde gogrpc`, generate gRPC stubs from protobuf schemas and swagger files for gRPC gateway support

# Swagger Support
`gralde goswag`, generate swagger files for Gin-Swagger support

# Dependencies Management
`gralde godep`, resolve packages build/test dependencies in gogradle DSL, then create/update go.mod and download all dependent packages.

# Test
`gralde gotest`, run testing, also support ignored folders and different coverage reports (json, xml). XML is Cobertura compatible.

# Custom Go Task
```
import com.pivotstir.gogradle.tasks.Go

task myTask(type: Go) {
    go("help test", [:])
    go("help build", [:])
}
```
