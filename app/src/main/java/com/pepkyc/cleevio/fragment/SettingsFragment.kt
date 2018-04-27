package com.pepkyc.cleevio.fragment

import android.os.Bundle
import android.os.Environment
import android.preference.EditTextPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import com.pepkyc.cleevio.R
import com.pepkyc.cleevio.activity.FileBrowserActivity

/**
 * Fragment zobrazující nastavení
 */
class SettingsFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.preferences)

        /*Nastaví defaultní hodnotu pokud ještě žádná nebyla drříve nastavena*/
        val editTextPreference = findPreference(FileBrowserActivity.DEFAULT_DIRECTORY_KEY) as EditTextPreference
        if (editTextPreference.text == null) {
            val defaultValue = Environment.getExternalStorageDirectory().path
            PreferenceManager.getDefaultSharedPreferences(activity).getString(FileBrowserActivity.DEFAULT_DIRECTORY_KEY, defaultValue)
            editTextPreference.text = defaultValue
        }

        /*Slouží ke aktuálnímu zobrazení nastavené DEFAULT PATH v nastavení pod titulkem*/
        editTextPreference.summary = editTextPreference.text
        editTextPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue.toString()
            true
        }
}
}