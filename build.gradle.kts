import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.3.0-rc-190"
    
    var ktor_version: String by extra
    ktor_version = "0.9.5"
    
    var rx_kotlin_version: String by extra
    rx_kotlin_version = "2.3.0"

    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", kotlin_version))
    }
}

group = "sen.khyber"
version = "1.0-SNAPSHOT"

plugins {
    application
    java
    kotlin("jvm") version "1.3.0-rc-190"
    id("org.jetbrains.kotlin.kapt") version "1.3.0-rc-190"
}

application {
    mainClassName = "sen.khyber.columbia.directory.ColumbiaDirectory"
}

val kotlin_version: String by extra
val ktor_version: String by extra
val rx_kotlin_version: String by extra

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile(kotlin("stdlib", kotlin_version))
    testCompile("junit", "junit", "4.12")
    compile("io.reactivex.rxjava2:rxkotlin:$rx_kotlin_version")
    compile("org.jsoup:jsoup:1.11.3")
    compile("com.squareup.okhttp3:okhttp:3.11.0")
    kapt("org.litote.kmongo:kmongo-annotation-processor:3.8.3")
    compile("org.litote.kmongo:kmongo-rxjava2:3.8.3")
    compile("org.seleniumhq.selenium:selenium-java:3.14.0")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {

}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-XXLanguage:+InlineClasses")
}