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
include(":android-lab")
include(":llama-android-lib")
