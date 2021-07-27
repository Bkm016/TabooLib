plugins {
    java
    kotlin("jvm") version "1.5.10"
}

repositories {
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    mavenCentral()
}

dependencies {
    compileOnly("net.md-5:bungeecord-chat:1.17-R0.1-SNAPSHOT")
    compileOnly(project(":common"))
    compileOnly(kotlin("stdlib"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}