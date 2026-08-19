package com.example.ssaidhook;

import android.app.Application;
import android.content.SharedPreferences;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

/**
 * 绑定 LSPosed / libxposed Service，用于：
 * 1. 判断框架是否已连接（替代旧版 self-hook 检测）
 * 2. 将配置写入 RemotePreferences 供目标进程读取
 */
public class App extends Application implements XposedServiceHelper.OnServiceListener {

    public static final String PREF_NAME = "ssaidhook_config";
    public static final String KEY_CONFIGS = "configs";

    private static volatile XposedService sService;

    public static XposedService getService() {
        return sService;
    }

    /** 框架是否已绑定（模块在 LSPosed 中启用时通常会回调） */
    public static boolean isFrameworkBound() {
        return sService != null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            XposedServiceHelper.registerListener(this);
        } catch (Throwable ignored) {
            // 无框架时不影响普通打开
        }
    }

    @Override
    public void onServiceBind(XposedService service) {
        sService = service;
    }

    @Override
    public void onServiceDied(XposedService service) {
        if (sService == service) {
            sService = null;
        }
    }

    /**
     * 将本地配置同步到框架 RemotePreferences（Hook 侧 getRemotePreferences 可读）
     */
    public static void syncConfigsToRemote(String configsJson) {
        XposedService svc = sService;
        if (svc == null) return;
        try {
            SharedPreferences remote = svc.getRemotePreferences(PREF_NAME);
            if (remote != null) {
                remote.edit().putString(KEY_CONFIGS, configsJson).commit();
            }
        } catch (Throwable ignored) {
        }
    }
}
