package de.markusfisch.android.binaryeye.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.activity.AutomatedActionsActivity
import de.markusfisch.android.binaryeye.activity.ProfilesActivity
import de.markusfisch.android.binaryeye.activity.SplashActivity
import de.markusfisch.android.binaryeye.app.hasBluetoothPermission
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.media.beepConfirm
import de.markusfisch.android.binaryeye.preference.UrlPreference
import de.markusfisch.android.binaryeye.view.setPaddingFromWindowInsets
import de.markusfisch.android.binaryeye.view.systemBarRecyclerViewScrollListener

class PreferencesFragment : PreferenceFragmentCompat() {
	private var lastProfile: String? = null

	private val changeListener = object : OnSharedPreferenceChangeListener {
		override fun onSharedPreferenceChanged(
			sharedPreferences: SharedPreferences?,
			key: String?
		) {
			key ?: return
			val preference = findPreference<Preference>(key) ?: return
			if (preference.key != PROFILE) {
				prefs.update()
			}
			when (preference.key) {
				"custom_locale" -> {
					activity?.restartApp()
					return
				}

				"beep_tone_name" -> beepConfirm()

				"send_scan_bluetooth" -> if (
					prefs.sendScanBluetooth &&
					activity?.hasBluetoothPermission() == false
				) {
					prefs.sendScanBluetooth = false
				}
			}
			setSummary(preference)
		}
	}

	override fun onCreatePreferences(state: Bundle?, rootKey: String?) {
		loadPreferences(rootKey)
	}

	private fun loadPreferences(rootKey: String? = null) {
		preferenceScreen?.sharedPreferences
			?.unregisterOnSharedPreferenceChangeListener(changeListener)

		preferenceManager.apply {
			sharedPreferencesName = prefs.profile ?: "${context.packageName}_preferences"
			sharedPreferencesMode = Context.MODE_PRIVATE
		}

		// Refresh the preferences.
		preferenceScreen = null
		setPreferencesFromResource(R.xml.preferences, rootKey)
		preferenceScreen.sharedPreferences
			?.registerOnSharedPreferenceChangeListener(changeListener)
		setSummaries(preferenceScreen)

		wireProfiles()
		wireAutomatedActions()
	}

	private fun wireProfiles() {
		findPreference<Preference>(PROFILE)?.apply {
			updateProfileSummary(this)
			onPreferenceClickListener = Preference.OnPreferenceClickListener {
				startActivity(Intent(activity, ProfilesActivity::class.java))
				true
			}
		}
	}

	private fun wireAutomatedActions() {
		findPreference<Preference>(AUTOMATED_ACTIONS)?.apply {
			updateAutomatedActionsSummary(this)
			onPreferenceClickListener = Preference.OnPreferenceClickListener {
				startActivity(
					Intent(activity, AutomatedActionsActivity::class.java)
				)
				true
			}
		}
	}

	override fun onResume() {
		super.onResume()
		if (lastProfile != prefs.profile) {
			loadPreferences()
		}
		activity?.setTitle(R.string.preferences)
		findPreference<Preference>(AUTOMATED_ACTIONS)?.let {
			updateAutomatedActionsSummary(it)
		}
		findPreference<Preference>(PROFILE)?.let {
			updateProfileSummary(it)
		}
		listView.setPaddingFromWindowInsets()
		listView.removeOnScrollListener(systemBarRecyclerViewScrollListener)
		listView.addOnScrollListener(systemBarRecyclerViewScrollListener)
	}

	override fun onPause() {
		super.onPause()
		preferenceScreen.sharedPreferences
			?.unregisterOnSharedPreferenceChangeListener(changeListener)
	}

	override fun onDisplayPreferenceDialog(preference: Preference) {
		if (preference is UrlPreference) {
			val fm = parentFragmentManager
			UrlDialogFragment.newInstance(preference.key).apply {
				setTargetFragment(this@PreferencesFragment, 0)
				show(fm, null)
			}
		} else if (preference.key == "send_scan_bluetooth_host") {
			val ac = activity ?: return
			super.onDisplayPreferenceDialog(preference)
		} else {
			super.onDisplayPreferenceDialog(preference)
		}
	}

	private fun setSummaries(screen: PreferenceGroup) {
		var i = screen.preferenceCount
		while (i-- > 0) {
			setSummary(screen.getPreference(i))
		}
	}

	private fun setSummary(preference: Preference) {
		if (preference.key == AUTOMATED_ACTIONS) {
			updateAutomatedActionsSummary(preference)
			return
		}
		when (preference) {
			is UrlPreference -> {
				preference.summary = preference.getUrl()
			}

			is ListPreference -> {
				preference.setSummary(preference.entry)
			}

			is MultiSelectListPreference -> {
				preference.summary = preference.values.joinToString(", ") {
					it.replace(Regex("_"), " ")
				}
			}

			is PreferenceGroup -> {
				setSummaries(preference)
			}
		}
	}

	private fun updateAutomatedActionsSummary(preference: Preference) {
		val count = prefs.automatedActions.size
		preference.summary = if (count == 0) {
			getString(R.string.automated_actions_none)
		} else {
			resources.getQuantityString(
				R.plurals.automated_actions_count,
				count,
				count
			)
		}
	}

	private fun updateProfileSummary(preference: Preference) {
		preference.summary = prefs.profile ?: getString(R.string.profile_default)
		lastProfile = prefs.profile
	}

	companion object {
		private const val PROFILE = "profile"
		private const val AUTOMATED_ACTIONS = "automated_actions"
	}
}

private fun Activity.restartApp() {
	val intent = Intent(this, SplashActivity::class.java)
	intent.addFlags(
		Intent.FLAG_ACTIVITY_NEW_TASK or
				Intent.FLAG_ACTIVITY_CLEAR_TASK
	)
	startActivity(intent)
	finish()
	// Restart to begin with an unmodified Locale to follow system settings.
	Runtime.getRuntime().exit(0)
}
