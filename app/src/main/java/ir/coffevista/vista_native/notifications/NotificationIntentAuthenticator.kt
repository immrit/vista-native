package ir.coffevista.vista_native.notifications

import android.content.Intent
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Stamps notification launch intents with an unforgeable per-process 256-bit secret so
 * external callers cannot spoof notification extras via exported MainActivity launch paths.
 */
internal object NotificationIntentAuthenticator {
    private const val EXTRA_INTERNAL_AUTH_TOKEN = "ir.coffevista.vista_native.extra.INTERNAL_NOTIFICATION_TOKEN"

    private val processSecret: ByteArray by lazy {
        ByteArray(32).also { SecureRandom().nextBytes(it) }
    }

    fun stamp(intent: Intent): Intent {
        intent.putExtra(EXTRA_INTERNAL_AUTH_TOKEN, processSecret.copyOf())
        return intent
    }

    fun verify(intent: Intent?): Boolean {
        val provided = intent?.getByteArrayExtra(EXTRA_INTERNAL_AUTH_TOKEN) ?: return false
        return provided.size == processSecret.size && MessageDigest.isEqual(provided, processSecret)
    }
}
