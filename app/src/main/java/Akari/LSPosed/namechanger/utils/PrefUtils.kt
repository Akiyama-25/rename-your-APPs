package Akari.LSPosed.namechanger.utils

import android.content.Context
import android.content.SharedPreferences
import java.io.File

object PrefUtils {
    const val PREF_NAME = "namechanger_config"
    const val SYS_PREF_NAME = "sys_config"
    const val KEY_SYSTEM_LAUNCHER = "system_launcher"
    const val KEY_LANGUAGE = "pref_language"
    const val KEY_THEME = "pref_theme"
    const val KEY_SORT_BY = "pref_sort_by"
    const val KEY_SORT_ORDER = "pref_sort_order"

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_WORLD_READABLE)
    }

    fun getSysPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(SYS_PREF_NAME, Context.MODE_WORLD_READABLE)
    }

    fun saveSystemLauncher(context: Context, packageName: String) {
        getSysPrefs(context).edit().putString(KEY_SYSTEM_LAUNCHER, packageName).commit()
        makePrefWorldReadable(context)
    }

    fun getAllCustomNames(context: Context): Map<String, String> {
        val prefs = getPrefs(context)
        val result = mutableMapOf<String, String>()
        for ((key, value) in prefs.all) {
            if (value is String && value.isNotBlank()) {
                result[key] = value
            }
        }
        return result
    }

    fun setCustomName(context: Context, packageName: String, customName: String) {
        val prefs = getPrefs(context)
        prefs.edit().putString(packageName, customName.trim()).commit()
        makePrefWorldReadable(context)
    }

    fun removeCustomName(context: Context, packageName: String) {
        val prefs = getPrefs(context)
        prefs.edit().remove(packageName).commit()
        makePrefWorldReadable(context)
    }

    fun makePrefWorldReadable(context: Context) {
        try {
            val appDir = File(context.applicationInfo.dataDir)
            if (appDir.exists()) {
                appDir.setExecutable(true, false)
                appDir.setReadable(true, false)
            }
            
            val dataDir = File(context.applicationInfo.dataDir, "shared_prefs")
            val prefFile = File(dataDir, "$PREF_NAME.xml")
            val sysPrefFile = File(dataDir, "$SYS_PREF_NAME.xml")
            if (dataDir.exists()) {
                dataDir.setReadable(true, false)
                dataDir.setExecutable(true, false)
            }
            if (prefFile.exists()) {
                prefFile.setReadable(true, false)
            }
            if (sysPrefFile.exists()) {
                sysPrefFile.setReadable(true, false)
            }
        } catch (ignored: Throwable) {
        }
    }
}
