plugins { java }
repositories { mavenCentral() }
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
sourceSets.main { java.setSrcDirs(listOf("scripts")) }
dependencies { implementation(files(".engine/lib/engine-core.jar", ".engine/lib/gson.jar")) }
