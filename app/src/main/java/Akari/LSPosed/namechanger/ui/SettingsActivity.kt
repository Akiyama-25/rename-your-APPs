package Akari.LSPosed.namechanger.ui

import Akari.LSPosed.namechanger.R
import Akari.LSPosed.namechanger.utils.PrefUtils
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences(PrefUtils.PREF_NAME, android.content.Context.MODE_WORLD_READABLE)
        val lang = prefs.getString(PrefUtils.KEY_LANGUAGE, "auto")
        if (lang == "zh" || lang == "en") {
            val locale = if (lang == "zh") java.util.Locale("zh", "CN") else java.util.Locale.ENGLISH
            java.util.Locale.setDefault(locale)
            val config = newBase.resources.configuration
            config.setLocale(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.menu_settings)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = PrefUtils.PREF_NAME
            preferenceManager.sharedPreferencesMode = android.content.Context.MODE_WORLD_READABLE
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // Dynamic status updates
            val rootPref = findPreference<Preference>("pref_status_root")
            val lsposedPref = findPreference<Preference>("pref_status_lsposed")

            rootPref?.summary = if (checkRoot()) getString(R.string.status_granted) else getString(R.string.status_not_available)
            val isLsposedActive = (activity as? SettingsActivity)?.isModuleActive() == true || Akari.LSPosed.namechanger.utils.ModuleStatusChecker.isModuleActive()
            lsposedPref?.summary = if (isLsposedActive) getString(R.string.status_activated) else getString(R.string.status_not_activated)

            // Listeners for immediate applying theme
            findPreference<Preference>("pref_theme")?.setOnPreferenceChangeListener { _, newValue ->
                applyTheme(newValue as String)
                true
            }
            findPreference<Preference>("pref_language")?.setOnPreferenceChangeListener { _, _ ->
                requireActivity().recreate()
                true
            }
        }

        private fun checkRoot(): Boolean {
            return try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                process.waitFor() == 0
            } catch (e: Exception) {
                false
            }
        }

        private fun applyTheme(theme: String) {
            when (theme) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    @androidx.annotation.Keep
    fun isModuleActive(): Boolean {
        return false
    }
}

