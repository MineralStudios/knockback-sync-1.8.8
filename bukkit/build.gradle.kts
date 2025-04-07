import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow")
}

base {
    archivesName.set("${rootProject.property("archives_base_name")}-bukkit")
}

val shadeThisThing: Configuration by configurations.creating {
    isCanBeConsumed = true
    isTransitive = true
}

// TODO migrate to only including sources sets for compile, test and javadoc tasks
tasks.withType<JavaCompile>().configureEach {
    source(project(":common").sourceSets.main.get().allSource)
}

tasks.withType<Javadoc>().configureEach {
    source(project(":common").sourceSets.main.get().allJava)
}

tasks.named<JavaCompile>("compileTestJava") {
    exclude("**/*")
}

dependencies {
    implementation(project(":common"))

    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")

    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    shadeThisThing(implementation("org.kohsuke:github-api:1.327")!!)
    compileOnly("com.github.retrooper:packetevents-spigot:2.8.0-SNAPSHOT")
//    shadeThisThing(implementation("net.kyori:adventure-platform-bukkit:4.3.4")!!)
    shadeThisThing(implementation("org.incendo:cloud-paper:2.0.0-SNAPSHOT")!!)
    shadeThisThing(implementation("org.incendo:cloud-core:2.0.0")!!)

    // Required for 1.14.4 support because gson is too old to have JosnParser.parseString()
    shadeThisThing(implementation("com.google.code.gson:gson:2.12.1")!!)
}

tasks.withType<ShadowJar> {
    configurations = listOf(shadeThisThing)
    isEnableRelocation = false
    relocationPrefix = "${project.property("maven_group")}.${project.property("archives_base_name")}.shaded"
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    from(project(":common").sourceSets.main.get().resources)
    inputs.property("version", project.version)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(
            "version" to project.version,
        )
    }
}