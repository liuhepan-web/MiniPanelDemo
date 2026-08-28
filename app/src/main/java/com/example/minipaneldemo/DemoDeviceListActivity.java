package com.example.minipaneldemo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.thingclips.smart.api.MicroContext;
import com.thingclips.smart.home.sdk.ThingHomeSdk;
import com.thingclips.smart.home.sdk.bean.HomeBean;
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback;
import com.thingclips.smart.panel.ota.service.AbsOtaCallerService;
import com.thingclips.smart.panelcaller.api.AbsPanelCallerService;
import com.thingclips.smart.sdk.api.IResultCallback;
import com.thingclips.smart.sdk.api.IThingDevice;
import com.thingclips.smart.sdk.bean.DeviceBean;

import java.util.ArrayList;
import java.util.List;

public class DemoDeviceListActivity extends AppCompatActivity {


    private DevAdapter adapter;

   

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.devicelist);
        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_homes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DevAdapter();
        rv.setAdapter(adapter);

        long hid = DemoCurrentHomeStore.getCurrentHomeId(this);
        if (hid == 0) {
            Toast.makeText(this, R.string.demo_need_home, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        ThingHomeSdk.newHomeInstance(hid).getHomeDetail(new IThingHomeResultCallback() {
            @Override
            public void onSuccess(HomeBean homeBean) {
                List<DeviceBean> list = homeBean.getDeviceList();
                adapter.setData(list != null ? list : new ArrayList<>());
            }

            @Override
            public void onError(String errorCode, String errorMsg) {
                Toast.makeText(DemoDeviceListActivity.this,
                        getString(R.string.demo_load_devices_error, errorMsg != null ? errorMsg : errorCode),
                        Toast.LENGTH_LONG).show();
            }
        });

    }







    private void openPanel(String devId) {

        Object svc = MicroContext.getServiceManager()
                .findServiceByInterface(AbsPanelCallerService.class.getName());
        if (svc instanceof AbsPanelCallerService) {
            ((AbsPanelCallerService) svc).goPanelWithCheckAndTip(this, devId);
        } else {
            Toast.makeText(this, R.string.demo_panel_missing, Toast.LENGTH_LONG).show();


        }



    }

    private void openFirmwareUpgrade(String devId) {
        if (devId == null || devId.isEmpty()) {
            return;
        }
        Object raw = MicroContext.getServiceManager()
                .findServiceByInterface(AbsOtaCallerService.class.getName());
        if (!(raw instanceof AbsOtaCallerService)) {
            Toast.makeText(this, R.string.demo_ota_service_missing, Toast.LENGTH_LONG).show();
            return;
        }
        AbsOtaCallerService ota = (AbsOtaCallerService) raw;
        if (ota.isSupportUpgrade(devId)) {
            ota.goFirmwareUpgrade(this, devId);
        } else {
            Toast.makeText(this, R.string.demo_ota_not_supported, Toast.LENGTH_LONG).show();
        }
    }

    private void confirmRemoveDevice(String devId) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.demo_remove_device_title)
                .setMessage(R.string.demo_remove_device_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> removeDevice(devId))
                .show();
    }

    private void removeDevice(String devId) {
        if (devId == null || devId.isEmpty()) {
            return;
        }
        IThingDevice mDevice = ThingHomeSdk.newDeviceInstance(devId);
        mDevice.removeDevice(new IResultCallback() {
            @Override
            public void onError(String errorCode, String errorMsg) {
                Toast.makeText(DemoDeviceListActivity.this,
                        getString(R.string.demo_remove_device_error,
                                errorMsg != null ? errorMsg : errorCode),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onSuccess() {
                Toast.makeText(DemoDeviceListActivity.this,
                        R.string.demo_remove_device_success,
                        Toast.LENGTH_SHORT).show();
                long hid = DemoCurrentHomeStore.getCurrentHomeId(DemoDeviceListActivity.this);
                if (hid != 0) {
                    loadDevices(hid);
                }
            }
        });
    }

    private void loadDevices(long hid) {
    }

    private class DevAdapter extends RecyclerView.Adapter<DevAdapter.VH> {

        private final List<DeviceBean> items = new ArrayList<>();

        void setData(List<DeviceBean> d) {
            items.clear();
            items.addAll(d);
            notifyDataSetChanged();
        }


        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.devicerow, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DeviceBean b = items.get(position);
            holder.t1.setText(b.getName() != null ? b.getName() : b.devId);
            holder.t2.setText(b.devId);
            holder.btnOta.setOnClickListener(v -> openFirmwareUpgrade(b.devId));
            holder.itemView.setOnClickListener(v -> openPanel(b.devId));
            holder.itemView.setOnLongClickListener(v -> {
                confirmRemoveDevice(b.devId);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView t1;
            final TextView t2;
            final MaterialButton btnOta;

            VH(@NonNull View itemView) {
                super(itemView);
                t1 = itemView.findViewById(R.id.tv_name);
                t2 = itemView.findViewById(R.id.tv_dev_id);
                btnOta = itemView.findViewById(R.id.btn_ota);
            }
        }
    }

}

