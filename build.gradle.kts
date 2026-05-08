import com.google.protobuf.gradle.id
import org.gradle.api.plugins.quality.Pmd

plugins {
    java
    id("org.springframework.boot") version "3.5.11"
    id("io.spring.dependency-management") version "1.1.7"
    pmd
    jacoco
    id("com.google.protobuf") version "0.10.0"
}

group = "id.ac.ui.cs.advprog"
version = "0.0.1-SNAPSHOT"
description = "bidmart-auth-service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE")
    implementation(platform("io.grpc:grpc-bom:1.81.0"))
    implementation("com.google.guava:guava:33.4.8-jre")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-stub")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // auth + db
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("org.postgresql:postgresql")

    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // add: in-memory DB for tests
    testRuntimeOnly("com.h2database:h2")

    // Untuk mail ngirim kode verifikasi
    implementation("org.springframework.boot:spring-boot-starter-mail")

    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

pmd {
    // jangan pakai ruleset default Gradle
    ruleSets = emptyList()
    isConsoleOutput = true

    // penting untuk Java 21 (classfile major 65)
    toolVersion = "7.11.0"
}

// set ignoreFailures di task PMD (bukan di extension pmd {})
tasks.withType<Pmd>().configureEach {
    ignoreFailures = false
    exclude("**/generated/**")
    exclude("**/build/generated/**")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named<Pmd>("pmdMain") {
    source = fileTree("src/main/java")
    ruleSetFiles = files("$rootDir/config/pmd/ruleset.xml")
}

tasks.named<Pmd>("pmdTest") {
    source = fileTree("src/test/java")
    ruleSetFiles = files("$rootDir/config/pmd/ruleset-test.xml")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.9"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.81.0"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
            }
        }
    }
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }
}

