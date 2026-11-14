package ir.coffevista.vista_native.share

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareConfigContractTest {

    private fun resolveAppFile(relativePath: String): File {
        val direct = File(relativePath)
        if (direct.exists()) return direct
        val fromAppDir = File("app", relativePath)
        if (fromAppDir.exists()) return fromAppDir
        val fromRoot = File("../app", relativePath)
        if (fromRoot.exists()) return fromRoot
        return File(System.getProperty("user.dir") ?: ".", relativePath)
    }

    @Test
    fun fileProviderPathsExposeStorySharesDirectoryForInstagramStories() {
        val filePathsXml = resolveAppFile("src/main/res/xml/vista_file_paths.xml").readText()
        assertTrue(
            "vista_file_paths.xml must expose cache-path story_shares/ for FileProvider",
            filePathsXml.contains("""path="story_shares/""""),
        )
    }

    @Test
    fun manifestQueriesDeclareExternalAppsForRealIconsAndDirectIntents() {
        val manifestXml = resolveAppFile("src/main/AndroidManifest.xml").readText()
        val requiredPackages = listOf(
            "com.instagram.android",
            "com.instagram.barcelona",
            "com.whatsapp",
            "org.telegram.messenger",
            "com.twitter.android",
        )
        requiredPackages.forEach { pkg ->
            assertTrue(
                "AndroidManifest.xml <queries> must declare $pkg",
                manifestXml.contains("""android:name="$pkg""""),
            )
        }
    }
}
