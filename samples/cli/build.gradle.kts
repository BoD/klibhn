import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose)
  alias(libs.plugins.kotlin.compose)
}

kotlin {
  jvm {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    binaries {
      executable {
        mainClass.set("MainKt")
      }
    }
  }

  macosArm64 {
    binaries {
      executable()
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        // Library
        implementation(project(":klibhn"))

        // Logging
        implementation(libs.klibnanolog)

        implementation(libs.mosaic.runtime)
        implementation(libs.jetbrains.androidx.lifecycle.viewmodelCompose)
        implementation(libs.kotlinx.datetime)
      }
    }

    jvmMain {
      dependencies {
        // Logging
        implementation(libs.logback)
      }
    }
  }
}

// Workaround for `installJvmDist` failing with "Entry lib/lifecycle-runtime-desktop-2.11.0.jar is a duplicate but no duplicate handling
// strategy has been set".
// The reason we need this is because `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose-desktop` (a dependency of mosaic)
// depends on both `org.jetbrains.androidx.lifecycle:lifecycle-runtime-desktop` and `androidx.lifecycle:lifecycle-runtime-desktop`
// which produce jars with the same name ¯\_(ツ)_/¯
tasks.withType<AbstractCopyTask>().configureEach {
  eachFile {
    if (
      file.path.contains("/org.jetbrains.androidx.lifecycle/") &&
      name.endsWith(".jar")
    ) {
      exclude()
    }
  }
}
