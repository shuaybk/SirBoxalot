package com.shuayb.capstone.android.sirboxalot

import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences,
            key: String?
        ) {
            when (key) {
                "round_start_sound" -> launchSoundThread(sharedPreferences.getString(key, ""))
                "round_end_sound" -> launchSoundThread(sharedPreferences.getString(key, ""))
                "round_end_warn_sound" -> launchSoundThread(sharedPreferences.getString(key, ""))
                "rest_end_warn_sound" -> launchSoundThread(sharedPreferences.getString(key, ""))
                "inter_round_alert_sound" -> launchSoundThread(sharedPreferences.getString(key, ""))
                "pause_resume_sound" -> launchSoundThread(sharedPreferences.getString(key, ""))
                //else -> Toast.makeText(context, getString(R.string.toast_settings_change), Toast.LENGTH_LONG).show()
                "num_rounds_key" -> {
                    var newValue = sharedPreferences.getString(key, "")
                    try {
                        var integer = newValue.toInt()
                        if (integer < 1 || integer >= 1000) {
                            throw (Exception())
                        }
                    } catch (e: Exception) {
                        findPreference<EditTextPreference>(key)?.setText("10")
                        Toast.makeText(context, "Invalid entry - Please enter a number between 1 and 1000", Toast.LENGTH_LONG).show()
                    }
                }
                "round_time_key" -> {
                    var newValue = sharedPreferences.getString(key, "")
                    try {
                        var integer = newValue.toInt()
                        if (integer < 5 || integer >= 600) {
                            throw (Exception())
                        }
                    } catch (e: Exception) {
                        findPreference<EditTextPreference>(key)?.setText("180")
                        Toast.makeText(context, "Invalid entry - Please enter a number between 5 and 600", Toast.LENGTH_LONG).show()
                    }
                }
                "rest_time_key" -> {
                    var newValue = sharedPreferences.getString(key, "")
                    try {
                        var integer = newValue.toInt()
                        if (integer < 5 || integer >= 600) {
                            throw (Exception())
                        }
                    } catch (e: Exception) {
                        findPreference<EditTextPreference>(key)?.setText("60")
                        Toast.makeText(context, "Invalid entry - Please enter a number between 5 and 600", Toast.LENGTH_LONG).show()
                    }
                }
                "prepare_time_key" -> {
                    var newValue = sharedPreferences.getString(key, "")
                    try {
                        var integer = newValue.toInt()
                        if (integer < 0 || integer >= 600) {
                            throw (Exception())
                        }
                    } catch (e: Exception) {
                        findPreference<EditTextPreference>(key)?.setText("30")
                        Toast.makeText(context, "Invalid entry - Please enter a number between 0 and 600", Toast.LENGTH_LONG).show()
                    }
                }
                "round_end_warn_time_key" -> {
                    var newValue = sharedPreferences.getString(key, "")
                    try {
                        var integer = newValue.toInt()
                        if (integer < 0 || integer >= 600) {
                            throw (Exception())
                        }
                    } catch (e: Exception) {
                        findPreference<EditTextPreference>(key)?.setText("90")
                        Toast.makeText(context, "Invalid entry - Please enter a number between 0 and 600", Toast.LENGTH_LONG).show()
                    }
                }
                "rest_end_warn_time_key" -> {
                    var newValue = sharedPreferences.getString(key, "")
                    try {
                        var integer = newValue.toInt()
                        if (integer < 0 || integer >= 600) {
                            throw (Exception())
                        }
                    } catch (e: Exception) {
                        findPreference<EditTextPreference>(key)?.setText("15")
                        Toast.makeText(context, "Invalid entry - Please enter a number between 0 and 600", Toast.LENGTH_LONG).show()
                    }
                }
                "inter_round_alert_time_key" -> {
                    var newValue = sharedPreferences.getString(key, "")
                    try {
                        var integer = newValue.toInt()
                        if (integer < 0 || integer >= 600) {
                            throw (Exception())
                        }
                    } catch (e: Exception) {
                        findPreference<EditTextPreference>(key)?.setText("90")
                        Toast.makeText(context, "Invalid entry - Please enter a number between 0 and 600", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        private fun launchSoundThread(rawResValue : String?) {
            var rawResId = 0
            val soundValues = getResources().obtainTypedArray(R.array.sound_values)
            val strArray = resources.getStringArray(R.array.sound_values)

            //Determine the resource ID for the string raw resource
            for (i in 0 .. strArray.size - 1) {
                if (strArray[i] == rawResValue) {
                    rawResId = soundValues.getResourceId(i, 0)
                    break
                }
            }

            GlobalScope.launch {
                val mpSound = MediaPlayer.create(context, rawResId)
                mpSound.start()
            }
            soundValues.recycle()
        }
    }
}