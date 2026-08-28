package com.example.minipaneldemo;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk;
import com.thingclips.smart.android.camera.sdk.api.IThingIPCVAS;
import com.thingclips.smart.android.camera.sdk.bean.CameraVASParams;
import com.thingclips.smart.android.camera.sdk.constant.ThingIPCConstant;
import com.thingclips.smart.api.router.UrlBuilder;
import com.thingclips.smart.api.router.UrlRouter;
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.OperationCallBack;
import com.thingclips.smart.home.sdk.ThingHomeSdk;

/**
 * Open IPC cloud storage (CATEGORY_CODE_SECURITY_CLOUD_SERVICE) miniapp page.
 * @see <a href="https://developer.tuya.com/cn/docs/app-development/ipc-value-added-service-2?id=Ke2iaqr2xoyz5">VAS 2.0</a>
 */
public class DemoIpcCloudVasActivity extends AppCompatActivity {

    private static final String TAG = "DemoIpcCloudVas";
    private static final String DEFAULT_DEV_ID = "vdevo178598442990463";

    private TextView tvStatus;
    private MaterialButton btnOpen;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ipc_cloud_vas);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvDevId = findViewById(R.id.tv_dev_id);
        tvStatus = findViewById(R.id.tv_status);
        btnOpen = findViewById(R.id.btn_open_cloud_vas);

        tvDevId.setText(getString(R.string.demo_ipc_cloud_vas_dev_fmt, DEFAULT_DEV_ID));
        btnOpen.setOnClickListener(v -> openCloudStorageMiniApp());
    }

    /**
     * Fetch cloud-storage VAS URL and route into miniApp container.
     */
    private void openCloudStorageMiniApp() {
        if (ThingHomeSdk.getUserInstance().getUser() == null) {
            Toast.makeText(this, R.string.demo_user_null, Toast.LENGTH_SHORT).show();
            return;
        }
        long homeId = DemoCurrentHomeStore.getCurrentHomeId(this);
        if (homeId == 0L) {
            Toast.makeText(this, R.string.demo_need_home, Toast.LENGTH_LONG).show();
            return;
        }

        IThingIPCVAS ipcVas = ThingIPCSdk.getIPCVAS();
        if (ipcVas == null) {
            setStatus(getString(R.string.demo_ipc_cloud_vas_sdk_missing));
            Toast.makeText(this, R.string.demo_ipc_cloud_vas_sdk_missing, Toast.LENGTH_LONG).show();
            return;
        }

        btnOpen.setEnabled(false);
        setStatus(getString(R.string.demo_ipc_cloud_vas_loading));

        CameraVASParams params = new CameraVASParams();
        params.devId = DEFAULT_DEV_ID;
        params.spaceId = String.valueOf(homeId);
        params.languageCode = "zh";
        params.categoryCode = ThingIPCConstant.CATEGORY_CODE_SECURITY_CLOUD_SERVICE;
        params.hybridType = ThingIPCConstant.HYBRID_TYPE_MINI_APP;

        ipcVas.fetchValueAddedServiceUrl(params, new OperationCallBack() {
            @Override
            public void onSuccess(int sessionId, int requestId, String data, Object camera) {
                runOnUiThread(() -> {
                    btnOpen.setEnabled(true);
                    if (data == null || data.isEmpty()) {
                        setStatus(getString(R.string.demo_ipc_cloud_vas_empty_url));
                        Toast.makeText(DemoIpcCloudVasActivity.this,
                                R.string.demo_ipc_cloud_vas_empty_url, Toast.LENGTH_LONG).show();
                        return;
                    }
                    setStatus(getString(R.string.demo_ipc_cloud_vas_opening));
                    Bundle bundle = new Bundle();
                    bundle.putString("url", data);
                    UrlBuilder builder = new UrlBuilder(DemoIpcCloudVasActivity.this, "miniApp")
                            .putExtras(bundle);
                    UrlRouter.execute(builder);
                });
            }

            @Override
            public void onFailure(int sessionId, int requestId, int errCode, Object camera) {
                runOnUiThread(() -> {
                    btnOpen.setEnabled(true);
                    String msg = getString(R.string.demo_ipc_cloud_vas_fail, errCode);
                    setStatus(msg);
                    Log.e(TAG, msg);
                    Toast.makeText(DemoIpcCloudVasActivity.this, msg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setStatus(String text) {
        if (tvStatus != null) {
            tvStatus.setText(text);
        }
    }
}
