package com.example.ssaidhook;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;

/**
 * 配置读取 + 模块存活心跳（供 Hook 进程上报「已在 LSPosed 下运行」）。
 */
public class ConfigProvider extends ContentProvider {

    public static final String AUTHORITY = "com.example.ssaidhook.config";
    public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/configs");

    public static final String METHOD_GET = "getConfigs";
    public static final String METHOD_NOTIFY_ACTIVE = "notifyActive";
    public static final String METHOD_GET_STATUS = "getStatus";

    public static final String KEY_JSON = "json";
    public static final String KEY_LAST_ACTIVE = "last_active_ms";
    public static final String KEY_ACTIVE = "active";

    /** 心跳有效期：30 分钟内收到过 Hook 上报即视为已激活 */
    public static final long ACTIVE_TTL_MS = 30L * 60L * 1000L;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Bundle out = new Bundle();
        if (METHOD_GET.equals(method)) {
            out.putString(KEY_JSON, readConfigsJson());
        } else if (METHOD_NOTIFY_ACTIVE.equals(method)) {
            writeLastActive();
            out.putBoolean(KEY_ACTIVE, true);
        } else if (METHOD_GET_STATUS.equals(method)) {
            out.putBoolean(KEY_ACTIVE, isRecentlyActive());
            out.putLong(KEY_LAST_ACTIVE, readLastActive());
        }
        return out;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        MatrixCursor c = new MatrixCursor(new String[]{KEY_JSON, KEY_LAST_ACTIVE, KEY_ACTIVE});
        c.addRow(new Object[]{
                readConfigsJson(),
                readLastActive(),
                isRecentlyActive() ? 1 : 0
        });
        return c;
    }

    private SharedPreferences prefs() {
        Context ctx = getContext();
        if (ctx == null) return null;
        return ctx.getSharedPreferences(App.PREF_NAME, Context.MODE_PRIVATE);
    }

    private String readConfigsJson() {
        SharedPreferences sp = prefs();
        if (sp == null) return "[]";
        String json = sp.getString(App.KEY_CONFIGS, "[]");
        return json != null ? json : "[]";
    }

    private void writeLastActive() {
        SharedPreferences sp = prefs();
        if (sp == null) return;
        sp.edit().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).commit();
    }

    private long readLastActive() {
        SharedPreferences sp = prefs();
        if (sp == null) return 0L;
        return sp.getLong(KEY_LAST_ACTIVE, 0L);
    }

    private boolean isRecentlyActive() {
        long last = readLastActive();
        if (last <= 0L) return false;
        return (System.currentTimeMillis() - last) <= ACTIVE_TTL_MS;
    }

    /** 供本应用 UI 直接读取 */
    public static boolean isRecentlyActive(Context ctx) {
        if (ctx == null) return false;
        SharedPreferences sp = ctx.getSharedPreferences(App.PREF_NAME, Context.MODE_PRIVATE);
        long last = sp.getLong(KEY_LAST_ACTIVE, 0L);
        if (last <= 0L) return false;
        return (System.currentTimeMillis() - last) <= ACTIVE_TTL_MS;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/json";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
