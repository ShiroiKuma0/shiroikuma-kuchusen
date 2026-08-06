import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.xilinjia.krdb")
    id("kotlin-parcelize")
    kotlin("plugin.serialization")
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
//    stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
}

kotlin { jvmToolchain(21) }

val metaInfExcludes = listOf("DEPENDENCIES", "LICENSE", "NOTICE", "CHANGES", "README.md", "NOTICE.txt", "LICENSE.txt", "MANIFEST.MF").map { "/META-INF/$it" }

// ---- shiroikuma-kuchusen fork: signing + versioning -------------------------------------------
// Release signing reads a gitignored keystore.properties at the repo root (falling back to the
// upstream releaseStoreFile/... project properties when absent, e.g. on CI).
val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// Fork version: VERSION_NAME/VERSION_CODE track upstream (gradle.properties); BUILD_NUMBER is our
// per-build increment. versionName = "<VERSION_NAME>+<BUILD_NUMBER padded to 3 digits>" (so APK
// filenames sort in build order — "+010" after "+009", not before "+9"),
// versionCode = VERSION_CODE * 10000 + BUILD_NUMBER (plain integer; padding is text only).
val buildNumber = project.property("BUILD_NUMBER").toString().toInt()
val forkVersionName = "${project.property("VERSION_NAME")}+${buildNumber.toString().padStart(3, '0')}"
val forkVersionCode = project.property("VERSION_CODE").toString().toInt() * 10000 + buildNumber

configure<ApplicationExtension> {
    namespace = "ac.mdiq.podcini"

    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 26
        targetSdk = 37

        versionCode = forkVersionCode
        versionName = forkVersionName

        ndkVersion = "29.0.14206865"

        applicationId = project.property("APP_ID").toString()

        val apiKey = project.findProperty("podcastindexApiKey") as? String ?: ""
        val apiSecret = project.findProperty("podcastindexApiSecret") as? String ?: ""
        if (apiKey.isNotEmpty()) {
            buildConfigField("String", "PODCASTINDEX_API_KEY", "\"$apiKey\"")
            buildConfigField("String", "PODCASTINDEX_API_SECRET", "\"$apiSecret\"")
        } else {
            buildConfigField("String", "PODCASTINDEX_API_KEY", "\"QT2RYHSUZ3UC9GDJ5MFY\"")
            buildConfigField("String", "PODCASTINDEX_API_SECRET", "\"Zw2NL74ht5aCtx5zFL$#MY$##qdVCX7x37jq95Sz\"")
        }
    }

//       sourceSets {
//           getByName("main") {
//               kotlin.directories.add("../../PodciniLib/src/main/kotlin")
//               aidl.directories.add("../../PodciniLib/src/main/aidl")
//           }
//       }

    packaging {
        resources {
            excludes.addAll(metaInfExcludes)
            pickFirsts.add("/META-INF/versions/9/OSGI-INF/MANIFEST.MF")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        resValues = true
        compose = true
        aidl = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = true
        }
    }

    flavorDimensions += "market"
    productFlavors {
        create("free") {
            dimension = "market"
        }
        create("play") {
            dimension = "market"
//             applicationIdSuffix = ".play"
        }
    }

    val strictLint = project.hasProperty("strictLint")
    lint {
        checkReleaseBuilds = strictLint
        checkDependencies = strictLint

        warningsAsErrors = strictLint
        abortOnError = strictLint

        disable += listOf(
            "UnsafeOptInUsageError",
            "TypographyDashes",
            "TypographyQuotes",
            "ObsoleteLintCustomCheck",
            "RestrictedApi"
        )
    }

    signingConfigs {
        create("releaseConfig") {
            enableV1Signing = true
            enableV2Signing = true
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            } else {
                storeFile = file(project.findProperty("releaseStoreFile") as? String ?: "keystore")
                storePassword = project.findProperty("releaseStorePassword") as? String ?: "password"
                keyAlias = project.findProperty("releaseKeyAlias") as? String ?:  "alias"
                keyPassword = project.findProperty("releaseKeyPassword") as? String ?:  "password"
            }
        }
    }

    buildTypes {
        getByName("release") {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "app_name", "白い熊 空中線")
            resValue("string", "provider_authority", "${project.property("APP_ID")}.provider")
//            vcsInfo.include = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs["releaseConfig"]
        }
        getByName("debug") {
            resValue("string", "app_name", "白い熊 空中線 Debug")
            applicationIdSuffix = ".debug"
            resValue("string", "provider_authority", "${project.property("APP_ID")}.debug.provider")
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
}

androidComponents {
    val androidExt = extensions.getByType<ApplicationExtension>()
    val appName = "Podcini.A"
    val versionName = androidExt.defaultConfig.versionName ?: "0.0.0"
    onVariants { variant ->
        val variantName = variant.name
        val capitalized = variantName.replaceFirstChar { it.uppercase() }
        val copyTask = tasks.register<Copy>("export${capitalized}Apks") {
            from(variant.artifacts.get(SingleArtifact.APK)) {
                include("**/*.apk")
                eachFile {
                    name = name
                        .replace(Regex("-(release|debug)(?=\\.apk$)"), "")
                        .replace("app", appName)
                        .replace(".apk", "-$versionName.apk")
                }
                into("")
            }
            into(layout.buildDirectory.dir("exported-apks/$variantName"))
        }
        tasks.matching { it.name == "assemble$capitalized" }.configureEach { finalizedBy(copyTask) }
    }
}

// ---- shiroikuma-kuchusen fork build entrypoint ------------------------------------------------
// Build the free release, copy the signed arm64-v8a APK to ~/tmp under our filename, then bump
// BUILD_NUMBER for the next build. Values are captured at configuration time (configuration cache).
run {
    val apkOutDir = layout.buildDirectory.dir("outputs/apk/free/release")
    val targetDir = File(System.getProperty("user.home"), "tmp")
    val gradlePropsFile = rootProject.file("gradle.properties")
    val capturedVersionName = forkVersionName
    val capturedVersionCode = forkVersionCode
    val capturedBuildNumber = buildNumber

    tasks.register("buildFork") {
        description = "Build the free release APK, copy the arm64-v8a APK to ~/tmp, and bump BUILD_NUMBER."
        group = "build"
        dependsOn("assembleFreeRelease")
        doLast {
            val outputDir = apkOutDir.get().asFile
            targetDir.mkdirs()
            val apk = outputDir.listFiles { _, name -> name.endsWith(".apk") && name.contains("arm64-v8a") }
                ?.firstOrNull()
                ?: throw GradleException("No arm64-v8a APK found in $outputDir")
            val targetFile = File(targetDir, "shiroikuma-kuchusen_${capturedVersionName}_arm64-v8a.apk")
            apk.copyTo(targetFile, overwrite = true)
            println(">>> ${targetFile.absolutePath}")
            println(">>> versionCode $capturedVersionCode")

            // Auto-increment BUILD_NUMBER for the next build.
            val next = capturedBuildNumber + 1
            gradlePropsFile.writeText(
                gradlePropsFile.readText().replace("BUILD_NUMBER=$capturedBuildNumber", "BUILD_NUMBER=$next")
            )
            println(">>> BUILD_NUMBER bumped to $next")
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.2")

    implementation("androidx.annotation:annotation:1.10.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.webkit:webkit:1.17.0")
    implementation("androidx.work:work-runtime:2.11.2")

    implementation("androidx.glance:glance-appwidget:1.1.1")

    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-datasource-okhttp:1.11.0")
    implementation("androidx.media3:media3-ui:1.11.0")
    implementation("androidx.media3:media3-common:1.11.0")
    implementation("androidx.media3:media3-session:1.11.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.11.0")

    implementation("com.google.android.material:material:1.14.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${project.property("kotlin_version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
    implementation("org.jetbrains.kotlinx:atomicfu:0.33.0")

   implementation("com.github.XilinJia:PodciniLib:1.1.2")
    implementation("io.github.xilinjia.krdb:library-base:${project.property("krdb_version")}")

    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.5.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:2.11.0")

    implementation("androidx.navigation3:navigation3-runtime:1.1.6")
    implementation("androidx.navigation3:navigation3-ui:1.1.6")

    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    implementation("io.ktor:ktor-http:3.5.2")
    implementation("io.ktor:ktor-client-core:3.5.2")
    implementation("io.ktor:ktor-client-okhttp:3.5.2")
    implementation("io.ktor:ktor-client-cio:3.5.2")
    implementation("io.ktor:ktor-utils:3.5.2")

    implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
    implementation("com.fleeksoft.ksoup:ksoup-network:0.2.6")

    implementation("io.github.pdvrieze.xmlutil:core:1.0.2")
    implementation("io.github.pdvrieze.xmlutil:serialization:1.0.2")
    implementation("io.github.pdvrieze.xmlutil:core-android:1.0.2")

    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:5.4.0")
    implementation("com.squareup.okio:okio:3.18.1")

    implementation("net.dankito.readability4j:readability4j:1.0.8")

    debugImplementation("androidx.compose.ui:ui-tooling:1.12.0")
    //noinspection GradleDependency
    debugImplementation("androidx.compose.ui:ui-tooling-preview:1.11.1")

    "freeImplementation"("org.conscrypt:conscrypt-android:2.5.3")

    "playImplementation"("androidx.media3:media3-cast:1.10.0")
    "playImplementation"("com.google.android.gms:play-services-base:18.9.0")
    "playImplementation"("androidx.mediarouter:mediarouter:1.8.1")
    "playImplementation"("com.google.android.gms:play-services-cast-framework:22.2.0")
}

val copyLicenseTask = tasks.register<Copy>("copyLicense") {
    from("../LICENSE")
    into("src/main/assets/")
    rename { "$it.txt" }
}

tasks.named("preBuild") {
    dependsOn(copyLicenseTask)
}
