import org.gradle.api.tasks.Copy

plugins {
    id("com.android.application") version "9.4.0"
}

val generatedRes = layout.buildDirectory.dir("generated/dramavoice-res")

val prepareDramaVoiceResources by tasks.registering(Copy::class) {
    from("dv6_icon.png")
    into(generatedRes.map { it.dir("drawable") })
}

android {
    namespace = "com.dramavoice6.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dramavoice6.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.1"
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.srcDirs(".")
            res.srcDir(generatedRes)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(prepareDramaVoiceResources)
}
