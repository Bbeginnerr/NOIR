plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass.set("app.noir.cli.MainKt")
    applicationName = "noir"
}
