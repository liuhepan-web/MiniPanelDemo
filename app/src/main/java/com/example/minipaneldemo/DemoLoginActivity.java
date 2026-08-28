package com.example.minipaneldemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.thingclips.smart.android.common.utils.ValidatorUtil;
import com.thingclips.smart.android.user.api.ILoginCallback;
import com.thingclips.smart.android.user.bean.User;
import com.thingclips.smart.bizbundle.initializer.BizBundleInitializer;
import com.thingclips.smart.home.sdk.ThingHomeSdk;

public class DemoLoginActivity extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(null);

        TextInputEditText etCountry = findViewById(R.id.et_country);
        TextInputEditText etAccount = findViewById(R.id.et_account);
        TextInputEditText etPassword = findViewById(R.id.et_password);
        etCountry.setText("86");


        MaterialCheckBox cbRemember = findViewById(R.id.cb_remember);
        if (DemoCurrentHomeStore.INSTANCE.restoreLoginFields(this, etCountry, etAccount, etPassword)) {
            cbRemember.setChecked(true);
        }
        MaterialButton btn = findViewById(R.id.btn_login);
        TextView tvGoRegister = findViewById(R.id.tv_go_register);
        btn.setOnClickListener(v -> {
            String country = text(etCountry);
            String account = text(etAccount);
            String password = text(etPassword);
            boolean remember = cbRemember.isChecked();
            ILoginCallback cb = new ILoginCallback() {
                @Override
                public void onSuccess(User user) {
                    BizBundleInitializer.onLogin();
                    if (remember) {
                        DemoCurrentHomeStore.INSTANCE.saveLoginCredentials(
                                DemoLoginActivity.this, country, account, password);
                    } else {
                        DemoCurrentHomeStore.INSTANCE.clearLoginCredentials(DemoLoginActivity.this);
                    }
                    startActivity(new Intent(DemoLoginActivity.this, DemoHubActivity.class));
                    finish();
                }

                @Override
                public void onError(String code, String error) {
                    Toast.makeText(DemoLoginActivity.this,
                            getString(R.string.demo_login_error, code, error != null ? error : ""),
                            Toast.LENGTH_LONG).show();
                }
            };
            if (ValidatorUtil.isEmail(account)) {
                ThingHomeSdk.getUserInstance().loginWithEmail(country, account, password, cb);
            } else {
                ThingHomeSdk.getUserInstance().loginWithPhonePassword(country, account, password, cb);
            }
        });

        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, UserRegisterActivity.class)));
    }

    private static String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }
    }

