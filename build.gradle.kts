plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    `maven-publish`
}

group = "io.github.brodykim.jsonlogic"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runCompatReporter") {
    group = "verification"
    description = "Run compatibility tests against compat-tables suites"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("io.github.brodykim.jsonlogic.CompatTablesReporter")
}

kotlin {
    jvmToolchain(17)
}

// JitPack publishing configuration
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "json-logic-kotlin"
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set("json-logic-kotlin")
                description.set("A pure Kotlin implementation of JsonLogic")
                url.set("https://github.com/brody-0125/json-logic-kotlin")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("brody-0125")
                        name.set("Brody Kim")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/brody-0125/json-logic-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com/brody-0125/json-logic-kotlin.git")
                    url.set("https://github.com/brody-0125/json-logic-kotlin")
                }
            }
        }
    }
}

// Ensure sources are included for better IDE support
java {
    withSourcesJar()
}
