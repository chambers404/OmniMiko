pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // MediaPipe LLM Inference + on-device GenAI artifacts
        maven { url = uri("https://storage.googleapis.com/download.tensorflow.org/maven") }
    }
}

rootProject.name = "OmniMiko"

include(":app")
include(":core:common")
include(":core:llm")
include(":core:agent")
include(":core:data")
