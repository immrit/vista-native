package ir.coffevista.vista_native.features.chat.data.remote

import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class ChatUploadPolicyTest {
    private val policy = ChatUploadPolicy(now = { 1_000L })

    @Test
    fun `normal premium and blue badge limits match Flutter`() {
        assertEquals(ChatUploadPolicy.NORMAL_MAX_BYTES, policy.maxBytes(null))
        assertEquals(
            ChatUploadPolicy.PREMIUM_MAX_BYTES,
            policy.maxBytes(profile(role = "premium", days = 1)),
        )
        assertNull(
            policy.maxBytes(
                profile(
                    verified = true,
                    verificationType = "blueTick",
                ),
            ),
        )
        assertEquals(
            ChatUploadPolicy.PREMIUM_MAX_BYTES,
            policy.maxBytes(profile(verified = true, verificationType = "goldTick")),
        )
        assertEquals(
            ChatUploadPolicy.NORMAL_MAX_BYTES,
            policy.maxBytes(profile(role = "premium", days = 0)),
        )
    }

    @Test
    fun `normal account cannot cross 15MB while premium can reach 100MB`() {
        assertRejects {
            validatePdf(size = ChatUploadPolicy.NORMAL_MAX_BYTES + 1, profile = null)
        }
        assertEquals(
            AttachmentKind.DOCUMENT,
            validatePdf(
                size = ChatUploadPolicy.PREMIUM_MAX_BYTES,
                profile = profile(role = "premium", days = 2),
            ).kind,
        )
        assertRejects {
            validatePdf(
                size = ChatUploadPolicy.PREMIUM_MAX_BYTES + 1,
                profile = profile(role = "premium", days = 2),
            )
        }
    }

    @Test
    fun `content signature defeats extension and mime spoofing`() {
        assertRejects {
            policy.validate(
                fileName = "fake.jpg",
                declaredMimeType = "image/jpeg",
                declaredKind = AttachmentKind.IMAGE,
                sizeBytes = 100,
                header = "%PDF-1.7".encodeToByteArray(),
                profile = null,
            )
        }
        assertRejects {
            policy.validate(
                fileName = "real.pdf",
                declaredMimeType = "image/jpeg",
                declaredKind = AttachmentKind.DOCUMENT,
                sizeBytes = 100,
                header = "%PDF-1.7".encodeToByteArray(),
                profile = null,
            )
        }
    }

    @Test
    fun `image gif video audio voice and pdf signatures are canonicalized`() {
        assertEquals(
            AttachmentKind.IMAGE,
            validate("image.png", "image/png", AttachmentKind.IMAGE, png()).kind,
        )
        assertEquals(
            AttachmentKind.GIF,
            validate("animation.gif", "image/gif", AttachmentKind.IMAGE, "GIF89a".encodeToByteArray()).kind,
        )
        assertEquals(
            AttachmentKind.VIDEO,
            validate("clip.mp4", "video/mp4", AttachmentKind.VIDEO, ftyp("isom")).kind,
        )
        assertEquals(
            AttachmentKind.AUDIO,
            validate("song.mp3", "audio/mpeg", AttachmentKind.AUDIO, "ID3".encodeToByteArray()).kind,
        )
        assertEquals(
            AttachmentKind.VOICE,
            validate("voice.m4a", "audio/mp4", AttachmentKind.VOICE, ftyp("M4A ")).kind,
        )
        assertEquals(AttachmentKind.DOCUMENT, validatePdf(100, null).kind)
    }

    private fun validatePdf(size: Long, profile: ChatUploadProfile?) = policy.validate(
        fileName = "document.pdf",
        declaredMimeType = "application/pdf",
        declaredKind = AttachmentKind.DOCUMENT,
        sizeBytes = size,
        header = "%PDF-1.7".encodeToByteArray(),
        profile = profile,
    )

    private fun validate(
        name: String,
        mime: String,
        kind: AttachmentKind,
        header: ByteArray,
    ) = policy.validate(name, mime, kind, 100, header, null)

    private fun profile(
        role: String? = null,
        days: Int? = null,
        verified: Boolean = false,
        verificationType: String? = null,
    ) = ChatUploadProfile(
        role = role,
        accountType = null,
        isVerified = verified,
        verificationType = verificationType,
        premiumDaysRemaining = days,
        subscriptionExpiresAtEpochMillis = null,
    )

    private fun png() = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )

    private fun ftyp(brand: String): ByteArray = byteArrayOf(0, 0, 0, 20) +
        "ftyp".encodeToByteArray() + brand.padEnd(4).take(4).encodeToByteArray()

    private fun assertRejects(block: () -> Unit) {
        try {
            block()
            fail("Expected upload validation to reject the file")
        } catch (_: IllegalArgumentException) {
            Unit
        }
    }
}
