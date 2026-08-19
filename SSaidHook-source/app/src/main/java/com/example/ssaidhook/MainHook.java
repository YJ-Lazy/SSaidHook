package com.example.ssaidhook;

import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;



import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

/**
 * libxposed API 102 入口
 */
public class MainHook extends XposedModule {
    private static final String TAG = "SSaidHook";
    private static final String PREF_NAME = "ssaidhook_config";
    private static final String KEY_CONFIGS = "configs";

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, TAG, "onModuleLoaded process=" + param.getProcessName()
                + " framework=" + getFrameworkName() + " api=" + getApiVersion());
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        final String packageName = param.getPackageName();
        if (packageName == null) return;

        // 本模块自身：现代 API 下模块应用默认不被 hook，此处仅作兜底
        if ("com.example.ssaidhook".equals(packageName)) {
            return;
        }

        String fakeSsaid = findFakeSsaid(packageName);
        if (fakeSsaid == null || fakeSsaid.isEmpty()) {
            return;
        }

        try {
            ClassLoader cl = param.getClassLoader();
            if (cl == null) {
                cl = ClassLoader.getSystemClassLoader();
            }
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
                } catch (Throwable ignored) {}
                return chain.proceed();
            });

            log(Log.INFO, TAG, "Hooked ANDROID_ID for " + packageName + " -> " + ssaid);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Hook failed for " + packageName, t);
        }
    }

    private String findFakeSsaid(String packageName) {
        try {
            SharedPreferences prefs = null;
            try {
                prefs = getRemotePreferences(PREF_NAME);
            } catch (Throwable ignored) {
            }
            if (prefs == null) {
                return null;
            }
            String configsJson = prefs.getString(KEY_CONFIGS, "[]");
            if (configsJson == null || configsJson.isEmpty() || "[]".equals(configsJson.trim())) {
                String oldPkg = prefs.getString("package", "");
                String oldId = prefs.getString("android_id", "");
                if (packageName.equals(oldPkg) && oldId != null && !oldId.isEmpty()) {
                    return oldId;
                }
                return null;
            }
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
            log(Log.ERROR, TAG, "findFakeSsaid error", t);
        }
        return null;
    }
}
