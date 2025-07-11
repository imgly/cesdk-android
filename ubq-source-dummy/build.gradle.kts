// This module is a dummy module to add the native ubq code to the project.

// Dummy java module to allow the ubq-source package search
//noinspection JavaPluginLanguageLevel
plugins { id("java-library") }

// Define source paths for the ubq-source package
sourceSets["main"].java.srcDirs(
    "$projectDir/../../../packages/",
)

// Prevent Gradle from trying to compile empty source sets
tasks.withType<JavaCompile>().configureEach {
    options.isIncremental = false
    setSource(emptyList<File>())
}
