package Akari.LSPosed.namechanger.ui

import android.graphics.drawable.Drawable

data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val customName: String?,
    val isSystemLauncher: Boolean = false,
    val installTime: Long = 0,
    val isHeader: Boolean = false,
    val headerText: String = ""
) {
    val isConfigured: Boolean
        get() = !customName.isNullOrBlank()
}
