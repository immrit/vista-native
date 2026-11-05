package ir.coffevista.vista_native.core.model.note

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class SharedProfileNote(
    val userId: String,
    val content: String,
    val createdAt: String? = null,
    val expiresAt: String? = null,
)

object ProfileNoteBridge {
    private val _ownNote = MutableStateFlow<SharedProfileNote?>(null)
    val ownNote: StateFlow<SharedProfileNote?> = _ownNote.asStateFlow()

    fun updateOwnNote(note: SharedProfileNote?) {
        _ownNote.value = note
    }

    fun clear() {
        _ownNote.value = null
    }
}

object ProfileNotePrompts {
    val prompts = listOf(
        "به چی فکر می‌کنی؟",
        "از کجا چه خبر؟",
        "امروز در چه حالی؟",
        "حرف دلت رو بنویس...",
        "یه حس یا فکر رو به اشتراک بذار...",
        "الان چه حسی داری؟",
        "چه خبر از دنیای خودت؟",
        "یه جمله ناب بنویس...",
        "نظرت درباره امروز چیه؟",
        "در چه فکری هستی؟",
        "یک اتفاق خوب امروز؟",
        "قهوه یا چای؟ حالت چطوره؟ ☕",
        "یه حس قشنگ رو ثبت کن...",
        "شاید کسی منتظر شنیدنش باشه...",
        "انرژی امروزت چطوره؟ ✨",
    )

    val quickChips = listOf(
        "قهوه تایم ☕",
        "مشغول کار 💻",
        "در حال استراحت 🍃",
        "حالم خوبه 💫",
        "موزیک تایم 🎵",
        "فکر و خیال... 💭",
    )

    fun randomPrompt(): String = prompts[Random.nextInt(prompts.size)]
}
