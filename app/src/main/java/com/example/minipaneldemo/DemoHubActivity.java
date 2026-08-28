package com.example.minipaneldemo;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.minipaneldemo.language.LocaleHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.thing.smart.miniappclient.ThingMiniAppClient;
import com.thingclips.smart.activator.plug.mesosphere.ThingDeviceActivatorManager;
import com.thingclips.smart.activator.plug.mesosphere.api.IThingDeviceActiveListener;
import com.thingclips.smart.android.user.api.ILogoutCallback;
import com.thingclips.smart.android.user.bean.User;
import com.thingclips.smart.api.MicroContext;
import com.thingclips.smart.api.router.UrlBuilder;
import com.thingclips.smart.api.router.UrlRouter;
import com.thingclips.smart.bizbundle.initializer.BizBundleInitializer;
import com.thingclips.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService;
import com.thingclips.smart.home.sdk.ThingHomeSdk;
import com.thingclips.smart.home.sdk.bean.HomeBean;
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback;
import com.thingclips.smart.panelcaller.api.AbsPanelCallerService;

import java.util.List;

public class DemoHubActivity extends AppCompatActivity {

    private static final String TAG = "DemoHub";
    private static final String PREFS_MINIAPP = "demo_miniapp_prefs";
    private static final String KEY_VCONSOLE = "vconsole_enabled";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.hub);
        MaterialButton bProfile = findViewById(R.id.btn_profile);
        MaterialButton bHome = findViewById(R.id.btn_pick_home);
        MaterialButton bFamilySetting = findViewById(R.id.btn_family_setting);
        MaterialButton bAct = findViewById(R.id.btn_activator);
        MaterialButton bDev = findViewById(R.id.btn_devices);
        MaterialButton bMini = findViewById(R.id.btn_miniapp);
        MaterialButton bMiniClear = findViewById(R.id.btn_miniapp_clear);
        MaterialButton bIpcCloudVas = findViewById(R.id.btn_ipc_cloud_vas);
        MaterialButton bLanguage = findViewById(R.id.btn_language);
        SwitchMaterial switchVConsole = findViewById(R.id.switch_vconsole);
        MaterialButton bOut = findViewById(R.id.btn_logout);

        bProfile.setOnClickListener(v -> showProfile());
        bHome.setOnClickListener(v ->
                startActivityForResult(new Intent(this, DemoHomePickerActivity.class), 1));
        bFamilySetting.setOnClickListener(v -> openFamilySetting());
        bAct.setOnClickListener(v -> openActivator());
        bDev.setOnClickListener(v -> startActivity(new Intent(this, DemoDeviceListActivity.class)));
        bMini.setOnClickListener(v -> promptMiniApp());
        bMiniClear.setOnClickListener(v -> confirmClearMiniAppCache());
        bIpcCloudVas.setOnClickListener(v ->
                startActivity(new Intent(this, DemoIpcCloudVasActivity.class)));
        bLanguage.setOnClickListener(v -> showLanguageDialog());
        setupVConsoleSwitch(switchVConsole);
        bOut.setOnClickListener(v -> logout());
    }

    private void showLanguageDialog() {
        final String[][] options = {
                {LocaleHelper.LANG_ENGLISH, getString(R.string.language_en)},
                {LocaleHelper.LANG_CHINESE, getString(R.string.language_zh)},
                {LocaleHelper.LANG_SYSTEM, getString(R.string.language_system)}
        };
        String[] labels = new String[options.length];
        String current = LocaleHelper.getLanguage(this);
        int checkedItem = -1;
        for (int i = 0; i < options.length; i++) {
            labels[i] = options[i][1];
            if (options[i][0].equals(current)) {
                checkedItem = i;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.language_dialog_title))
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    dialog.dismiss();
                    String selected = options[which][0];
                    if (selected.equals(LocaleHelper.getLanguage(this))) {
                        return;
                    }
                    LocaleHelper.switchLanguageAndRestart(this, selected);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            syncFamilyFromStore();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncFamilyFromStore();
    }

    private void syncFamilyFromStore() {
        long hid = DemoCurrentHomeStore.getCurrentHomeId(this);
        if (hid == 0) {
            return;
        }
        ThingHomeSdk.newHomeInstance(hid).getHomeDetail(new IThingHomeResultCallback() {
            @Override
            public void onSuccess(HomeBean homeBean) {
                Object raw = MicroContext.getServiceManager()
                        .findServiceByInterface(AbsBizBundleFamilyService.class.getName());
                if (raw instanceof AbsBizBundleFamilyService) {
                    ((AbsBizBundleFamilyService) raw).shiftCurrentFamily(
                            homeBean.getHomeId(),
                            homeBean.getName() != null ? homeBean.getName() : "");
                }
            }

            @Override
            public void onError(String errorCode, String errorMsg) {
                Log.w(TAG, "getHomeDetail " + errorCode + " " + errorMsg);
            }
        });
    }

    private void showProfile() {
        User u = ThingHomeSdk.getUserInstance().getUser();
        if (u == null) {
            Toast.makeText(this, R.string.demo_user_null, Toast.LENGTH_SHORT).show();
            return;
        }
        String msg = getString(R.string.demo_profile_fmt,
                nz(u.getUid()),
                nz(u.getEmail()),
                nz(u.getMobile()));
        new AlertDialog.Builder(this)
                .setTitle(R.string.demo_profile_title)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private static String nz(String s) {
        return s != null ? s : "";
    }

    private void openFamilySetting() {
        long homeId = DemoCurrentHomeStore.getCurrentHomeId(this);
        if (homeId == 0) {
            Toast.makeText(this, R.string.demo_need_home, Toast.LENGTH_LONG).show();
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("family_setting", String.valueOf(homeId));
        UrlBuilder urlBuilder = new UrlBuilder(this, "family_setting").putExtras(bundle);
        UrlRouter.execute(urlBuilder);
    }

    //配网
    private void openActivator() {
        if (DemoCurrentHomeStore.getCurrentHomeId(this) == 0) {
            Toast.makeText(this, R.string.demo_need_home, Toast.LENGTH_LONG).show();
            return;
        }

        ThingDeviceActivatorManager.INSTANCE.startDeviceActiveAction(this);
        ThingDeviceActivatorManager.INSTANCE.addListener(new IThingDeviceActiveListener() {
            @Override
            public void onDevicesAdd(List<String> list) {
                Log.i(TAG, "onDevicesAdd " + list);

                Log.i(TAG, "onDevicesAdd " + list);
                if (list == null || list.isEmpty()) {
                    return;
                }
                String devId = list.get(0);
                openDevicePanelWithCheck(devId);

            }

            @Override
            public void onRoomDataUpdate() {
                Log.i(TAG, "onRoomDataUpdate");

            }

            @Override
            public void onOpenDevicePanel(String s) {


            }
        });

    }

    //扫描跳转设备页面
    private void openDevicePanelWithCheck(String devId) {

        if (devId == null || devId.isEmpty()) {
            return;
        }
        Object svc = MicroContext.getServiceManager()
                .findServiceByInterface(AbsPanelCallerService.class.getName());
        if (svc instanceof AbsPanelCallerService) {
            ((AbsPanelCallerService) svc).goPanelWithCheckAndTip(DemoHubActivity.this, devId);
        }
    }

    private void confirmClearMiniAppCache() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.demo_miniapp_clear_title)
                .setMessage(R.string.demo_miniapp_clear_message)
                .setPositiveButton(android.R.string.ok, (d, w) -> clearMiniAppCache())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void clearMiniAppCache() {
        ThingMiniAppClient.coreClient().clearCache();
        Toast.makeText(this, R.string.demo_miniapp_clear_success, Toast.LENGTH_SHORT).show();
    }

    private void setupVConsoleSwitch(@NonNull SwitchMaterial switchVConsole) {
        SharedPreferences prefs = getSharedPreferences(PREFS_MINIAPP, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_VCONSOLE, false);
        switchVConsole.setOnCheckedChangeListener(null);
        switchVConsole.setChecked(enabled);
        applyVConsoleDebug(enabled, false);
        switchVConsole.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_VCONSOLE, isChecked).apply();
            applyVConsoleDebug(isChecked, true);
        });
    }

    private void applyVConsoleDebug(boolean enabled, boolean showToast) {
        ThingMiniAppClient.debugClient().vConsoleDebugEnable(enabled);
        if (showToast) {
            Toast.makeText(this,
                    enabled ? R.string.demo_vconsole_on : R.string.demo_vconsole_off,
                    Toast.LENGTH_SHORT).show();
        }
    }

    //输入小程序ID打开
    private void promptMiniApp() {
        TextInputEditText input = new TextInputEditText(this);
        input.setHint(R.string.demo_miniapp_hint);
        input.setText(R.string.demo_miniapp_sample_id);
        new AlertDialog.Builder(this)
                .setTitle(R.string.demo_miniapp_title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String appId = input.getText() != null ? input.getText().toString().trim() : "";
                    if (appId.isEmpty()) {
                        Toast.makeText(this, R.string.demo_miniapp_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
//                    ThingMiniAppClient.coreClient().openMiniAppByUrl(this,"godzilla://tyfarinynzhfisqswp?aiPtChannel=aipt_eflkb68j85c0", null);

                    ThingMiniAppClient
                            .coreClient()
                            .openMiniAppByAppId(this, "tyfarinynzhfisqswp", null, null);


                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void logout() {
        ThingHomeSdk.getUserInstance().logout(new ILogoutCallback() {
            @Override
            public void onSuccess() {
                BizBundleInitializer.onLogout(DemoHubActivity.this);
                DemoCurrentHomeStore.INSTANCE.clearAll(DemoHubActivity.this);
                Intent i = new Intent(DemoHubActivity.this, DemoLoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }

            @Override
            public void onError(String errorCode, String errorMsg) {
                Toast.makeText(DemoHubActivity.this,
                        errorMsg != null ? errorMsg : errorCode,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
