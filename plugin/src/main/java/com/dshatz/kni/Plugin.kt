package com.dshatz.kni

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.gradle.internal.tasks.MergeNativeLibsTask
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.SharedLibrary
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import javax.inject.Inject

class Plugin: Plugin<Project> {

    override fun apply(target: Project) {
        val kotlin = target.extensions.getByType(KotlinMultiplatformExtension::class.java)
        kotlin.extensions.create("optionalTargets", OptionalTargetsExtension::class.java, kotlin)
        target.extensions.create("kni", KniExtension::class.java, target.objects)
    }
}

internal fun autoWire(target: Project, config: AutoWireExtension) {
    val kotlin = target.extensions.getByType(KotlinMultiplatformExtension::class.java)
    target.pluginManager.withPlugin("com.google.devtools.ksp") {
        kotlin.targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).all { androidTarget ->
            kotlin.addJniSourceSets(androidTarget, "jniJvm", "common")
            target.dependencies.add("ksp${androidTarget.name.capitalized()}", config.kspDependency)
        }
    }

    val jniNatives = (androidNativeTargets + linuxTargets + macOsTargets + windowsTargets)
    val jniJvms = listOf("android", "jvm")
    kotlin.targets.all {
        if (it.name == "metadata") return@all
        val (groupName, dependOn) = when (it.name) {
            in jniNatives -> {
                "jniNative" to "native"
            }

            in jniJvms -> {
                "jniJvm" to "common"
            }

            else -> null to null
        }
        if (groupName == null || dependOn == null) return@all
        kotlin.addJniSourceSets(it, groupName, dependOn)
        target.dependencies.add("ksp${it.name.capitalized()}", config.kspDependency)
    }
}

private fun KotlinMultiplatformExtension.addJniSourceSets(target: KotlinTarget, groupSourceSet: String, dependOn: String) {
    val (main, test) = if (dependOn == "common") {
        sourceSets.commonMain to sourceSets.commonTest
    } else if (dependOn == "native") {
        sourceSets.nativeMain to sourceSets.nativeTest
    } else error("Unknown dependOn value")
    target.compilations.all { compilation ->
        when (compilation.name) {
            "main" -> {
                val s = sourceSets.maybeCreate("${groupSourceSet}Main")
                main.configure {
                    s.dependsOn(it)
                }
                compilation.defaultSourceSet.dependsOn(s)
                project.logger.info("Adding dependency from ${compilation.defaultSourceSet.name} to ${s.name}")
            }

            "deviceTest", "test" -> {
                val s = sourceSets.maybeCreate("${groupSourceSet}Test")
                test.configure {
                    s.dependsOn(it)
                }
                compilation.defaultSourceSet.dependsOn(s)
                project.logger.info("Adding dependency from ${compilation.defaultSourceSet.name} to ${s.name}")
            }
        }
    }
}

private fun List<KotlinNativeTarget>.findSharedLibs(buildType: NativeBuildType): List<SharedLibrary> {
    return map { native ->
        native.binaries.findSharedLib(buildType)
            ?: throw Exception("Could not find target's ${native.name} shared lib (${buildType.name}) to bundle into JAR/AAR.")
    }
}

private fun Project.getNativeBuildType(): NativeBuildType {
    val requestedType = project.getKniProperty(Config.ARG_NATIVE_BUILD_TYPE) ?: "release"
    return NativeBuildType.valueOf(requestedType.uppercase())
}

sealed class BundledLibs {
    data object Prebuilt: BundledLibs()
    data class KotlinNative(val buildType: NativeBuildType): BundledLibs() {
        override fun toString(): String {
            return buildType.name.lowercase().capitalized()
        }
    }
}
private fun Project.registerBundleLibsTask(
    target: String,
    config: BundledLibs,
    configure: Action<Copy>
): TaskProvider<Copy?> {
    val taskName = when (config) {
        is BundledLibs.KotlinNative -> "bundle${target.capitalized()}NativeLibs${config}"
        is BundledLibs.Prebuilt -> "bundle${target.capitalized()}PrebuiltNativeLibs"
    }
    return tasks.register(taskName, Copy::class.java) { bundleTask ->
        bundleTask.group = "kni"
        bundleTask.description = "Aggregates all native libs for $target packaging."
        val outputDir = project.layout.buildDirectory.dir("generated/$taskName")
        bundleTask.into(outputDir)

        configure.execute(bundleTask)
    }
}

infix fun KotlinJvmTarget.bundlesNatives(
    nativeTargets: List<KotlinNativeTarget>
) {
    val buildType: NativeBuildType = project.getNativeBuildType()
    val bundleTask = project.registerBundleLibsTask(this.name, BundledLibs.KotlinNative(buildType)) { bundleTask ->
        nativeTargets.findSharedLibs(buildType).forEach { lib ->
            bundleTask.dependsOn(lib.linkTaskProvider)
            bundleTask.from(lib.outputFile) {
                it.into("lib/${lib.target.name}")
            }
        }
    }
    addLibsToJvm(bundleTask)
}


private val androidArchMap = mapOf(
    "androidNativeArm64" to "arm64-v8a",
    "androidNativeX64"   to "x86_64",
    "androidNativeArm32" to "armeabi-v7a",
    "androidNativeX86"   to "x86"
)

fun KotlinMultiplatformAndroidLibraryTarget.bundlesNatives(
    nativeTargets: List<KotlinNativeTarget>
) {
    val buildType: NativeBuildType = project.getNativeBuildType()
    val bundleTask = project.registerBundleLibsTask(this.name, BundledLibs.KotlinNative(buildType)) { bundleTask ->
        nativeTargets.findSharedLibs(buildType).forEach { lib ->
            bundleTask.dependsOn(lib.linkTaskProvider)
            bundleTask.from(lib.outputDirectory) {
                it.into("${androidArchMap[lib.target.name]}")
            }
        }
    }

    project.addLibsToAndroid(bundleTask)
}

abstract class AndroidBundlePrebuiltLibsExtension @Inject constructor(objects: ObjectFactory) {
    val arm64_v8a: ListProperty<Directory> = objects.listProperty(Directory::class.java)
    val armeabi_v7a: ListProperty<Directory> = objects.listProperty(Directory::class.java)
    val x86: ListProperty<Directory> = objects.listProperty(Directory::class.java)
    val x86_64: ListProperty<Directory> = objects.listProperty(Directory::class.java)
    val all: ListProperty<Directory> = objects.listProperty(Directory::class.java)
}

abstract class JvmBundlePrebuiltLibsExtension @Inject constructor(objects: ObjectFactory) {
    val linuxX64: ListProperty<Directory> = objects.listProperty(Directory::class.java)
    val linuxArm64: ListProperty<Directory> = objects.listProperty(Directory::class.java)
    val mingwX64: ListProperty<Directory> = objects.listProperty(Directory::class.java)
    val macosX64: ListProperty<Directory> = objects.listProperty(Directory::class.java)
    val macosArm64: ListProperty<Directory> = objects.listProperty(Directory::class.java)
}

fun KotlinJvmTarget.bundlesPrebuiltNatives(
    configure: Action<JvmBundlePrebuiltLibsExtension>
) {
    val extension = project.objects.newInstance(JvmBundlePrebuiltLibsExtension::class.java)
    configure.execute(extension)

    val collectPrebuiltLibsJvm = project.tasks.register("collectPrebuiltLibsJvm", Sync::class.java) { sync ->
        sync.group = "kni"
        sync.from(extension.linuxX64) { it.into("lib/linuxX64") }
        sync.from(extension.linuxArm64) { it.into("lib/linuxArm64") }
        sync.from(extension.mingwX64) { it.into("lib/mingwX64") }
        sync.from(extension.macosX64) { it.into("lib/macosX64") }
        sync.from(extension.macosArm64) { it.into("lib/macosArm64") }
        sync.into(project.layout.buildDirectory.dir("intermediates/collectPrebuiltLibsJvm"))
    }

    addLibsToJvm(collectPrebuiltLibsJvm)
}


fun KotlinMultiplatformAndroidLibraryTarget.bundlesPrebuiltNatives(
    configure: Action<AndroidBundlePrebuiltLibsExtension>
) {
    val extension = project.objects.newInstance(AndroidBundlePrebuiltLibsExtension::class.java)
    configure.execute(extension)

    val collectPrebuiltLibsAndroid = project.tasks.register("collectPrebuiltLibsAndroid", Sync::class.java) { sync ->
        sync.group = "kni"
        // Map flat input dirs to structured output dirs
        sync.from(extension.arm64_v8a) { it.into("arm64-v8a") }
        sync.from(extension.armeabi_v7a) { it.into("armeabi-v7a") }
        sync.from(extension.x86) { it.into("x86") }
        sync.from(extension.x86_64) { it.into("x86_64") }
        sync.from(extension.all) { it.into("") }

        sync.into(project.layout.buildDirectory.dir("intermediates/collectPrebuiltLibsAndroid"))

        sync.eachFile {
            val topLevelDir = it.path.split('/').firstOrNull()
            if (topLevelDir == null || topLevelDir !in androidArchMap.values) {
                throw GradleException(
                    "Invalid JNI structure for 'bundlesPrebuilt.all' property. " +
                            "File '${it.path} is not inside a valid ABI folder ${androidArchMap.values.joinToString()}."
                )
            }
        }
    }

    project.addLibsToAndroid(collectPrebuiltLibsAndroid)
}

/**
 * Registers outputs of given task as source of android native libs.
 */
private fun Project.addLibsToAndroid(libTaskProvider: TaskProvider<*>) {
    tasks.withType(MergeNativeLibsTask::class.java).configureEach {
        if (it.name == "mergeAndroidMainNativeLibs" || it.name == "mergeAndroidDeviceTestNativeLibs") {
            it.externalLibNativeLibs.from(libTaskProvider)
            it.externalLibNativeLibs.builtBy(libTaskProvider)
        }
    }
}

private fun KotlinJvmTarget.addLibsToJvm(libTaskProvider: TaskProvider<*>) {
    project.kotlinExtension.sourceSets.named("${name}Main").configure {
        it.resources.srcDir(libTaskProvider)
    }

    project.kotlinExtension.sourceSets.named("${name}Test").configure {
        it.resources.srcDir(libTaskProvider)
    }
}
