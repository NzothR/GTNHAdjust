
plugins {
    id("cn.elytra.gradle.conventions") version("1.2.1")
    id("com.gtnewhorizons.gtnhconvention")
}

// GTNR's Java 8 dependency signatures reference a JVM Downgrader record stub.
// The stub is compile-only; the game supplies its own runtime implementation.
val compileStub by sourceSets.creating
sourceSets.main { compileClasspath += compileStub.output }
tasks.named("compileJava") { dependsOn(tasks.named(compileStub.compileJavaTaskName)) }
