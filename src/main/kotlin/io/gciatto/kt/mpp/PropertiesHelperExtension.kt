package io.gciatto.kt.mpp

open class PropertiesHelperExtension : DefaultProperties {
    private val propertiesImpl: MutableMap<String, PropertyDescriptor> = mutableMapOf()
    val properties: Map<String, PropertyDescriptor>
        get() = propertiesImpl.toMap()

    var overwriteGradlePropertiesFile: Boolean = false

    private fun addProperty(
        name: String,
        description: String,
        mandatory: Boolean,
        defaultValue: Any? = null
    ) = PropertyDescriptor(name, description, mandatory, defaultValue).also { addProperty(it) }

    internal fun addProperty(property: PropertyDescriptor) {
        propertiesImpl[property.name] = property
    }

    internal fun addMandatoryProperty(name: String, description: String, defaultValue: Any? = null) =
        addProperty(name, description, true, defaultValue)

    internal fun addOptionalProperty(name: String, description: String, defaultValue: Any? = null) =
        addProperty(name, description, false, defaultValue)

    private val allProperties: Sequence<PropertyDescriptor>
        get() = propertiesImpl.values.asSequence().distinct()

    fun generateExplanatoryText(): String = allProperties.joinToString("\n") { it.explanation + "." }

    fun generateGradlePropertiesText(): String =
        allProperties.joinToString("\n\n", postfix = "\n") {
            "# ${it.description}\n${it.name}=${it.defaultValue ?: ""}"
        }
}
