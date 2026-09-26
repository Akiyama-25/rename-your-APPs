package Akari.LSPosed.namechanger.ui

import Akari.LSPosed.namechanger.R
import Akari.LSPosed.namechanger.databinding.ActivityMainBinding
import Akari.LSPosed.namechanger.databinding.DialogRenameBinding
import Akari.LSPosed.namechanger.utils.PrefUtils
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences(PrefUtils.PREF_NAME, android.content.Context.MODE_PRIVATE)
        val lang = prefs.getString(PrefUtils.KEY_LANGUAGE, "auto")
        if (lang == "zh" || lang == "en") {
            val locale = if (lang == "zh") Locale("zh", "CN") else Locale.ENGLISH
            Locale.setDefault(locale)
            val config = newBase.resources.configuration
            config.setLocale(locale)
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AppListAdapter
    private var allApps: List<AppItem> = emptyList()
    private var currentFilter: String = ""
    private var currentLang: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PrefUtils.getPrefs(this)
        currentLang = prefs.getString(PrefUtils.KEY_LANGUAGE, "auto") ?: "auto"

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PrefUtils.makePrefWorldReadable(this)
        setupViews()
        checkModuleStatus()
        loadInstalledApps()
    }

    override fun onResume() {
        super.onResume()
        val prefs = PrefUtils.getPrefs(this)
        val lang = prefs.getString(PrefUtils.KEY_LANGUAGE, "auto") ?: "auto"
        if (lang != currentLang) {
            recreate()
            return
        }

        val theme = prefs.getString(PrefUtils.KEY_THEME, "auto")
        when (theme) {
            "light" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            else -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        checkModuleStatus()
        loadInstalledApps()
    }

    private fun checkModuleStatus() {
        val isActivated = isModuleActive() || Akari.LSPosed.namechanger.utils.ModuleStatusChecker.isModuleActive()
        binding.cvStatus.isVisible = true
        if (isActivated) {
            binding.cvStatus.setCardBackgroundColor(com.google.android.material.color.MaterialColors.getColor(binding.cvStatus, com.google.android.material.R.attr.colorPrimaryContainer))
            binding.tvLsposedStatus.text = getString(R.string.module_activated)
            binding.tvLsposedStatus.setTextColor(com.google.android.material.color.MaterialColors.getColor(binding.tvLsposedStatus, com.google.android.material.R.attr.colorOnPrimaryContainer))
        } else {
            binding.cvStatus.setCardBackgroundColor(com.google.android.material.color.MaterialColors.getColor(binding.cvStatus, com.google.android.material.R.attr.colorErrorContainer))
            binding.tvLsposedStatus.text = getString(R.string.module_not_activated)
            binding.tvLsposedStatus.setTextColor(com.google.android.material.color.MaterialColors.getColor(binding.tvLsposedStatus, com.google.android.material.R.attr.colorOnErrorContainer))
        }
    }

    @androidx.annotation.Keep
    fun isModuleActive(): Boolean {
        return false
    }

    private fun setupViews() {
        adapter = AppListAdapter { appItem ->
            showRenameDialog(appItem)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        findViewById<android.widget.ImageView>(R.id.btnMore).setOnClickListener { view ->
            val popup = android.widget.PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_main, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_restart_launcher -> {
                        var targetPkg = PrefUtils.getSysPrefs(this).getString(PrefUtils.KEY_SYSTEM_LAUNCHER, null)
                        if (targetPkg.isNullOrEmpty()) {
                            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                            val resolveInfo = packageManager.resolveActivity(homeIntent, 0)
                            targetPkg = resolveInfo?.activityInfo?.packageName
                        }

                        if (targetPkg.isNullOrEmpty()) {
                            Toast.makeText(this, "未找到目标桌面包名，请在设置中配置", Toast.LENGTH_SHORT).show()
                            return@setOnMenuItemClickListener true
                        }

                        lifecycleScope.launch(Dispatchers.IO) {
                            val isSuccess = try {
                                val command = """
                                    pid=${'$'}(pidof $targetPkg)
                                    if [ -n "${'$'}pid" ]; then
                                        kill -9 ${'$'}pid
                                    fi
                                    /system/bin/am force-stop $targetPkg
                                """.trimIndent()

                                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                                process.outputStream.close()
                                process.waitFor() == 0
                            } catch (e: Exception) {
                                e.printStackTrace()
                                false
                            }

                            withContext(Dispatchers.Main) {
                                if (isSuccess) {
                                    Toast.makeText(this@MainActivity, R.string.toast_restart_launcher_success, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@MainActivity, "停止桌面失败：请检查 Root 授权状态", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        true
                    }
                    R.id.action_settings -> {
                        startActivity(Intent(this, SettingsActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        binding.swipeRefresh.setOnRefreshListener {
            checkModuleStatus()
            loadInstalledApps()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentFilter = s?.toString() ?: ""
                filterApps(currentFilter)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSearchClear.setOnClickListener {
            binding.etSearch.text?.clear()
        }
    }

    private fun loadInstalledApps() {
        binding.progressBar.isVisible = allApps.isEmpty()
        lifecycleScope.launch(Dispatchers.IO) {
            val pm = packageManager
            val customNames = PrefUtils.getAllCustomNames(this@MainActivity)
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val homeIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0).toMutableList()
            resolveInfos.addAll(pm.queryIntentActivities(homeIntent, 0))
            val seenPackages = mutableSetOf<String>()
            val items = mutableListOf<AppItem>()
            
            val defaultLauncherIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
            val defaultLauncherResolveInfo = pm.resolveActivity(defaultLauncherIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val currentLauncherPkg = defaultLauncherResolveInfo?.activityInfo?.packageName
            if (currentLauncherPkg != null) {
                PrefUtils.saveSystemLauncher(this@MainActivity, currentLauncherPkg)
            }

            for (resolveInfo in resolveInfos) {
                val pkg = resolveInfo.activityInfo.packageName
                if (seenPackages.add(pkg)) {
                    val appName = resolveInfo.loadLabel(pm).toString()
                    val icon = try { resolveInfo.loadIcon(pm) } catch (e: Exception) { null }
                    val customName = customNames[pkg]
                    val isSystemLauncher = (pkg == currentLauncherPkg)
                    val installTime = try { pm.getPackageInfo(pkg, 0).firstInstallTime } catch(e: Exception) { 0L }
                    items.add(AppItem(pkg, appName, icon, customName, isSystemLauncher, installTime))
                }
            }

            val allPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in allPackages) {
                val pkg = appInfo.packageName
                if (seenPackages.add(pkg) && customNames.containsKey(pkg)) {
                    val appName = appInfo.loadLabel(pm).toString()
                    val icon = try { appInfo.loadIcon(pm) } catch (e: Exception) { null }
                    val customName = customNames[pkg]
                    val isSystemLauncher = (pkg == currentLauncherPkg)
                    val installTime = try { pm.getPackageInfo(pkg, 0).firstInstallTime } catch(e: Exception) { 0L }
                    items.add(AppItem(pkg, appName, icon, customName, isSystemLauncher, installTime))
                }
            }

            withContext(Dispatchers.Main) {
                allApps = items
                binding.progressBar.isVisible = false
                binding.swipeRefresh.isRefreshing = false
                val launcherName = currentLauncherPkg ?: "未知"
                binding.tvLauncher.text = getString(R.string.current_system_launcher_format, launcherName)
                updateSummary()
                filterApps(currentFilter)
            }
        }
    }

    private fun filterApps(query: String) {
        val prefs = PrefUtils.getPrefs(this)
        val sortBy = prefs.getString(PrefUtils.KEY_SORT_BY, "app_name") ?: "app_name"
        val sortOrder = prefs.getString(PrefUtils.KEY_SORT_ORDER, "asc") ?: "asc"
        val isDesc = sortOrder == "desc"

        val filtered = if (query.isEmpty()) {
            allApps
        } else {
            val lowerQuery = query.lowercase(Locale.getDefault())
            allApps.filter {
                it.appName.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                it.packageName.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                (it.customName?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true)
            }
        }

        val comparator = java.util.Comparator<AppItem> { o1, o2 ->
            val result = when (sortBy) {
                "pkg_name" -> o1.packageName.compareTo(o2.packageName, ignoreCase = true)
                "install_time" -> o1.installTime.compareTo(o2.installTime)
                else -> o1.appName.compareTo(o2.appName, ignoreCase = true)
            }
            if (isDesc) -result else result
        }

        val configuredApps = filtered.filter { it.isConfigured }.sortedWith(comparator)
        val unconfiguredApps = filtered.filter { !it.isConfigured }.sortedWith(comparator)

        val finalList = mutableListOf<AppItem>()
        if (configuredApps.isNotEmpty()) {
            finalList.add(AppItem("", "", null, null, isHeader = true, headerText = getString(R.string.header_configured)))
            finalList.addAll(configuredApps)
        }
        if (unconfiguredApps.isNotEmpty()) {
            finalList.add(AppItem("", "", null, null, isHeader = true, headerText = getString(R.string.header_unconfigured)))
            finalList.addAll(unconfiguredApps)
        }

        adapter.submitList(finalList)
        binding.tvEmpty.isVisible = finalList.isEmpty()
    }
    
    private fun updateSummary() {
        val total = allApps.size
        val configured = allApps.count { it.isConfigured }
        binding.tvSummary.text = getString(R.string.app_count_summary, configured, total)
    }

    private fun showRenameDialog(item: AppItem) {
        val dialogBinding = DialogRenameBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogAppName.text = item.appName
        dialogBinding.tvDialogPackage.text = item.packageName
        if (item.icon != null) {
            dialogBinding.ivDialogAppIcon.setImageDrawable(item.icon)
        } else {
            dialogBinding.ivDialogAppIcon.setImageResource(R.mipmap.ic_launcher)
        }

        if (item.isConfigured) {
            dialogBinding.etNewName.setText(item.customName)
        }

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val newName = dialogBinding.etNewName.text?.toString()?.trim() ?: ""
                if (newName.isNotEmpty()) {
                    PrefUtils.setCustomName(this, item.packageName, newName)
                    notifyAppItemChanged(item.copy(customName = newName))
                    Toast.makeText(this, getString(R.string.toast_saved, item.appName), Toast.LENGTH_SHORT).show()
                } else {
                    PrefUtils.removeCustomName(this, item.packageName)
                    notifyAppItemChanged(item.copy(customName = null))
                    Toast.makeText(this, getString(R.string.toast_cleared, item.appName), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)

        if (item.isConfigured) {
            builder.setNeutralButton(R.string.action_reset) { _, _ ->
                PrefUtils.removeCustomName(this, item.packageName)
                notifyAppItemChanged(item.copy(customName = null))
                Toast.makeText(this, getString(R.string.toast_cleared, item.appName), Toast.LENGTH_SHORT).show()
            }
        }

        builder.show()
    }

    private fun notifyAppItemChanged(newItem: AppItem) {
        allApps = allApps.map {
            if (it.packageName == newItem.packageName) newItem else it
        }
        updateSummary()
        allApps = allApps.sortedWith(compareByDescending<AppItem> { it.isConfigured }.thenBy { it.appName.lowercase(Locale.getDefault()) })
        filterApps(currentFilter)
    }
}


