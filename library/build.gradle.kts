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

        // Regex
        implementation(libs.klibregexdsl)

        // Logging
        implementation(libs.klibnanolog)
      }
    }

    commonTest {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.kotlinx.io.core)
      }
    }

    jvmMain {
      dependencies {
        // Ktor OkHttp
        implementation(libs.ktor.client.okhttp)
      }
    }

    jvmTest {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.konsist)
      }
    }

    macosArm64Main {
      dependencies {
        // Ktor curl
        implementation(libs.ktor.client.curl)
      }
    }
  }
}

Librarian.module(project)
