package de.markusfisch.android.binaryeye.view

import android.content.Context
import android.media.AudioManager
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.media.beepConfirm
import de.markusfisch.android.binaryeye.media.beepError

fun Context.scanFeedback() {
	if (prefs.beep && !isSilent()) {
		beepConfirm()
	}
}

fun Context.errorFeedback() {
	if (prefs.beep && !isSilent()) {
		beepError()
	}
}

private fun Context.isSilent(): Boolean {
	val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
	return when (am.ringerMode) {
		AudioManager.RINGER_MODE_SILENT,
		AudioManager.RINGER_MODE_VIBRATE -> true

		else -> false
	}
}
