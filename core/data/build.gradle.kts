plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.portfoy.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:calc"))
    implementation(project(":core:network"))
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
