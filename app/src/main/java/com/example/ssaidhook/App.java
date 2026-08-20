package com.example.ssaidhook;

import android.app.Application;

/**
 * 无 libxposed service 依赖，保证任意机型/AGP 可编译、可打开。
 */
public class App extends Application {

    public static final String PREF_NAME = "ssaidhook_config";
    public static final String KEY_CONFIGS = "configs";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static boolean isModuleEnabledInLsp() {
        return false;
    }

    public static void tryBindServiceSafe() {
        // no-op
    }

    public static void syncConfigsToRemote(String configsJson) {
        // no-op：配置由 ConfigProvider 提供给 Hook
    }
}
