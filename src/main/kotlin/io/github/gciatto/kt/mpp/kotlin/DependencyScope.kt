package io.github.gciatto.kt.mpp.kotlin

import org.gradle.api.artifacts.dsl.DependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.gradle.kotlin.dsl.kotlin as kotlinDependency

sealed interface DependencyScope {
    fun api(dependency: Any)
    fun implementation(dependency: Any)

    fun test(dependency: Any)

    fun kotlin(module: String, version: String? = null): Any

    companion object {
        @JvmStatic
        fun of(handler: DependencyHandler): DependencyScope =
            GradleDependencyHandler2DependencyScopeAdapter(handler)

        @JvmStatic
        fun of(handler: KotlinDependencyHandler): DependencyScope =
            KotlinDependencyHandler2DependencyScopeAdapter(handler)
    }
}

private class GradleDependencyHandler2DependencyScopeAdapter(
    private val handler: DependencyHandler,
) : DependencyScope {
    override fun api(dependency: Any) {
        handler.add("api", dependency)
    }

    override fun implementation(dependency: Any) {
        handler.add("implementation", dependency)
    }

    override fun test(dependency: Any) {
        handler.add("testImplementation", dependency)
    }

    override fun kotlin(module: String, version: String?): Any =
        handler.kotlinDependency(module, version)
}

private class KotlinDependencyHandler2DependencyScopeAdapter(
    private val handler: KotlinDependencyHandler,
) : DependencyScope {
    override fun api(dependency: Any) {
        handler.api(dependency)
    }

    override fun implementation(dependency: Any) {
        handler.implementation(dependency)
    }

    override fun test(dependency: Any) {
        handler.implementation(dependency)
    }

    override fun kotlin(module: String, version: String?): Any =
        handler.kotlin(module, version)
}
