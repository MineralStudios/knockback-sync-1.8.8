plugins {
    id("com.github.gmazzo.buildconfig") version "5.6.3"
}

dependencies {
    // True compileOnly deps
    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    // Shaded in or bundled by platform-specific code
      compileOnly("com.github.retrooper:packetevents-api:2.8.0-SNAPSHOT")
      implementation("org.yaml:snakeyaml:2.4")
//    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
//    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")
    implementation("org.kohsuke:github-api:1.327") {
        exclude(group = "commons-io", module = "commons-io")
        exclude(group = "org.apache.commons", module = "commons-lang3")
    }

    implementation("org.incendo:cloud-core:2.0.0")
    implementation("org.incendo:cloud-minecraft-extras:2.0.0-SNAPSHOT")
}

buildConfig {
    buildConfigField("String", "GITHUB_REPO", "\"${project.rootProject.ext["githubRepo"]}\"")
}