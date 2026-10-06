plugins { id("com.android.application"); kotlin("android") }
android { namespace="com.historiasvivas.lab"; compileSdk=36
 defaultConfig { applicationId="com.historiasvivas.lab"; minSdk=33; targetSdk=36; versionCode=1; versionName="0.1.0" }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
}
dependencies {
 implementation(project(":engine"))
 implementation(project(":llama-android-lib"))
 implementation("androidx.appcompat:appcompat:1.7.0")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
}
