package com.example.ssaidhook;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

/**
 * libxposed API 102。
 * - 任意被注入进程：向本模块 ConfigProvider 上报心跳 → UI 显示「已激活」
 * - 本模块进程：Hook isModuleActive() → true
 * - 目标应用：Hook ANDROID_ID
 */
public class MainHook extends XposedModule {
    private static final String TAG = "SSaidHook";
    private static final String SELF_PKG = "com.example.ssaidhook";

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, TAG, "onModuleLoaded process=" + param.getProcessName()
                + " framework=" + getFrameworkName() + " api=" + getApiVersion());
        notifyModuleActive();
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        final String packageName = param.getPackageName();
        if (packageName == null) return;

        // 只要 Hook 能跑到这里，说明模块已在 LSPosed 中启用
        notifyModuleActive();

        if (SELF_PKG.equals(packageName)) {
            hookSelfActive(param);
            return;
        }

        String fakeSsaid = findFakeSsaid(packageName);
        if (fakeSsaid == null || fakeSsaid.isEmpty()) return;

        try {
            ClassLoader cl = param.getClassLoader();
            if (cl == null) cl = ClassLoader.getSystemClassLoader();
            Class<?> secureClass = Class.forName("android.provider.Settings$Secure", false, cl);
            Method getString = secureClass.getDeclaredMethod(
                    "getString",
                    android.content.ContentResolver.class,
                    String.class
            );

            final String ssaid = fakeSsaid;
            hook(getString).intercept(chain -> {
                try {
                    if (chain.getArgs() != null && chain.getArgs().size() >= 2
                            && Settings.Secure.ANDROID_ID.equals(chain.getArg(1))) {
                        return ssaid;
                    }
                } catch (Throwable ignored) {
                }
                return chain.proceed();
            });

            log(Log.INFO, TAG, "Hooked ANDROID_ID for " + packageName + " -> " + ssaid);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Hook failed for " + packageName, t);
        }
    }

    /** 向模块 App 上报：我正在 LSPosed 下运行 */
    private void notifyModuleActive() {
        try {
            android.app.Application app = null;
            try {
                Class<?> at = Class.forName("android.app.ActivityThread");
                app = (android.app.Application) at.getMethod("currentApplication").invoke(null);
            } catch (Throwable ignored) {
            }
            if (app == null) return;
            ContentResolver cr = app.getContentResolver();
            cr.call(ConfigProvider.URI, ConfigProvider.METHOD_NOTIFY_ACTIVE, null, null);
            log(Log.INFO, TAG, "notifyActive ok");
        } catch (Throwable t) {
            log(Log.WARN, TAG, "notifyActive failed: " + t.getMessage());
        }
    }

    private void hookSelfActive(PackageReadyParam param) {
        try {
            ClassLoader cl = param.getClassLoader();
            if (cl == null) return;
            Class<?> activityClz = Class.forName(
                    "com.example.ssaidhook.MainActivity", false, cl);
            Method m = activityClz.getDeclaredMethod("isModuleActive");
            hook(m).intercept(chain -> Boolean.TRUE);
            log(Log.INFO, TAG, "Hooked isModuleActive() -> true");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "hookSelfActive failed", t);
        }
    }

    private String findFakeSsaid(String packageName) {
        String configsJson = loadConfigsJson();
        if (configsJson == null || configsJson.isEmpty()) return null;
        try {
            JSONArray arr = new JSONArray(configsJson);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String pkg = obj.optString("package", "");
                String id = obj.optString("android_id", "");
                if (packageName.equals(pkg) && !id.isEmpty()) {
                    return id;
                }
            }
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "parse configs", t);
        }
        return null;
    }

    private String loadConfigsJson() {
        try {
            android.content.SharedPreferences prefs = getRemotePreferences(App.PREF_NAME);
            if (prefs != null) {
                String json = prefs.getString(App.KEY_CONFIGS, null);
                if (json != null && !json.isEmpty() && !"[]".equals(json.trim())) {
                    return json;
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            android.app.Application app = null;
            try {
                Class<?> at = Class.forName("android.app.ActivityThread");
                app = (android.app.Application) at.getMethod("currentApplication").invoke(null);
            } catch (Throwable ignored) {
            }
            if (app != null) {
                ContentResolver cr = app.getContentResolver();
                try {
                    Bundle b = cr.call(ConfigProvider.URI, ConfigProvider.METHOD_GET, null, null);
                    if (b != null) {
                        String json = b.getString(ConfigProvider.KEY_JSON);
                        if (json != null && !json.isEmpty()) return json;
                    }
                } catch (Throwable ignored) {
                }
                try (Cursor c = cr.query(ConfigProvider.URI, null, null, null, null)) {
                    if (c != null && c.moveToFirst()) {
                        int idx = c.getColumnIndex(ConfigProvider.KEY_JSON);
                        if (idx >= 0) {
                            String json = c.getString(idx);
                            if (json != null && !json.isEmpty()) return json;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "loadConfigsJson", t);
        }
        return "[]";
    }
}
