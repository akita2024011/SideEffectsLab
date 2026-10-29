// Fix for AGP AndroidLocationsException when both ANDROID_PREFS_ROOT and ANDROID_USER_HOME are set
try {
    val processEnv = Class.forName("java.lang.ProcessEnvironment")
    val theEnvField = processEnv.getDeclaredField("theEnvironment")
    theEnvField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val env = theEnvField.get(null) as MutableMap<String, String>
    env.remove("ANDROID_PREFS_ROOT")
    
    val theCaseInsensitiveEnvField = processEnv.getDeclaredField("theCaseInsensitiveEnvironment")
    theCaseInsensitiveEnvField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val caseInsensitiveEnv = theCaseInsensitiveEnvField.get(null) as MutableMap<String, String>
    caseInsensitiveEnv.remove("ANDROID_PREFS_ROOT")
} catch (_: Throwable) {}

pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "SideEffectsLab"
include(":app")
