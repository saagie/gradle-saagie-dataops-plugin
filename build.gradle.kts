val junitVersion = "5.5.1"
val okthttp = "4.2.2"
val apacheTika = "1.22"
val validationApiVersion = "2.0.0.Final"
val hibernateValidatorVersion = "6.0.2.Final"
val javaxElVersion = "3.0.0"
val glassfishElVersion = "2.2.6"
val byteBuddyVersion = "1.10.3"
val httpclient = "4.5.12"

val nexusUsername: String by project
val nexusPassword: String by project
val version: String by project
val previousVersion: String by project

plugins {
    id("net.researchgate.release") version "2.8.1"
    id("io.codearte.nexus-staging") version "0.21.1"
    id("com.gradle.plugin-publish") version "0.11.0"
    id("org.sonarqube") version "2.8"
    id("com.adarshr.test-logger") version "2.0.0"
    id("java-gradle-plugin")
    id("de.marcphilipp.nexus-publish") version "0.4.0"
    groovy
    maven
    signing
    jacoco
}

group = "io.saagie"
description = "Gradle plugin to manage Saagie's DataFabric jobs with gradle."

repositories {
    jcenter()
    mavenCentral()
}

configurations {
    testCompile {
        exclude(module = "groovy-all")
    }
}

dependencies {
    compileOnly(gradleApi())
    implementation("com.squareup.okhttp3:okhttp:$okthttp")
    compile("org.apache.tika", "tika-core", apacheTika)
    compile("org.apache.httpcomponents", "httpclient", httpclient)
    compile("org.threeten", "threeten-extra", "1.5.0")
    compile("net.lingala.zip4j", "zip4j", "2.6.1")
    compile("com.squareup.okio", "okio", "2.8.0")
    implementation("javax.validation:validation-api:$validationApiVersion")
    implementation("org.hibernate.validator:hibernate-validator:$hibernateValidatorVersion")
    implementation("org.hibernate.validator:hibernate-validator-annotation-processor:$hibernateValidatorVersion")
    implementation("javax.el:javax.el-api:$javaxElVersion")
    implementation("org.glassfish.web:javax.el:$glassfishElVersion")
    implementation("net.bytebuddy:byte-buddy:$byteBuddyVersion")

    testImplementation(gradleTestKit())
    testImplementation("org.spockframework:spock-core:1.3-groovy-2.4")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okthttp")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("junit:junit:4.12")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$junitVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    from(sourceSets.main.get().allJava)
    archiveClassifier.set("sources")
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    from(tasks.javadoc)
    archiveClassifier.set("javadoc")
}

release {
    preTagCommitMessage = "[ci skip] pre tag commit:"
    newVersionCommitMessage = "[ci skip] new version commit:"
}

tasks {
    val replaceVersionInFiles by creating {
        doLast {
            val readMe = File("README.md")
            val regex = "id\\s\"io\\.saagie\\.gradle\\-saagie\\-dataops\\-plugin\"\\sversion\\s\"([0-9\\.]+)\""
            readMe.writeText(regex.toRegex().replace(readMe.readText(), "id \"io.saagie.gradle-saagie-dataops-plugin\" version \"${version}\""))
            val properties = File("gradle.properties")
            properties.writeText(properties.readText().replace(previousVersion, version.replace("-SNAPSHOT", "")))
        }
    }
    val prepareDocCommit by creating(Exec::class) {
        dependsOn(replaceVersionInFiles)
        description = "Replace version number in documentation"
        commandLine = listOf("git", "add", "README.md", "gradle.properties")
    }
    val commitVersion by creating(Exec::class) {
        dependsOn(prepareDocCommit)
        description = "Commit version"
        commandLine = listOf("git", "commit", "-m", "[ci skip] replace readme version.")
    }
    val updateVersion by getting {
        dependsOn(commitVersion)
    }

    val publish by getting

    val publishPlugins by getting {
        dependsOn(publish)
    }
    val afterReleaseBuild by getting {
        dependsOn(publishPlugins)
    }
}

nexusPublishing {
    repositories {
        sonatype {
            username.set(nexusUsername)
            password.set(nexusPassword)
        }
    }
}

publishing {
    publications {
        val plugin = create<MavenPublication>("pluginMaven") {
            groupId = group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

            pom {
                url.set("https://github.com/saagie/gradle-saagie-dataops-plugin")
                name.set(project.name)
                description.set(project.description)

                scm {
                    url.set("scm:git@github.com:saagie/gradle-saagie-dataops-plugin.git")
                    connection.set("scm:git@github.com:saagie/gradle-saagie-dataops-plugin.git")
                    developerConnection.set("scm:git@github.com:saagie/gradle-saagie-dataops-plugin.git")
                }

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("decampsrenan")
                        name.set("Renan Decamps")
                        url.set("https://github.com/decampsrenan")
                    }
                }
            }
        }
        signing {
            isRequired = signatory != null
            sign(plugin)
        }
    }
}
nexusStaging {
    username = nexusUsername
    password = nexusPassword
    delayBetweenRetriesInMillis = 30000
}

gradlePlugin {
    plugins {
        create("saagiePlugin") {
            id = "io.saagie.gradle-saagie-dataops-plugin"
            implementationClass = "io.saagie.plugin.DataOpsPlugin"
        }
    }
    isAutomatedPublishing = false
}

pluginBundle {
    website = "https://www.saagie.com/"
    vcsUrl = "https://github.com/saagie/gradle-saagie-dataops-plugin"
    description = project.description
    tags = listOf("saagie", "data fabric")

    (plugins) {
        "saagiePlugin" {
            id = "io.saagie.gradle-saagie-dataops-plugin"
            displayName = "Gradle Saagie plugin"
        }
    }
}

testlogger {
    setTheme("mocha")
}
