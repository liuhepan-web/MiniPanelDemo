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

/**
 * Login entry. After first successful login, next launches reuse SDK session or
 * auto-call login with locally saved credentials (no need to tap Sign in).
 */
public class DemoLoginActivity extends AppCompatActivity {

    private TextInputEditText etCountry;
    private TextInputEditText etAccount;
    private TextInputEditText etPassword;
    private MaterialCheckBox cbRemember;
    private MaterialButton btnLogin;
    private boolean autoLoginInFlight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) SDK session still valid → enter Hub directly
        if (isSdkSessionValid()) {
            enterHubAfterLogin();
            return;
        }

        setContentView(R.layout.login);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(null);

        etCountry = findViewById(R.id.et_country);
        etAccount = findViewById(R.id.et_account);
        etPassword = findViewById(R.id.et_password);
        etCountry.setText("86");

        cbRemember = findViewById(R.id.cb_remember);
        if (DemoCurrentHomeStore.INSTANCE.restoreLoginFields(this, etCountry, etAccount, etPassword)) {
            cbRemember.setChecked(true);
        } else {
            // Demo default: remember for next cold-start auto-login
            cbRemember.setChecked(true);
        }

        btnLogin = findViewById(R.id.btn_login);
        TextView tvGoRegister = findViewById(R.id.tv_go_register);
        btnLogin.setOnClickListener(v ->
                performLogin(text(etCountry), text(etAccount), text(etPassword),
                        cbRemember.isChecked(), false));

        tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, UserRegisterActivity.class)));

        // 2) Session expired but credentials saved → auto login API
        if (DemoCurrentHomeStore.INSTANCE.hasSavedLoginCredentials(this)) {
            String country = DemoCurrentHomeStore.INSTANCE.getSavedCountry(this);
            String account = DemoCurrentHomeStore.INSTANCE.getSavedAccount(this);
            String password = DemoCurrentHomeStore.INSTANCE.getSavedPassword(this);
            performLogin(country, account, password, true, true);
        }
    }

    private static boolean isSdkSessionValid() {
        try {
            if (ThingHomeSdk.getUserInstance().isLogin()) {
                return true;
            }
        } catch (Throwable ignored) {
            // older / stub APIs may not expose isLogin
        }
        return ThingHomeSdk.getUserInstance().getUser() != null;
    }

    private void performLogin(
            String country,
            String account,
            String password,
            boolean remember,
            boolean fromAuto) {
        if (account.isEmpty() || password.isEmpty()) {
            if (!fromAuto) {
                Toast.makeText(this, R.string.demo_login_empty, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (autoLoginInFlight) {
            return;
        }
        autoLoginInFlight = true;
        setLoginUiEnabled(false);
        if (fromAuto) {
            Toast.makeText(this, R.string.demo_auto_login_ing, Toast.LENGTH_SHORT).show();
        }

        ILoginCallback cb = new ILoginCallback() {
            @Override
            public void onSuccess(User user) {
                autoLoginInFlight = false;
                if (remember) {
                    DemoCurrentHomeStore.INSTANCE.saveLoginCredentials(
                            DemoLoginActivity.this, country, account, password);
                } else {
                    DemoCurrentHomeStore.INSTANCE.clearLoginCredentials(DemoLoginActivity.this);
                }
                enterHubAfterLogin();
            }

            @Override
            public void onError(String code, String error) {
                autoLoginInFlight = false;
                setLoginUiEnabled(true);
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
    }

    private void enterHubAfterLogin() {
        BizBundleInitializer.onLogin();
        startActivity(new Intent(this, DemoHubActivity.class));
        finish();
    }

    private void setLoginUiEnabled(boolean enabled) {
        if (btnLogin != null) {
            btnLogin.setEnabled(enabled);
        }
        if (etCountry != null) {
            etCountry.setEnabled(enabled);
        }
        if (etAccount != null) {
            etAccount.setEnabled(enabled);
        }
        if (etPassword != null) {
            etPassword.setEnabled(enabled);
        }
        if (cbRemember != null) {
            cbRemember.setEnabled(enabled);
        }
    }

    private static String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }
}
