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


        implementation("com.jakewharton.mosaic:mosaic-runtime:0.18.0")
        implementation(libs.jetbrains.androidx.lifecycle.viewmodelCompose)
        implementation(libs.kotlinx.datetime)
      }
    }
  }
}

//tasks.withType<Tar> {
//  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//}
//
//tasks.withType<Zip> {
//  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//}
//
//tasks.withType<Copy> {
//  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//}

