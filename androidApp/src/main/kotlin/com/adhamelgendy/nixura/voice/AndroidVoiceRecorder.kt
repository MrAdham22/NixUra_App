package com.adhamelgendy.nixura.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import androidx.core.content.ContextCompat
import com.adhamelgendy.nixura.domain.coach.ai.VoiceClip
import com.adhamelgendy.nixura.domain.coach.ai.VoiceRecorder
import java.io.File
import kotlin.math.sqrt

/**
 * Records a voice note as small AAC audio (mono, 16 kHz, 32 kbit/s: about 240 KB a minute) into the app's private
 * cache. The file is deleted as soon as it is read or cancelled; nothing is kept on the phone.
 */
class AndroidVoiceRecorder(context: Context) : VoiceRecorder {
    private val appContext = context.applicationContext
    private val file get() = File(appContext.cacheDir, "voice_note.m4a")
    private var recorder: MediaRecorder? = null

    /** Set by MainActivity: shows the system's microphone question. */
    @Volatile
    var launcher: (() -> Unit)? = null

    override val isSupported: Boolean
        get() = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override fun requestPermission() {
        if (!hasPermission()) launcher?.invoke()
    }

    @Synchronized
    override fun start(): Boolean {
        release()
        if (!hasPermission()) return false
        return try {
            file.delete()
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(appContext) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(16_000)
            r.setAudioEncodingBitRate(32_000)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            true
        } catch (failure: Exception) {
            release()
            false
        }
    }

    @Synchronized
    override fun level(): Float {
        val amplitude = try { recorder?.maxAmplitude ?: 0 } catch (failure: Exception) { 0 }
        // Square root makes quiet speech visible without clipping loud speech.
        return sqrt(amplitude / 32_767f).coerceIn(0f, 1f)
    }

    @Synchronized
    override fun stop(): VoiceClip? {
        val r = recorder ?: return null
        recorder = null
        val ok = try { r.stop(); true } catch (failure: Exception) { false } // stop() throws when nothing was captured
        r.release()
        return try {
            if (!ok || !file.exists() || file.length() < MIN_BYTES || file.length() > MAX_BYTES) null
            else VoiceClip(Base64.encodeToString(file.readBytes(), Base64.NO_WRAP), "audio/mp4")
        } catch (failure: Exception) {
            null
        } finally {
            file.delete()
        }
    }

    @Synchronized
    override fun cancel() {
        release()
        file.delete()
    }

    private fun release() {
        recorder?.let { r ->
            try { r.stop() } catch (failure: Exception) { /* was not recording */ }
            r.release()
        }
        recorder = null
    }

    private companion object {
        const val MIN_BYTES = 1_000L
        /** The server takes up to about 440 KB of audio; a 60 second note is about 240 KB. */
        const val MAX_BYTES = 430_000L
    }
}
