package io.github.gciatto.test0;

/**
 * The java platform.
 */
class JavaPlatform {
    private JavaPlatform() {

    }

    public static String getJava() {
        return JvmPlatform.INSTANCE.getJvm() + "-java";
    }
}
