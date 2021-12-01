
plugins {
    base
}
group = "ru.fbear"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

tasks.clean {
    delete ("$rootDir/build")
}