package com.pepkyc.cleevio.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.preference.EditTextPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import com.pepkyc.cleevio.R
import com.pepkyc.cleevio.activity.FileManagerActivity
import com.pepkyc.cleevio.activity.SelectFolderActivity

/**
 * Fragment zobrazující nastavení
 */
class SettingsFragment : PreferenceFragment() {

    private val SELECT_DEFAULT_PATH_RC = 456

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        /*Nastaví defaultní hodnotu pokud ještě žádná nebyla drříve nastavena*/
        val editTextPreference = findPreference(FileManagerActivity.DEFAULT_DIRECTORY_KEY) as EditTextPreference
        if (editTextPreference.text == null) {
            val defaultValue = Environment.getExternalStorageDirectory().path
            PreferenceManager.getDefaultSharedPreferences(activity).getString(FileManagerActivity.DEFAULT_DIRECTORY_KEY, defaultValue)
            editTextPreference.text = defaultValue
        }

        /*Slouží ke aktuálnímu zobrazení nastavené DEFAULT PATH v nastavení pod titulkem*/
        editTextPreference.summary = editTextPreference.text
        editTextPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue.toString()
            true
        }
        editTextPreference.setOnPreferenceClickListener {
            val i = Intent(activity, SelectFolderActivity::class.java)
            startActivityForResult(i, SELECT_DEFAULT_PATH_RC)
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                when (requestCode) {
                    SELECT_DEFAULT_PATH_RC -> {
                        val newPath = data?.getStringExtra(SelectFolderActivity.SELECTED_PATH)
                        val editTextPreference = findPreference(FileManagerActivity.DEFAULT_DIRECTORY_KEY) as EditTextPreference
                        editTextPreference.editText.setText(newPath)
                        editTextPreference.editText.setSelection(newPath!!.length)
                        PreferenceManager.getDefaultSharedPreferences(activity).edit().putString(FileManagerActivity.DEFAULT_DIRECTORY_KEY, newPath).commit()
                        editTextPreference.text = newPath
                    }
                }
            }
        }
    }
}