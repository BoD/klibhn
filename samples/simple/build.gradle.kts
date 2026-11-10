import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  alias(libs.plugins.kotlin.multiplatform)
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

        // Coroutines
        implementation(libs.kotlinx.coroutines.core)

        // Logging
        implementation(libs.klibnanolog)
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
