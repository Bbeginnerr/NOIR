plugins {
    kotlin("jvm")
    application
}

// Override in CI: ./gradlew :cli:installDist -PnoirVersion=0.2.0
val noirVersion = providers.gradleProperty("noirVersion").orElse("0.2.2")
version = noirVersion.get()

tasks.jar {
    manifest {
        attributes["Implementation-Title"] = "NOIR"
        attributes["Implementation-Version"] = noirVersion.get()
    }
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass.set("app.noir.cli.MainKt")
    applicationName = "noir"
}
