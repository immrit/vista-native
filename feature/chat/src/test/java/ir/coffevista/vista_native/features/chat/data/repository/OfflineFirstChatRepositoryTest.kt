package ir.coffevista.vista_native.features.chat.data.repository

import ir.coffevista.vista_native.features.chat.data.remote.GroupInviteDto
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFirstChatRepositoryTest {
    @Test
    fun `created group survives a transient details enrichment failure`() = runTest {
        val result = fetchCreatedGroupInfoBestEffort {
            throw IOException("details unavailable")
        }

        assertNull(result)
    }

    @Test
    fun `created group enrichment preserves coroutine cancellation`() = runTest {
        var cancellationPropagated = false

        try {
            fetchCreatedGroupInfoBestEffort {
                throw CancellationException("cancelled")
            }
        } catch (_: CancellationException) {
            cancellationPropagated = true
        }

        assertTrue(cancellationPropagated)
    }

    @Test
    fun `group details survive a transient invite enrichment failure`() = runTest {
        val result = fetchGroupInviteBestEffort {
            throw IOException("invite unavailable")
        }

        assertNull(result)
    }

    @Test
    fun `group invite enrichment preserves coroutine cancellation`() = runTest {
        var cancellationPropagated = false

        try {
            fetchGroupInviteBestEffort {
                throw CancellationException("cancelled")
            }
        } catch (_: CancellationException) {
            cancellationPropagated = true
        }

        assertTrue(cancellationPropagated)
    }

    @Test
    fun `group invite enrichment returns the server contract`() = runTest {
        val invite = GroupInviteDto(inviteCode = "fresh", enabled = false)

        assertTrue(fetchGroupInviteBestEffort { invite } === invite)
    }
}
