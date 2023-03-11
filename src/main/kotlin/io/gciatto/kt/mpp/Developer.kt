package io.gciatto.kt.mpp

import io.gciatto.kt.mpp.Organization.Companion.getOrg
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPomDeveloperSpec

data class Developer(val name: String, val url: String?, val email: String?, val org: Organization?) {

    fun toPerson() = mutableMapOf<String, Any?>(
        "name" to name,
        "url" to url,
        "email" to email,
    )

    override fun toString(): String =
        name + (email?.let { " <$email>" } ?: "") +
                (url?.let { " (homepage: $url)" } ?: "") +
                (org?.let { " @ $org" } ?: "")

    fun applyTo(maven: MavenPomDeveloperSpec) {
        maven.developer { dev ->
            dev.name.set(name)
            email?.let { dev.email.set(it) }
            url?.let { dev.url.set(it) }
            org?.applyTo(dev)
        }
    }

    companion object {
        fun Project.getDev(key: String): Developer {
            val name = property("${key}Name")?.toString() ?: error("Missing property ${key}Name")
            val url = findProperty("${key}Url")?.toString()
            val email = findProperty("${key}Email")?.toString()
            val orgKey = findProperty("${key}Org")?.toString()
            val org = orgKey?.let { getOrg(it) }
            return Developer(name, url, email, org)
        }

        fun Project.getAllDevs(): Set<Developer> =
            properties.keys.asSequence()
                .filter { it.startsWith("developer") && it.endsWith("Name") }
                .map { it.replace("Name", "") }
                .distinct()
                .map { getDev(it) }
                .toSet()
    }
}
