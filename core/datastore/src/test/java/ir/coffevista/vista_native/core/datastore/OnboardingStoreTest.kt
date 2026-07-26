package ir.coffevista.vista_native.core.datastore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStoreTest {
    @Test
    fun completionOnlySurvivesForTheCurrentOnboardingVersion() {
        assertTrue(isCurrentOnboardingCompletion(true, "1.0.0"))
        assertFalse(isCurrentOnboardingCompletion(true, "0.9.0"))
        assertFalse(isCurrentOnboardingCompletion(false, "1.0.0"))
    }
}
