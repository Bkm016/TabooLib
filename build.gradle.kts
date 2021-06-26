allprojects {
    group = "taboolib"
    version = "6.0.0"

    tasks.withType<Jar> {
        destinationDirectory.set(file("$rootDir/build/libs"))
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}