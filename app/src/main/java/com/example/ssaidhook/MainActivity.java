package com.example.ssaidhook;

import android.app.Activity;
import android.app.AlertDialog;
import android.provider.Settings;
import android.net.Uri;
import android.content.Intent;
import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final String PREF_NAME = "ssaidhook_config";
    private static final String KEY_CONFIGS = "configs";

    private static final int COLOR_PRIMARY = 0xFF1976D2;
    private static final int COLOR_PRIMARY_DARK = 0xFF0D47A1;
    private static final int COLOR_ACCENT = 0xFF26A69A;
    private static final int COLOR_BG = 0xFFF5F7FA;
    private static final int COLOR_CARD = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFF212121;
    private static final int COLOR_TEXT_SEC = 0xFF757575;
    private static final int COLOR_DIVIDER = 0xFFE0E0E0;
    private static final int COLOR_SELECTED = 0xFFE3F2FD;
    private static final int COLOR_DANGER = 0xFFE53935;
    private static final int COLOR_WARNING = 0xFFFB8C00;

    private EditText filterEdit;
    private EditText pkgEdit;
    private EditText idEdit;
    private LinearLayout listContainer;
    private TextView emptyHint;
    private TextView countLabel;
    private JSONArray configs = new JSONArray();
    private int selectedIndex = -1;
    private String currentFilter = "";



    public boolean isModuleActive() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            buildMainUi();
        } catch (Throwable t) {
            try {
                TextView tv = new TextView(this);
                tv.setText("SSaidHook 启动异常:\n" + t.getClass().getSimpleName()
                        + "\n" + (t.getMessage() == null ? "" : t.getMessage())
                        + "\n\n可卸载重装。未启用 LSPosed 也应能打开本页。");
                tv.setTextColor(0xFF212121);
                tv.setPadding(48, 48, 48, 48);
                setContentView(tv);
            } catch (Throwable ignored) {
            }
            return;
        }
    }

    private void buildMainUi() {
        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);

        // Header（左侧标题，右上角联系方式）
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(COLOR_PRIMARY);
        header.setPadding(dp(20), dp(28), dp(20), dp(16));

        LinearLayout headerTop = new LinearLayout(this);
        headerTop.setOrientation(LinearLayout.HORIZONTAL);
        headerTop.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText("SSaidHook");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        titleCol.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("多应用 ANDROID_ID (SSAID) 配置");
        subtitle.setTextSize(13);
        subtitle.setTextColor(0xCCFFFFFF);
        subtitle.setPadding(0, dp(4), 0, 0);
        titleCol.addView(subtitle);
        headerTop.addView(titleCol);

        // 右上角「联系」
        TextView contactBtn = new TextView(this);
        try {
            try {
            contactBtn.setText(getString(R.string.contact_btn));
        } catch (Throwable e) {
            contactBtn.setText("QQ群");
        }
        } catch (Exception e) {
            contactBtn.setText("QQ群");
        }
        contactBtn.setTextSize(14);
        contactBtn.setTypeface(null, Typeface.BOLD);
        contactBtn.setTextColor(Color.WHITE);
        contactBtn.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable contactBg = roundedBg(0x33FFFFFF, 16);
        contactBtn.setBackground(contactBg);
        contactBtn.setOnClickListener(v -> showContactDialog());
        headerTop.addView(contactBtn);

        header.addView(headerTop);
        root.addView(header);

        // ===== LSPosed 状态条（不阻止打开，仅提示） =====
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(16));
        content.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Search row
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);

        filterEdit = makeEditText("搜索已配置的包名…");
        filterEdit.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        filterEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                currentFilter = s.toString().trim().toLowerCase(Locale.ROOT);
                refreshList();
            }
        });
        searchRow.addView(filterEdit);

        Button searchPkgBtn = makeButton("选应用", COLOR_ACCENT);
        searchPkgBtn.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        searchPkgBtn.setOnClickListener(v -> showPackagePicker());
        searchRow.addView(searchPkgBtn);
        content.addView(searchRow);

        countLabel = new TextView(this);
        countLabel.setTextSize(12);
        countLabel.setTextColor(COLOR_TEXT_SEC);
        countLabel.setPadding(0, dp(8), 0, dp(4));
        content.addView(countLabel);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(0, 0, 0, dp(8));
        scroll.addView(listContainer);
        content.addView(scroll);

        emptyHint = new TextView(this);
        emptyHint.setText("暂无配置\n点击下方「添加」或「选应用」开始");
        emptyHint.setTextColor(COLOR_TEXT_SEC);
        emptyHint.setGravity(Gravity.CENTER);
        emptyHint.setTextSize(14);
        emptyHint.setPadding(dp(16), dp(40), dp(16), dp(40));

        // Input card
        LinearLayout inputCard = makeCard();
        inputCard.setOrientation(LinearLayout.VERTICAL);
        inputCard.setPadding(dp(14), dp(14), dp(14), dp(14));

        TextView inputTitle = new TextView(this);
        inputTitle.setText("添加 / 编辑配置");
        inputTitle.setTextSize(14);
        inputTitle.setTypeface(null, Typeface.BOLD);
        inputTitle.setTextColor(COLOR_TEXT);
        inputTitle.setPadding(0, 0, 0, dp(10));
        inputCard.addView(inputTitle);

        pkgEdit = makeEditText("目标包名 (如 com.tencent.mm)");
        inputCard.addView(pkgEdit);

        idEdit = makeEditText("SSAID (16位十六进制)");
        LinearLayout.LayoutParams idLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        idLp.topMargin = dp(8);
        idEdit.setLayoutParams(idLp);
        inputCard.addView(idEdit);

        LinearLayout btnRow1 = new LinearLayout(this);
        btnRow1.setOrientation(LinearLayout.HORIZONTAL);
        btnRow1.setPadding(0, dp(12), 0, 0);

        Button addBtn = makeButton("添加 / 更新", COLOR_PRIMARY);
        addBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        addBtn.setOnClickListener(v -> addOrUpdate());
        btnRow1.addView(addBtn);

        View spacer1 = new View(this);
        spacer1.setLayoutParams(new LinearLayout.LayoutParams(dp(8), 1));
        btnRow1.addView(spacer1);

        Button randomBtn = makeButton("随机 SSAID", COLOR_ACCENT);
        randomBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        randomBtn.setOnClickListener(v -> {
            String randomId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            idEdit.setText(randomId);
        });
        btnRow1.addView(randomBtn);
        inputCard.addView(btnRow1);

        LinearLayout btnRow2 = new LinearLayout(this);
        btnRow2.setOrientation(LinearLayout.HORIZONTAL);
        btnRow2.setPadding(0, dp(8), 0, 0);

        Button delBtn = makeButton("删除选中", COLOR_DANGER);
        delBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        delBtn.setOnClickListener(v -> deleteSelected());
        btnRow2.addView(delBtn);

        View spacer2 = new View(this);
        spacer2.setLayoutParams(new LinearLayout.LayoutParams(dp(8), 1));
        btnRow2.addView(spacer2);

        Button clearBtn = makeButton("清空全部", 0xFF757575);
        clearBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        clearBtn.setOnClickListener(v -> confirmClear());
        btnRow2.addView(clearBtn);
        inputCard.addView(btnRow2);

        content.addView(inputCard);

        // Save
        Button saveBtn = makeButton("保存所有配置（立即生效）", COLOR_PRIMARY_DARK);
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        saveLp.topMargin = dp(12);
        saveBtn.setLayoutParams(saveLp);
        saveBtn.setOnClickListener(v -> saveAll());
        content.addView(saveBtn);

        // Force-stop row
        LinearLayout forceRow = new LinearLayout(this);
        forceRow.setOrientation(LinearLayout.HORIZONTAL);
        forceRow.setPadding(0, dp(8), 0, 0);

        Button forceOneBtn = makeButton("结束选中应用", COLOR_WARNING);
        forceOneBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        forceOneBtn.setOnClickListener(v -> forceStopSelected());
        forceRow.addView(forceOneBtn);

        View spacer3 = new View(this);
        spacer3.setLayoutParams(new LinearLayout.LayoutParams(dp(8), 1));
        forceRow.addView(spacer3);

        Button forceAllBtn = makeButton("结束全部目标", COLOR_DANGER);
        forceAllBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        forceAllBtn.setOnClickListener(v -> forceStopAll());
        forceRow.addView(forceAllBtn);
        content.addView(forceRow);

        TextView tip = new TextView(this);
        tip.setText("提示：保存后请在 LSPosed 勾选对应应用作用域。\n「结束后台」无需 Root；如需完整强停可跳转系统应用详情页手动操作。");
        tip.setTextSize(11);
        tip.setTextColor(COLOR_TEXT_SEC);
        tip.setPadding(0, dp(10), 0, 0);
        content.addView(tip);

        root.addView(content);
        setContentView(root);

        loadConfigs();
        refreshList();

        // 未启用时仅显示状态条；说明对话框改为点击状态条再弹出，避免启动竞态
        // （不再自动弹窗，防止部分机型 Dialog 导致异常）
        }

    // ========== UI Helpers ==========

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private GradientDrawable roundedBg(int color, float radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp((int) radiusDp));
        return gd;
    }

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        card.setLayoutParams(lp);
        GradientDrawable bg = roundedBg(COLOR_CARD, 12);
        bg.setStroke(dp(1), COLOR_DIVIDER);
        card.setBackground(bg);
        return card;
    }

    private EditText makeEditText(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setSingleLine(true);
        et.setTextSize(14);
        et.setTextColor(COLOR_TEXT);
        et.setHintTextColor(0xFF9E9E9E);
        et.setPadding(dp(12), dp(12), dp(12), dp(12));
        et.setBackground(roundedBg(0xFFF0F0F0, 8));
        return et;
    }

    private Button makeButton(String text, int bgColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(13);
        btn.setAllCaps(false);
        btn.setPadding(dp(12), dp(10), dp(12), dp(10));
        btn.setBackground(roundedBg(bgColor, 8));
        btn.setMinHeight(0);
        btn.setMinimumHeight(0);
        return btn;
    }

    // ========== Data ==========

    private void loadConfigs() {
        try {
            SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            String json = sp.getString(KEY_CONFIGS, "[]");
            try {
                configs = new JSONArray(json != null ? json : "[]");
            } catch (Exception e) {
                configs = new JSONArray();
            }
            if (configs.length() == 0) {
                String oldPkg = sp.getString("package", "");
                String oldId = sp.getString("android_id", "");
                if (oldPkg != null && !oldPkg.isEmpty() && oldId != null && !oldId.isEmpty()) {
                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("package", oldPkg);
                        obj.put("android_id", oldId);
                        configs.put(obj);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            configs = new JSONArray();
        }
    }

    private void refreshList() {
        listContainer.removeAllViews();
        selectedIndex = -1;

        List<Integer> visibleIndices = new ArrayList<>();
        for (int i = 0; i < configs.length(); i++) {
            try {
                JSONObject obj = configs.getJSONObject(i);
                String pkg = obj.optString("package", "");
                String id = obj.optString("android_id", "");
                if (!currentFilter.isEmpty()) {
                    String lower = (pkg + " " + id).toLowerCase(Locale.ROOT);
                    if (!lower.contains(currentFilter)) continue;
                }
                visibleIndices.add(i);
            } catch (Exception ignored) {}
        }

        countLabel.setText("已配置 " + configs.length() + " 项" +
                (currentFilter.isEmpty() ? "" : "（筛选后 " + visibleIndices.size() + " 项）"));

        if (visibleIndices.isEmpty()) {
            listContainer.addView(emptyHint);
            return;
        }

        for (int idx : visibleIndices) {
            try {
                JSONObject obj = configs.getJSONObject(idx);
                final int index = idx;
                String pkg = obj.optString("package", "");
                String id = obj.optString("android_id", "");

                LinearLayout card = makeCard();
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(14), dp(12), dp(14), dp(12));
                card.setOnClickListener(v -> selectItem(index, card));

                TextView pkgTv = new TextView(this);
                pkgTv.setText(pkg);
                pkgTv.setTextSize(14);
                pkgTv.setTypeface(null, Typeface.BOLD);
                pkgTv.setTextColor(COLOR_TEXT);
                card.addView(pkgTv);

                TextView idTv = new TextView(this);
                idTv.setText("SSAID: " + id);
                idTv.setTextSize(12);
                idTv.setTextColor(COLOR_TEXT_SEC);
                idTv.setPadding(0, dp(4), 0, 0);
                card.addView(idTv);

                listContainer.addView(card);
            } catch (Exception ignored) {}
        }
    }

    private void selectItem(int index, LinearLayout cardView) {
        selectedIndex = index;
        try {
            JSONObject obj = configs.getJSONObject(index);
            pkgEdit.setText(obj.optString("package", ""));
            idEdit.setText(obj.optString("android_id", ""));
            Toast.makeText(this, "已选中，可编辑 / 结束进程 / 删除", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            selectedIndex = -1;
        }

        for (int i = 0; i < listContainer.getChildCount(); i++) {
            View child = listContainer.getChildAt(i);
            if (child instanceof LinearLayout) {
                GradientDrawable bg = roundedBg(COLOR_CARD, 12);
                bg.setStroke(dp(1), COLOR_DIVIDER);
                child.setBackground(bg);
            }
        }
        GradientDrawable selectedBg = roundedBg(COLOR_SELECTED, 12);
        selectedBg.setStroke(dp(2), COLOR_PRIMARY);
        cardView.setBackground(selectedBg);
    }

    private void addOrUpdate() {
        String pkg = pkgEdit.getText().toString().trim();
        String id = idEdit.getText().toString().trim();

        if (pkg.isEmpty()) {
            Toast.makeText(this, "请输入目标包名", Toast.LENGTH_SHORT).show();
            return;
        }
        if (id.isEmpty()) {
            Toast.makeText(this, "请输入 SSAID", Toast.LENGTH_SHORT).show();
            return;
        }
        if (id.length() != 16) {
            Toast.makeText(this, "SSAID 建议为 16 位（当前 " + id.length() + " 位）", Toast.LENGTH_SHORT).show();
        }

        try {
            for (int i = 0; i < configs.length(); i++) {
                if (i == selectedIndex) continue;
                JSONObject obj = configs.getJSONObject(i);
                if (pkg.equals(obj.optString("package", ""))) {
                    Toast.makeText(this, "该包名已存在，请先选中后更新或删除旧配置", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            JSONObject obj = new JSONObject();
            obj.put("package", pkg);
            obj.put("android_id", id);

            if (selectedIndex >= 0 && selectedIndex < configs.length()) {
                configs.put(selectedIndex, obj);
                Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();
            } else {
                configs.put(obj);
                Toast.makeText(this, "已添加", Toast.LENGTH_SHORT).show();
            }
            pkgEdit.setText("");
            idEdit.setText("");
            selectedIndex = -1;
            refreshList();
        } catch (Exception e) {
            Toast.makeText(this, "操作失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteSelected() {
        if (selectedIndex < 0 || selectedIndex >= configs.length()) {
            Toast.makeText(this, "请先点击列表中的一项进行选择", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < configs.length(); i++) {
                if (i != selectedIndex) {
                    newArr.put(configs.getJSONObject(i));
                }
            }
            configs = newArr;
            pkgEdit.setText("");
            idEdit.setText("");
            selectedIndex = -1;
            Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
            refreshList();
        } catch (Exception e) {
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmClear() {
        if (configs.length() == 0) {
            Toast.makeText(this, "列表已空", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("确认清空")
                .setMessage("确定删除全部 " + configs.length() + " 条配置吗？")
                .setPositiveButton("清空", (d, w) -> {
                    configs = new JSONArray();
                    pkgEdit.setText("");
                    idEdit.setText("");
                    selectedIndex = -1;
                    refreshList();
                    Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveAll() {
        try {
            SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            sp.edit()
                    .putString(KEY_CONFIGS, configs.toString())
                    .remove("package")
                    .remove("android_id")
                    .commit();
            try {
                App.syncConfigsToRemote(configs.toString());
            } catch (Throwable ignored) {}
            // LSPosed 通过 xposedsharedprefs 读取本应用 MODE_PRIVATE 配置，无需 WORLD_READABLE
            Toast.makeText(this, "已保存 " + configs.length() + " 条配置\n请确保 LSPosed 作用域包含对应应用",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ========== 结束目标应用进程（无需 Root） ==========

    private void forceStopSelected() {
        String pkg = null;
        if (selectedIndex >= 0 && selectedIndex < configs.length()) {
            try {
                pkg = configs.getJSONObject(selectedIndex).optString("package", "");
            } catch (Exception ignored) {}
        }
        if (pkg == null || pkg.isEmpty()) {
            pkg = pkgEdit.getText().toString().trim();
        }
        if (pkg.isEmpty()) {
            Toast.makeText(this, "请先选中列表中的应用，或在输入框填写包名", Toast.LENGTH_SHORT).show();
            return;
        }
        final String target = pkg;
        new AlertDialog.Builder(this)
                .setTitle("结束应用进程")
                .setMessage("目标：\n" + target + "\n\n"
                        + "• 结束后台进程：无需 Root，立即尝试\n"
                        + "• 打开应用详情：可在系统设置中手动「强制停止」")
                .setPositiveButton("结束后台", (d, w) -> doKillBackground(Collections.singletonList(target)))
                .setNeutralButton("打开应用详情", (d, w) -> openAppDetails(target))
                .setNegativeButton("取消", null)
                .show();
    }

    private void forceStopAll() {
        if (configs.length() == 0) {
            Toast.makeText(this, "没有已配置的目标应用", Toast.LENGTH_SHORT).show();
            return;
        }
        final List<String> pkgs = new ArrayList<>();
        for (int i = 0; i < configs.length(); i++) {
            try {
                String p = configs.getJSONObject(i).optString("package", "");
                if (!p.isEmpty()) pkgs.add(p);
            } catch (Exception ignored) {}
        }
        if (pkgs.isEmpty()) {
            Toast.makeText(this, "没有有效包名", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("结束全部目标进程")
                .setMessage("将对 " + pkgs.size() + " 个目标应用调用结束后台进程（无需 Root）。\n"
                        + "若应用正在前台，可能无法完全结束，可再点「打开应用详情」手动强停。")
                .setPositiveButton("全部结束后台", (d, w) -> doKillBackground(pkgs))
                .setNegativeButton("取消", null)
                .show();
    }

    private void doKillBackground(final List<String> packages) {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am == null) {
            Toast.makeText(this, "无法获取 ActivityManager", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        int ok = 0;
        for (String pkg : packages) {
            try {
                am.killBackgroundProcesses(pkg);
                ok++;
                sb.append("✓ ").append(pkg).append("\n");
            } catch (Exception e) {
                sb.append("✗ ").append(pkg).append("\n  ").append(e.getMessage()).append("\n");
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("已请求结束后台  " + ok + "/" + packages.size())
                .setMessage(sb.toString()
                        + "\n说明：系统 API 只能结束后台进程。"
                        + "若目标仍在运行，请用「打开应用详情」手动强制停止，然后重新打开目标应用。")
                .setPositiveButton("确定", null)
                .show();
    }

    private void openAppDetails(String packageName) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Toast.makeText(this, "请在系统页面点击「强制停止」", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "无法打开应用详情: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }



    private void showContactDialog() {
        final String groupNumber = "764468055";
        String info;
        try {
            info = getString(R.string.contact_info);
        } catch (Exception e) {
            info = "QQ 群号：" + groupNumber;
        }
        new AlertDialog.Builder(this)
                .setTitle("联系方式 · QQ 群")
                .setMessage(info)
                .setPositiveButton("加入群聊", (d, w) -> openQQGroup(groupNumber))
                .setNeutralButton("复制群号", (d, w) -> {
                    try {
                        android.content.ClipboardManager cm =
                                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        if (cm != null) {
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("qq_group", groupNumber));
                            Toast.makeText(this, "群号已复制：" + groupNumber, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "复制失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    /** 尝试打开手机 QQ 并跳转到指定群 */
    private void openQQGroup(String groupNumber) {
        String[] uris = {
                "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=" + groupNumber + "&card_type=group&source=qrcode",
                "tencent://groupwpa/?src_type=web&version=1.0&uin=" + groupNumber
        };
        for (String u : uris) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(u));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            } catch (Exception ignored) {}
        }
        try {
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("qq_group", groupNumber));
            }
        } catch (Exception ignored) {}
        Toast.makeText(this, "未检测到 QQ，群号 " + groupNumber + " 已复制，请手动搜索添加", Toast.LENGTH_LONG).show();
    }


    // ========== 包名搜索 ==========

    private static class AppEntry {
        String packageName;
        String label;
        boolean isSystem;

        AppEntry(String packageName, String label, boolean isSystem) {
            this.packageName = packageName;
            this.label = label;
            this.isSystem = isSystem;
        }
    }

    private void showPackagePicker() {
        final List<AppEntry> allApps = new ArrayList<>();
        PackageManager pm = getPackageManager();
        try {
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo info : apps) {
                String label;
                try {
                    label = info.loadLabel(pm).toString();
                } catch (Exception e) {
                    label = info.packageName;
                }
                boolean isSystem = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                allApps.add(new AppEntry(info.packageName, label, isSystem));
            }
            Collections.sort(allApps, new Comparator<AppEntry>() {
                @Override
                public int compare(AppEntry a, AppEntry b) {
                    return a.label.compareToIgnoreCase(b.label);
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "无法获取已安装应用: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout dialogRoot = new LinearLayout(this);
        dialogRoot.setOrientation(LinearLayout.VERTICAL);
        dialogRoot.setPadding(dp(16), dp(12), dp(16), dp(8));

        final EditText searchBox = makeEditText("搜索应用名或包名…");
        dialogRoot.addView(searchBox);

        TextView hint = new TextView(this);
        hint.setText("共 " + allApps.size() + " 个应用，点击选择");
        hint.setTextSize(12);
        hint.setTextColor(COLOR_TEXT_SEC);
        hint.setPadding(0, dp(8), 0, dp(4));
        dialogRoot.addView(hint);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(360)));

        final LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        dialogRoot.addView(scroll);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择已安装应用")
                .setView(dialogRoot)
                .setNegativeButton("取消", null)
                .create();

        final Runnable refreshPicker = new Runnable() {
            @Override
            public void run() {
                list.removeAllViews();
                String q = searchBox.getText().toString().trim().toLowerCase(Locale.ROOT);
                int shown = 0;
                for (final AppEntry entry : allApps) {
                    if (!q.isEmpty()) {
                        String hay = (entry.label + " " + entry.packageName).toLowerCase(Locale.ROOT);
                        if (!hay.contains(q)) continue;
                    }
                    LinearLayout row = new LinearLayout(MainActivity.this);
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setPadding(dp(12), dp(10), dp(12), dp(10));
                    row.setBackgroundColor(shown % 2 == 0 ? Color.WHITE : 0xFFF8F8F8);

                    TextView nameTv = new TextView(MainActivity.this);
                    nameTv.setText(entry.label + (entry.isSystem ? "  [系统]" : ""));
                    nameTv.setTextSize(14);
                    nameTv.setTextColor(COLOR_TEXT);
                    nameTv.setTypeface(null, Typeface.BOLD);
                    row.addView(nameTv);

                    TextView pkgTv = new TextView(MainActivity.this);
                    pkgTv.setText(entry.packageName);
                    pkgTv.setTextSize(11);
                    pkgTv.setTextColor(COLOR_TEXT_SEC);
                    row.addView(pkgTv);

                    row.setOnClickListener(v -> {
                        pkgEdit.setText(entry.packageName);
                        if (idEdit.getText().toString().trim().isEmpty()) {
                            String randomId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                            idEdit.setText(randomId);
                        }
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "已选择: " + entry.label, Toast.LENGTH_SHORT).show();
                    });

                    list.addView(row);
                    shown++;
                    if (shown >= 300) {
                        TextView more = new TextView(MainActivity.this);
                        more.setText("… 结果过多，请输入关键词缩小范围");
                        more.setTextColor(COLOR_TEXT_SEC);
                        more.setPadding(dp(12), dp(12), dp(12), dp(12));
                        list.addView(more);
                        break;
                    }
                }
                if (shown == 0) {
                    TextView none = new TextView(MainActivity.this);
                    none.setText("无匹配应用");
                    none.setTextColor(COLOR_TEXT_SEC);
                    none.setGravity(Gravity.CENTER);
                    none.setPadding(dp(16), dp(32), dp(16), dp(32));
                    list.addView(none);
                }
            }
        };

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                refreshPicker.run();
            }
        });

        refreshPicker.run();
        dialog.show();
    }
}
