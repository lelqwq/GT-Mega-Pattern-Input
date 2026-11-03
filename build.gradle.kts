
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// 发布文件名使用当前 RELEASE_VERSION。
base {
    archivesName.set("gt-mega-pattern-input")
}

val nativeVerificationAsm by configurations.creating
dependencies {
    add(nativeVerificationAsm.name, "org.ow2.asm:asm:9.8")
    add(nativeVerificationAsm.name, "org.ow2.asm:asm-tree:9.8")
}

val verifyNativeReuse by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    classpath = nativeVerificationAsm + sourceSets.test.get().runtimeClasspath
    mainClass.set("com.lelqwq.gtmpi.NativeReuseSelfTest")
}
tasks.check { dependsOn(verifyNativeReuse) }

// Regression checks use the explicit JavaExec task instead of a test-framework dependency.
tasks.test { failOnNoDiscoveredTests = false }

val verifyNativeMixinServer by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    classpath = nativeVerificationAsm + sourceSets.test.get().runtimeClasspath
    mainClass.set("com.lelqwq.gtmpi.NativeMixinApplyTest")
}
val verifyNativeMixinClient by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    classpath = nativeVerificationAsm + sourceSets.test.get().runtimeClasspath
    mainClass.set("com.lelqwq.gtmpi.NativeMixinApplyTest")
    args("client")
}

listOf(verifyNativeMixinServer, verifyNativeMixinClient).forEach { verification ->
    verification.configure {
        providers.gradleProperty("gtmpiRuntimeGtJar").orNull?.let { runtimeGt ->
            dependsOn(tasks.named("reobfJar"))
            systemProperty("gtmpi.runtimeGtJar", runtimeGt)
            systemProperty("gtmpi.productionJar", tasks.jar.get().archiveFile.get().asFile.absolutePath)
        }
    }
}
tasks.check { dependsOn(verifyNativeMixinServer, verifyNativeMixinClient) }
