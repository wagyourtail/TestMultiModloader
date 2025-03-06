import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask

plugins {
    id("java")
    id("xyz.wagyourtail.unimined") version "1.3.13"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    maven("https://maven.wagyourtail.xyz/releases")
    maven("https://repo.spongepowered.org/maven")
//    maven("https://maven.jemnetworks.com/snapshots")
    mavenCentral()
}

val fabric by sourceSets.creating
val forge by sourceSets.creating
val neoforge by sourceSets.creating

val merger by sourceSets.creating

unimined.minecraft {
    version("1.21.1")

    mappings {
        mojmap()

        devFallbackNamespace("official")
    }

    defaultRemapJar = false
}

unimined.minecraft(fabric) {
    combineWith(sourceSets.main.get())

    fabric {
        loader("0.16.10")
    }

    defaultRemapJar = true
}

unimined.minecraft(forge) {
    combineWith(sourceSets.main.get())

    minecraftForge {
        loader("52.1.0")
    }

    defaultRemapJar = true
}

unimined.minecraft(neoforge) {
    combineWith(sourceSets.main.get())

    neoForge {
        loader(129)
    }

    defaultRemapJar = true
}

tasks.withType(RemapJarTask::class.java).forEach {
    it.mixinRemap {
        disableRefmap()
    }
}

dependencies {
    val mergerImplementation by configurations.getting
    val fabricInclude by configurations.getting
    val fabricCompileOnly by configurations.getting


    fabricCompileOnly(fabricInclude(compileOnly("net.bytebuddy:byte-buddy-agent:1.17.2")!!)!!)
    fabricInclude(mergerImplementation(compileOnly("io.github.prcraftmc:class-diff:1.0-SNAPSHOT")!!)!!)
    fabricInclude(mergerImplementation(compileOnly("io.github.java-diff-utils:java-diff-utils:4.12")!!)!!)
    fabricInclude(mergerImplementation(compileOnly("com.nothome:javaxdelta:2.0.1")!!)!!)
    fabricInclude(mergerImplementation(compileOnly("trove:trove:1.0.2")!!)!!)

    compileOnly("org.spongepowered:mixin:0.8.5-SNAPSHOT")

    // commons-compress
    mergerImplementation("org.apache.commons:commons-compress:1.26.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

val mergeJars by tasks.creating(Jar::class) {
    val temp = temporaryDir.resolve("temp.jar")
    from(zipTree(temp))
    archiveClassifier = "merged"
    dependsOn("mergerClasses", "jar", "remapFabricJar", "remapNeoforgeJar", "remapForgeJar")

    doFirst {

        javaexec {
            classpath = merger.runtimeClasspath + merger.output
            mainClass = "xyz.wagyourtail.jarmerge.Merger"

            args = listOf(
                tasks.named("jar").get().outputs.files.singleFile.absolutePath,
                temp.absolutePath,
                "fabric",
                tasks.named("remapFabricJar").get().outputs.files.singleFile.absolutePath,
                "neoforge",
                tasks.named("remapNeoforgeJar").get().outputs.files.singleFile.absolutePath,
                "forge",
                tasks.named("remapForgeJar").get().outputs.files.singleFile.absolutePath
            )
        }

    }

    manifest {
        attributes(mapOf(
            "MixinConnector" to "org.example.agent.PlatformAgentHook"
        ))
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.named("processFabricResources", ProcessResources::class) {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.named("processForgeResources", ProcessResources::class) {
    inputs.property("version", project.version)

    filesMatching("META-INF/mods.toml") {
        expand("version" to project.version)
    }
}

tasks.named("processNeoforgeResources", ProcessResources::class) {
    inputs.property("version", project.version)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
}