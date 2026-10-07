pluginManagement {
 repositories {
  google()
  mavenCentral()
  gradlePluginPortal()
 }
}
dependencyResolutionManagement {
 repositories {
  google()
  mavenCentral()
 }
}
rootProject.name = "HistoriasVivas"
include(":engine")
if (providers.gradleProperty("includeAndroidLab").orNull == "true") {
 include(":android-lab")
 include(":llama-android-lib")
}
