package ir.coffevista.vista_native.core.designsystem.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.view.KeyEvent

/**
 * دریافت‌کننده دکمه‌های نوتیفیکیشن، قفل‌صفحه و کلیدهای هدست (مشابه `MusicPlayerReceiver` تلگرام).
 */
class VistaMusicPlayerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        VistaAudioPlayer.attach(context.applicationContext)
        val action = intent.action.orEmpty()
        if (action == Intent.ACTION_MEDIA_BUTTON) {
            val keyEvent: KeyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            } ?: return
            if (keyEvent.action != KeyEvent.ACTION_DOWN) return

            when (keyEvent.keyCode) {
                KeyEvent.KEYCODE_HEADSETHOOK,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    if (VistaAudioPlayer.state.value.isPlaying) {
                        VistaAudioPlayer.pause()
                    } else {
                        VistaAudioPlayer.resume()
                    }
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> VistaAudioPlayer.resume()
                KeyEvent.KEYCODE_MEDIA_PAUSE -> VistaAudioPlayer.pause()
                KeyEvent.KEYCODE_MEDIA_STOP -> VistaAudioPlayer.stop()
                KeyEvent.KEYCODE_MEDIA_NEXT -> VistaAudioPlayer.next()
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> VistaAudioPlayer.previous()
            }
            return
        }

        when (action) {
            VistaMusicPlayerService.ACTION_PLAY -> VistaAudioPlayer.resume()
            VistaMusicPlayerService.ACTION_PAUSE,
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> VistaAudioPlayer.pause()
            VistaMusicPlayerService.ACTION_NEXT -> VistaAudioPlayer.next()
            VistaMusicPlayerService.ACTION_PREVIOUS -> VistaAudioPlayer.previous()
            VistaMusicPlayerService.ACTION_CLOSE -> VistaAudioPlayer.stop()
            VistaMusicPlayerService.ACTION_REPEAT -> VistaAudioPlayer.cycleRepeat()
            VistaMusicPlayerService.ACTION_SHUFFLE -> VistaAudioPlayer.toggleShuffle()
        }
    }
}
