package ir.coffevista.vista_native.core.datastore

import ir.coffevista.vista_native.core.datastore.proto.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacySettingsCacheTest {
    @Test
    fun snapshotIsVisibleOnlyToTheAccountThatCreatedIt() {
        val stored = AppPreferences.newBuilder()
            .setPrivacyCacheOwnerId("account-a")
            .setPrivacyCacheIsPrivate(true)
            .setPrivacyCacheLastSeenVisibility("nobody")
            .setPrivacyCacheMessagePrivacy("friends")
            .setPrivacyCacheGroupAddPrivacy("following")
            .setPrivacyCacheReadReceipts(true)
            .setPrivacyCacheAllowProfileZoom(true)
            .build()

        val own = stored.privacySnapshotFor("account-a")

        assertTrue(own!!.isPrivate)
        assertEquals("nobody", own.lastSeenVisibility)
        assertEquals("friends", own.messagePrivacy)
        assertNull(stored.privacySnapshotFor("account-b"))
        assertNull(stored.privacySnapshotFor(""))
    }
}
