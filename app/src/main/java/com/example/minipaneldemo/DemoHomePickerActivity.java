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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.thingclips.smart.api.MicroContext;
import com.thingclips.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService;
import com.thingclips.smart.home.sdk.ThingHomeSdk;
import com.thingclips.smart.home.sdk.bean.HomeBean;
import com.thingclips.smart.home.sdk.callback.IThingGetHomeListCallback;
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback;

import java.util.ArrayList;
import java.util.List;

public class DemoHomePickerActivity extends AppCompatActivity {



    private final List<HomeBean> data = new ArrayList<>();
    private HomesAdapter adapter;
    private TextView tvEmptyHint;
    private RecyclerView rvHomes;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.homechouse);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        tvEmptyHint = findViewById(R.id.tv_empty_hint);
        rvHomes = findViewById(R.id.rv_homes);
        rvHomes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HomesAdapter();
        rvHomes.setAdapter(adapter);

        MaterialButton btnCreate = findViewById(R.id.btn_create_home);
        btnCreate.setOnClickListener(v -> showCreateHomeDialog());

        reloadHomeList();


    }

    private void reloadHomeList() {

        ThingHomeSdk.getHomeManagerInstance().queryHomeList(new IThingGetHomeListCallback() {
            @Override
            public void onSuccess(List<HomeBean> homeBeans) {
                data.clear();
                if (homeBeans != null) {
                    data.addAll(homeBeans);
                }
                adapter.notifyDataSetChanged();
                updateEmptyUi();
            }

            @Override
            public void onError(String errorCode, String error) {
                Toast.makeText(
                        DemoHomePickerActivity.this,
                        getString(R.string.demo_home_list_error, error != null ? error : errorCode),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateEmptyUi() {
        boolean empty = data.isEmpty();
        tvEmptyHint.setVisibility(empty ? View.VISIBLE : View.GONE);


    }

    private void showCreateHomeDialog() {

        View form = LayoutInflater.from(this).inflate(R.layout.diago_home, null, false);
        TextInputEditText etName = form.findViewById(R.id.et_home_name);
        TextInputEditText etCity = form.findViewById(R.id.et_city);
        etCity.setText(R.string.demo_home_default_city);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.demo_home_create_home)
                .setView(form)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.demo_home_create_confirm, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String homeName = etName.getText() != null ? etName.getText().toString().trim() : "";
            String city = etCity.getText() != null ? etCity.getText().toString().trim() : "";
            if (homeName.isEmpty()) {
                Toast.makeText(this, R.string.demo_home_name_required, Toast.LENGTH_SHORT).show();
                return;
            }
            createHomeOnCloud(homeName, city);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void createHomeOnCloud(String homeName, String city) {

        ThingHomeSdk.getHomeManagerInstance().createHome(
                homeName,
                120.52,
                30.40,
                city,
                new ArrayList<>(),
                new IThingHomeResultCallback() {
                    @Override
                    public void onSuccess(HomeBean bean) {
                        Toast.makeText(
                                DemoHomePickerActivity.this,
                                R.string.demo_home_create_success,
                                Toast.LENGTH_SHORT).show();
                        reloadHomeList();
                    }

                    @Override
                    public void onError(String errorCode, String errorMsg) {
                        Toast.makeText(
                                DemoHomePickerActivity.this,
                                getString(R.string.demo_home_create_fail,
                                        errorMsg != null ? errorMsg : errorCode),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void onPick(HomeBean bean) {
        DemoCurrentHomeStore.INSTANCE.setCurrentHomeId(this, bean.getHomeId());
        tryShiftBizBundleFamily(bean.getHomeId(), bean.getName() != null ? bean.getName() : "");
        Toast.makeText(this, R.string.demo_home_selected, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void tryShiftBizBundleFamily(long homeId, String s) {

        try {
            Class<?> microClz = Class.forName("com.thingclips.smart.api.MicroContext");
            Object serviceManager = microClz.getMethod("getServiceManager").invoke(null);
            if (serviceManager == null) {
                return;
            }
            String iface = "com.thingclips.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService";
            Object raw = serviceManager.getClass()
                    .getMethod("findServiceByInterface", String.class)
                    .invoke(serviceManager, iface);
            if (raw == null) {
                return;
            }
            raw.getClass()
                    .getMethod("shiftCurrentFamily", long.class, String.class)
                    .invoke(raw, homeId, s != null ? s : "");
        } catch (Throwable ignored) {
            // BizBundle not on classpath or API mismatch — SDK home id is still stored locally.
        }
    }

    private class HomesAdapter extends RecyclerView.Adapter<HomesAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.itme_home, parent, false);
            return new VH(v);
        }



        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            HomeBean b = data.get(position);
            holder.title.setText(b.getName() != null ? b.getName() : "(no name)");
            holder.sub.setText("homeId=" + b.getHomeId());
            holder.itemView.setOnClickListener(v -> onPick(b));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView sub;

            VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tv_home_name);
                sub = itemView.findViewById(R.id.tv_home_id);
            }
        }
    }
}
