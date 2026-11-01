plugins {
    java
    application
}

group = "com.minecraftai"
version = "1.2"

repositories {
    mavenCentral()
}

val lwjglVersion = "3.3.3"
val jomlVersion = "1.10.5"

dependencies {
    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-stb:${lwjglVersion}")
    implementation("org.joml:joml:$jomlVersion")
    implementation("com.google.code.gson:gson:2.10.1")

    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-windows")

    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-macos")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-macos")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-macos")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-macos")

    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-linux")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-linux")
}

application {
    mainClass.set("com.minecraftai.Game")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.minecraftai.Game"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(configurations.runtimeClasspath.map { files ->
        files.map { if (it.isDirectory) it else zipTree(it) }
    }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}