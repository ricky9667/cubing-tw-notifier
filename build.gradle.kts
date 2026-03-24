plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.kotlinJpa)
    alias(libs.plugins.gradleKtlint)
}

group = "io.github.ricky9667"
version = "1.0.0"
description = "A notifier bot that checks for updates from Cubing TW website."

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.springBootStarterDataJpa)
    implementation(libs.springBootStarterWebmvc)
    implementation(libs.kotlinReflect)
    implementation(libs.jacksonModuleKotlin)
    implementation(libs.jda)
    implementation(libs.jsoup)

    developmentOnly(libs.springBootDockerCompose)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.springBootStarterDataJpaTest)
    testImplementation(libs.springBootStarterWebmvcTest)
    testImplementation(libs.kotlinTestJunit5)
    testRuntimeOnly(libs.junitPlatformLauncher)
    testRuntimeOnly(libs.h2)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

ktlint {
    version.set(libs.versions.ktlintCli)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
