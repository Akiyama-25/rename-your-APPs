package Akari.LSPosed.namechanger.hook;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainHook implements IXposedHookLoadPackage {
    private static final Map<String, String> NAME_MAP = new HashMap<>();
    private static final String MODULE_PACKAGE = "Akari.LSPosed.namechanger";
    private static final String PREF_NAME = "namechanger_config";
    private static volatile boolean configLoaded = false;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam == null || lpparam.packageName == null) {
            return;
        }

        String packageName = lpparam.packageName;
        boolean isSystem = "android".equals(packageName);
        boolean isLauncher = isLauncherPackage(packageName);

        if (MODULE_PACKAGE.equals(packageName)) {
            try {
                XposedHelpers.findAndHookMethod(
                    "Akari.LSPosed.namechanger.utils.ModuleStatusChecker",
                    lpparam.classLoader,
                    "isModuleActive",
                    de.robv.android.xposed.XC_MethodReplacement.returnConstant(true)
                );
                XposedBridge.log("RenameAPPs: ModuleStatusChecker hooked successfully.");
            } catch (Throwable t) {
                XposedBridge.log("RenameAPPs: ModuleStatusChecker hook failed: " + t.getMessage());
            }
        }

        if (!isSystem && !isLauncher) {
            return;
        }

        XposedBridge.log("RenameAPPs: >>> handleLoadPackage " + packageName + " system=" + isSystem + " launcher=" + isLauncher);

        loadConfig();

        XC_MethodHook renameHook = createRenameHook();
        int hookCount = 0;

        // Hook 1: PackageItemInfo.loadLabel(PackageManager)
        try {
            XposedHelpers.findAndHookMethod(
                PackageItemInfo.class,
                "loadLabel",
                PackageManager.class,
                renameHook
            );
            hookCount++;
            XposedBridge.log("RenameAPPs: Hook1 OK (PackageItemInfo.loadLabel)");
        } catch (Throwable t) {
            XposedBridge.log("RenameAPPs: Hook1 FAIL: " + t.getMessage());
        }

        // Hook 2: PackageItemInfo.loadSafeLabel(PackageManager, float, int)
        try {
            XposedHelpers.findAndHookMethod(
                PackageItemInfo.class,
                "loadSafeLabel",
                PackageManager.class,
                Float.TYPE,
                Integer.TYPE,
                renameHook
            );
            hookCount++;
            XposedBridge.log("RenameAPPs: Hook2 OK (PackageItemInfo.loadSafeLabel)");
        } catch (Throwable t) {
            XposedBridge.log("RenameAPPs: Hook2 FAIL: " + t.getMessage());
        }

        // Hook 3: PackageItemInfo.loadSafeLabel(PackageManager, float, int, int)
        try {
            XposedHelpers.findAndHookMethod(
                PackageItemInfo.class,
                "loadSafeLabel",
                PackageManager.class,
                Float.TYPE,
                Integer.TYPE,
                Integer.TYPE,
                renameHook
            );
            hookCount++;
            XposedBridge.log("RenameAPPs: Hook3 OK (PackageItemInfo.loadSafeLabel 3-param)");
        } catch (Throwable t) {
            XposedBridge.log("RenameAPPs: Hook3 FAIL: " + t.getMessage());
        }

        // Hook 4: PackageItemInfo.loadUnsafeLabel(PackageManager)
        try {
            XposedHelpers.findAndHookMethod(
                PackageItemInfo.class,
                "loadUnsafeLabel",
                PackageManager.class,
                renameHook
            );
            hookCount++;
            XposedBridge.log("RenameAPPs: Hook4 OK (PackageItemInfo.loadUnsafeLabel)");
        } catch (Throwable t) {
            XposedBridge.log("RenameAPPs: Hook4 FAIL: " + t.getMessage());
        }

        // Hook 5: ApplicationPackageManager.getApplicationLabel(ApplicationInfo)
        try {
            Class<?> appPmClass = Class.forName("android.app.ApplicationPackageManager", false, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(
                appPmClass,
                "getApplicationLabel",
                ApplicationInfo.class,
                renameHook
            );
            hookCount++;
            XposedBridge.log("RenameAPPs: Hook5 OK (ApplicationPackageManager.getApplicationLabel)");
        } catch (Throwable t) {
            XposedBridge.log("RenameAPPs: Hook5 FAIL: " + t.getMessage());
        }

        // Hook 6: PackageItemInfo.readFromParcel(Parcel)
        try {
            XposedHelpers.findAndHookMethod(
                PackageItemInfo.class,
                "readFromParcel",
                Parcel.class,
                new XC_MethodHook() {
                    private int count = 0;

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        checkAndReloadConfig();
                        PackageItemInfo pii = (PackageItemInfo) param.thisObject;
                        if (pii == null || pii.packageName == null) {
                            return;
                        }

                        String pkg = pii.packageName;
                        if (NAME_MAP.containsKey(pkg)) {
                            String newName = NAME_MAP.get(pkg);
                            boolean fieldSet = false;
                            String[] fieldNames = new String[] { "nonLocalizedLabel", "mNonLocalizedLabel", "label", "mLabel", "mName" };

                            for (String fName : fieldNames) {
                                try {
                                    XposedHelpers.setObjectField(pii, fName, newName);
                                    fieldSet = true;
                                    break;
                                } catch (Throwable ignored) {
                                }
                            }

                            pii.labelRes = 0;
                            count++;
                            if (count <= 3) {
                                if (fieldSet) {
                                    XposedBridge.log("RenameAPPs: [readFromParcel] RENAMED " + pkg + " -> " + newName);
                                } else {
                                    XposedBridge.log("RenameAPPs: [readFromParcel] WARNING: no label field found for " + pkg);
                                }
                            }
                        }
                    }
                }
            );
            hookCount++;
            XposedBridge.log("RenameAPPs: Hook6 OK (PackageItemInfo.readFromParcel)");
        } catch (Throwable t) {
            XposedBridge.log("RenameAPPs: Hook6 FAIL: " + t.getMessage());
        }

        XposedBridge.log("RenameAPPs: TOTAL hooks registered: " + hookCount);
    }

    private XC_MethodHook createRenameHook() {
        return new XC_MethodHook() {
            private int count = 0;

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                checkAndReloadConfig();
                Object obj = param.thisObject;
                String pkg = null;

                if (obj instanceof PackageItemInfo) {
                    pkg = ((PackageItemInfo) obj).packageName;
                } else if (param.args != null && param.args.length > 0 && (param.args[0] instanceof ApplicationInfo)) {
                    pkg = ((ApplicationInfo) param.args[0]).packageName;
                }

                if (pkg != null && NAME_MAP.containsKey(pkg)) {
                    String newName = NAME_MAP.get(pkg);
                    if (newName != null) {
                        param.setResult(newName);
                        count++;
                        if (count <= 5) {
                            XposedBridge.log("RenameAPPs: [loadLabel] RENAMED " + pkg + " -> " + newName);
                        }
                    }
                }
            }
        };
    }

    private static volatile String cachedSystemLauncher = null;
    private static volatile boolean sysConfigLoaded = false;

    private boolean isLauncherPackage(String pkg) {
        if (pkg == null) return false;

        if (!sysConfigLoaded) {
            try {
                XSharedPreferences sysPref = new XSharedPreferences(MODULE_PACKAGE, "sys_config");
                sysPref.reload();
                cachedSystemLauncher = sysPref.getString("system_launcher", null);
            } catch (Throwable ignored) {
            }
            sysConfigLoaded = true;
        }

        if (cachedSystemLauncher != null && cachedSystemLauncher.equals(pkg)) {
            return true;
        }

        String lower = pkg.toLowerCase();
        return lower.contains("launcher")
            || pkg.contains("com.miui.home")
            || pkg.contains("com.android.launcher")
            || pkg.contains("com.google.android.apps.nexuslauncher")
            || pkg.contains("com.huawei.android.launcher")
            || pkg.contains("com.oppo.launcher")
            || pkg.contains("com.coloros.launcher")
            || pkg.contains("com.vivo.launcher")
            || pkg.contains("com.samsung.android.app.launcher");
    }

    private static XSharedPreferences pref;
    private static long lastCheckTime = 0;

    private static void checkAndReloadConfig() {
        long now = System.currentTimeMillis();
        if (now - lastCheckTime < 1000) {
            return;
        }
        lastCheckTime = now;

        if (pref == null) {
            pref = new XSharedPreferences(MODULE_PACKAGE, PREF_NAME);
        }

        if (pref.hasFileChanged()) {
            try {
                pref.reload();
                Map<String, ?> all = pref.getAll();
                NAME_MAP.clear();
                if (all != null) {
                    for (Map.Entry<String, ?> entry : all.entrySet()) {
                        if (entry.getValue() != null && !entry.getValue().toString().trim().isEmpty()) {
                            NAME_MAP.put(entry.getKey(), entry.getValue().toString());
                        }
                    }
                }
                XposedBridge.log("RenameAPPs: Config reloaded. " + NAME_MAP.size() + " entries.");
            } catch (Throwable t) {
                XposedBridge.log("RenameAPPs: Config reload failed: " + t.getMessage());
            }
        }
    }

    private static void loadConfig() {
        if (configLoaded) return;
        try {
            checkAndReloadConfig();
            configLoaded = true;
        } catch (Throwable t) {
            XposedBridge.log("RenameAPPs: initial loadConfig failed: " + t.getMessage());
            configLoaded = true;
        }
    }
}

