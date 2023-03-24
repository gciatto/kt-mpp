package io.github.gciatto.test0;

class JavaPlatform {
    private JavaPlatform() {

    }

    public static String getJava() {
        return JvmPlatform.INSTANCE.getJvm() + "-java";
    }
}
