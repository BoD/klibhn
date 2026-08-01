import com.gradleup.librarian.gradle.Librarian

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  jvm()

  macosArm64()

  sourceSets {
    commonMain {
      dependencies {
        // Ktor
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.contentNegotiation)
        implementation(libs.ktor.client.logging)
        implementation(libs.ktor.serialization.kotlinx.json)

        // Serialization
        implementation(libs.kotlinx.serialization.json)

        // Logging
        implementation(libs.klibnanolog)
      }
    }

    jvmMain {
      dependencies {
        // Ktor OkHttp
        implementation(libs.ktor.client.okhttp)
      }
    }

    macosArm64Main {
      dependencies {
        // Ktor CIO
        implementation(libs.ktor.client.darwin)
      }
    }
  }
}

Librarian.module(project)
