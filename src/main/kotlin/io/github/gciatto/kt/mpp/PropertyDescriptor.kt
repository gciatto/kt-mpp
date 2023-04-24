package io.github.gciatto.kt.mpp

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import java.util.Locale

data class PropertyDescriptor(
    val name: String,
    val description: String,
    val mandatory: Boolean,
    val defaultValue: Any? = null,
) {

    private var alreadyLogged: Boolean = false

    private fun helpMessage(project: Project): String {
        val result = StringBuilder()
        val rootProject = project.rootProject
        val value = defaultValue ?: "<value>"
        result.append(if (mandatory) "mandatory" else "optional").append(" property $name is missing.\n")
            .append(explanation).append("\n")
            .append("Things you can do to avoid this ").append(if (mandatory) "error" else "warning").append(":\n")
            .append("1. invoke Gradle with -P$name=$value option;\n")
            .append("2. invoke Gradle after setting ORG_GRADLE_PROJECT_$name=$value environment variable;\n")
        if (project != rootProject) {
            result.append("3. add a line containing $name=$value in the file ${project.gradlePropertiesPath};\n")
                .append("4. add a line containing $name=$value in the file ${rootProject.gradlePropertiesPath}.\n")
                .append("We recommend to do step 3 or 4 in any case, using a (possibly blank) default value, ")
        } else {
            result.append("3. add a line containing $name=$value in the file ${project.gradlePropertiesPath};\n")
                .append("We recommend (i) doing step 3 in any case, using a (possibly blank) default value, ")
        }
        result.append("or (ii) running task ")
            .append(project.tasks.getByName("generateGradlePropertiesFile").path)
            .append(" to generate the gradle.properties file automatically.")
        return result.replace("\\n".toRegex(), "\n    ")
    }

    internal fun logHelpIfNecessary(project: Project) {
        if (!alreadyLogged) {
            project.log(helpMessage(project), if (mandatory) LogLevel.ERROR else LogLevel.WARN)
            alreadyLogged = true
        }
    }

    internal val explanation: String
        get() {
            val sb = StringBuilder("Property $name (")
            sb.append(if (mandatory) "mandatory" else "optional")
            if (defaultValue != null) {
                sb.append(", default value: ")
                if (defaultValue is String) {
                    sb.append("'${defaultValue.replace("'", "\'")}'")
                } else {
                    sb.append(defaultValue)
                }
            }
            sb.append("): ")
            sb.append(description.replaceFirstChar { it.lowercase(Locale.getDefault()) })
            return sb.toString()
        }
}
