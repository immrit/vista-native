package ir.coffevista.vista_native.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VistaMediaBrowserHelpersTest {

    @Test
    fun `filterAudioItems searches title artist album and filename case-insensitively`() {
        val tracks = listOf(
            VistaAudioMediaItem(
                id = 1L,
                uriString = "content://media/external/audio/media/1",
                albumArtUriString = null,
                title = "Midnight Rain",
                artist = "Shadmehr",
                album = "Nostalgia",
                displayName = "midnight_rain.mp3",
                mimeType = "audio/mpeg",
                extension = "mp3",
                durationMs = 215_000L,
                sizeBytes = 5_200_000L,
            ),
            VistaAudioMediaItem(
                id = 2L,
                uriString = "content://media/external/audio/media/2",
                albumArtUriString = null,
                title = "بهار من",
                artist = "همایون شجریان",
                album = "آلبوم سنتی",
                displayName = "bahar.m4a",
                mimeType = "audio/mp4",
                extension = "m4a",
                durationMs = 180_000L,
                sizeBytes = 4_100_000L,
            ),
        )

        assertEquals(2, VistaMediaBrowserHelpers.filterAudioItems(tracks, "   ").size)
        assertEquals(listOf(1L), VistaMediaBrowserHelpers.filterAudioItems(tracks, "shadmehr").map { it.id })
        assertEquals(listOf(1L), VistaMediaBrowserHelpers.filterAudioItems(tracks, "RAIN").map { it.id })
        assertEquals(listOf(2L), VistaMediaBrowserHelpers.filterAudioItems(tracks, "سنتی").map { it.id })
        assertEquals(listOf(2L), VistaMediaBrowserHelpers.filterAudioItems(tracks, "bahar.m4a").map { it.id })
        assertTrue(VistaMediaBrowserHelpers.filterAudioItems(tracks, "notfound").isEmpty())
    }

    @Test
    fun `filterFileItems applies category and search query together`() {
        val files = listOf(
            VistaDeviceFileItem(
                id = 10L,
                uriString = "content://media/external/file/10",
                displayName = "Contract_2026.pdf",
                extension = "pdf",
                mimeType = "application/pdf",
                sizeBytes = 420_000L,
                dateModifiedSec = 200L,
                folderLabel = "Documents",
                isDownload = false,
            ),
            VistaDeviceFileItem(
                id = 11L,
                uriString = "content://media/external/downloads/11",
                displayName = "Invoice_Mehr.pdf",
                extension = "pdf",
                mimeType = "application/pdf",
                sizeBytes = 850_000L,
                dateModifiedSec = 300L,
                folderLabel = "Download",
                isDownload = true,
            ),
            VistaDeviceFileItem(
                id = 12L,
                uriString = "content://media/external/downloads/12",
                displayName = "Podcast.mp3",
                extension = "mp3",
                mimeType = "audio/mpeg",
                sizeBytes = 3_200_000L,
                dateModifiedSec = 250L,
                folderLabel = "Download/Telegram",
                isDownload = true,
            ),
        )

        assertEquals(
            listOf(10L, 11L, 12L),
            VistaMediaBrowserHelpers.filterFileItems(files, "", VistaFileCategoryFilter.ALL).map { it.id },
        )
        assertEquals(
            listOf(10L, 11L),
            VistaMediaBrowserHelpers.filterFileItems(files, "", VistaFileCategoryFilter.PDF_ONLY).map { it.id },
        )
        assertEquals(
            listOf(11L, 12L),
            VistaMediaBrowserHelpers.filterFileItems(files, "", VistaFileCategoryFilter.DOWNLOADS).map { it.id },
        )
        assertEquals(
            listOf(11L),
            VistaMediaBrowserHelpers.filterFileItems(files, "invoice", VistaFileCategoryFilter.DOWNLOADS).map { it.id },
        )
    }

    @Test
    fun `filterGalleryItems filters by type album and query cleanly`() {
        val items = listOf(
            VistaGalleryMediaItem(
                id = 1L,
                uriString = "content://media/external/images/media/1",
                isVideo = false,
                isGif = false,
                mimeType = "image/jpeg",
                displayName = "IMG_001.jpg",
                bucketName = "Camera",
            ),
            VistaGalleryMediaItem(
                id = 2L,
                uriString = "content://media/external/images/media/2",
                isVideo = false,
                isGif = true,
                mimeType = "image/gif",
                displayName = "funny_cat.gif",
                bucketName = "Downloads",
            ),
            VistaGalleryMediaItem(
                id = 3L,
                uriString = "content://media/external/video/media/3",
                isVideo = true,
                isGif = false,
                mimeType = "video/mp4",
                displayName = "VID_003.mp4",
                bucketName = "Camera",
                durationMs = 15_000L,
            ),
        )

        assertEquals(listOf("Camera", "Downloads"), VistaMediaBrowserHelpers.extractAlbumNames(items))
        assertEquals(
            listOf(1L, 2L),
            VistaMediaBrowserHelpers.filterGalleryItems(items, filter = VistaGalleryFilter.IMAGES).map { it.id },
        )
        assertEquals(
            listOf(3L),
            VistaMediaBrowserHelpers.filterGalleryItems(items, filter = VistaGalleryFilter.VIDEOS).map { it.id },
        )
        assertEquals(
            listOf(2L),
            VistaMediaBrowserHelpers.filterGalleryItems(items, filter = VistaGalleryFilter.GIFS).map { it.id },
        )
        assertEquals(
            listOf(1L),
            VistaMediaBrowserHelpers.filterGalleryItems(
                items,
                filter = VistaGalleryFilter.IMAGES,
                albumName = "Camera",
            ).map { it.id },
        )
    }

    @Test
    fun `duration size and extension inference format cleanly in Persian`() {
        assertEquals("۳:۰۵", VistaMediaBrowserHelpers.formatMediaDuration(185_000L))
        assertEquals("۱:۰۲:۰۳", VistaMediaBrowserHelpers.formatMediaDuration(3_723_000L))
        assertEquals("۰:۰۰", VistaMediaBrowserHelpers.formatMediaDuration(0L))

        assertEquals("۵۱۲ کیلوبایت", VistaMediaBrowserHelpers.formatFileSize(512L * 1024L))
        assertEquals("۲.۰ مگابایت", VistaMediaBrowserHelpers.formatFileSize(2L * 1024L * 1024L))

        assertEquals("pdf", VistaMediaBrowserHelpers.inferExtension("statement", "application/pdf"))
        assertEquals("mp3", VistaMediaBrowserHelpers.inferExtension("track.MP3", ""))
        assertEquals("m4a", VistaMediaBrowserHelpers.inferExtension("", "audio/m4a"))
        assertEquals("", VistaMediaBrowserHelpers.inferExtension("noextfile", ""))
        assertEquals("", VistaMediaBrowserHelpers.extensionFromFileName("statement"))
    }
}
