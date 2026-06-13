pluginManagement {
    repositories {
//       mavenLocal() // 👈 Add this line
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
// plugins {
//     id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
// }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
//       mavenLocal() // 👈 Add this line
        google()
        mavenCentral()
        maven(url = uri("https://jitpack.io"))
    }
}

include(":app")

// includeBuild('../VistaGuide') {
//     dependencySubstitution {
//         substitute module('com.github.XilinJia.vistaguide:VistaGuide') using project(':extractor')
//     }
// }
