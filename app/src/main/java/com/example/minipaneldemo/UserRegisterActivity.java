package com.example.minipaneldemo;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.thingclips.smart.android.common.utils.ValidatorUtil;
import com.thingclips.smart.android.user.api.IRegisterCallback;
import com.thingclips.smart.android.user.bean.User;
import com.thingclips.smart.bizbundle.initializer.BizBundleInitializer;
import com.thingclips.smart.home.sdk.ThingHomeSdk;
import com.thingclips.smart.sdk.api.IResultCallback;

public class UserRegisterActivity extends AppCompatActivity {



    private static final int VERIFY_TYPE_REGISTER = 1;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.regist);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        TextInputEditText etCountry = findViewById(R.id.et_country);
        TextInputEditText etAccount = findViewById(R.id.et_account);
        TextInputEditText etPassword = findViewById(R.id.et_password);
        TextInputEditText etCode = findViewById(R.id.et_code);
        etCountry.setText("86");
        MaterialButton btnSendCode = findViewById(R.id.btn_send_code);
        MaterialButton btnRegister = findViewById(R.id.btn_register);

        btnSendCode.setOnClickListener(v -> {
            String country = text(etCountry);
            String account = text(etAccount);
            if (!ValidatorUtil.isEmail(account)) {
                Toast.makeText(this, R.string.user_email_address, Toast.LENGTH_SHORT).show();
                return;
            }
            ThingHomeSdk.getUserInstance().sendVerifyCodeWithUserName(
                    account,
                    "",
                    country,
                    VERIFY_TYPE_REGISTER,
                    new IResultCallback() {
                        public void onError(String code, String error) {
                            Toast.makeText(UserRegisterActivity.this,
                                    getString(R.string.demo_send_code_error, code,
                                            error != null ? error : ""),
                                    Toast.LENGTH_LONG).show();
                        }
                        public void onSuccess() {
                            Toast.makeText(UserRegisterActivity.this,
                                    R.string.demo_send_verify_code,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        btnRegister.setOnClickListener(v -> {
            String country = text(etCountry);
            String account = text(etAccount);
            String password = text(etPassword);
            String code = text(etCode);
            if (!ValidatorUtil.isEmail(account)) {
                Toast.makeText(this, R.string.user_email_address, Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.isEmpty() || code.isEmpty()) {
                Toast.makeText(this, R.string.demo_password, Toast.LENGTH_SHORT).show();
                return;
            }
            ThingHomeSdk.getUserInstance().registerAccountWithEmail(
                    country,
                    account,
                    password,
                    code,
                    new IRegisterCallback() {

                        @Override
                        public void onSuccess(User user) {
                            BizBundleInitializer.onLogin();
                            startActivity(new Intent(UserRegisterActivity.this, DemoHubActivity.class));
                            finish();
                        }

                        @Override
                        public void onError(String code, String error) {
                            Toast.makeText(UserRegisterActivity.this,
                                    getString(R.string.demo_register_error, code,
                                            error != null ? error : ""),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

    }

    private static String text(TextInputEditText e) {
        return e.getText() != null ? e.getText().toString().trim() : "";
    }
}
