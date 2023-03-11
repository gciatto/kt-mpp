package io.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPomDeveloper

data class Organization(val name: String, val url: String?) {

    fun applyTo(developer: MavenPomDeveloper) {
        developer.organization.set(name)
        url?.let { developer.organizationUrl.set(it) }
    }

    override fun toString(): String =
        name + (url?.let { " ($it)" } ?: "")

    companion object {
        fun Project.getOrg(key: String): Organization {
            val name = property("${key}Name")?.toString() ?: error("Missing property ${key}Name")
            val url = findProperty("${key}Url")?.toString()
            return Organization(name, url)
        }
    }
}
