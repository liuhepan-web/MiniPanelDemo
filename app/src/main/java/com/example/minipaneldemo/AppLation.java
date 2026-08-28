package com.example.minipaneldemo;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.facebook.soloader.SoLoader;
import com.thing.smart.miniappclient.ThingMiniAppClient;
import com.thingclips.smart.android.user.api.ILogoutCallback;
import com.thingclips.smart.api.MicroContext;
import com.thingclips.smart.api.router.UrlBuilder;
import com.thingclips.smart.api.service.RedirectService;
import com.thingclips.smart.api.service.RouteEventListener;
import com.thingclips.smart.api.service.ServiceEventListener;
import com.thingclips.smart.bizbundle.initializer.BizBundleInitializer;
import com.thingclips.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService;
import com.thingclips.smart.home.sdk.ThingHomeSdk;
import com.thingclips.smart.sdk.api.INeedLoginListener;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class AppLation extends Application {

    @Override
    public void onCreate() {
        super.onCreate();



        ThingHomeSdk.init(this);
        ThingHomeSdk.setDebugMode(true);

        SoLoader.init(this, false);


        BizBundleInitializer.init(this, new RouteEventListener() {
            @Override
            public void onFaild(int errorCode, UrlBuilder urlBuilder) {
                Log.e("BizBundleRoute", "not implemented: " + urlBuilder);
            }
        }, new ServiceEventListener() {
            @Override
            public void onFaild(String serviceName) {
                Log.e("BizBundleService", "not implemented: " + serviceName);
            }
        });
        BizBundleInitializer.registerService(AbsBizBundleFamilyService.class, new BizBundleFamilyServiceImpl());

        RedirectService redirectService = MicroContext.getServiceManager()
                .findServiceByInterface(RedirectService.class.getName());
        if (redirectService != null) {
            redirectService.registerUrlInterceptor(new RedirectService.UrlInterceptor() {
                @Override
                public void forUrlBuilder(UrlBuilder urlBuilder, RedirectService.InterceptorCallback interceptorCallback) {
                    interceptorCallback.onContinue(urlBuilder);
                }
            });
        }

        ThingMiniAppClient.initialClient().initialize();
        restoreVConsoleDebugSetting();

        ThingHomeSdk.setOnNeedLoginListener(new INeedLoginListener() {
            @Override
            public void onNeedLogin(Context context) {

                ThingHomeSdk.getUserInstance().logout(new ILogoutCallback() {
                    @Override
                    public void onSuccess() {
                        //退出登录成功
                        Toast.makeText(context, "退出成功", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String errorCode, String errorMsg) {
                    }
                });

                Intent intent = new Intent(context, DemoLoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
        

    }

    private void restoreVConsoleDebugSetting() {
        boolean enabled = getSharedPreferences("demo_miniapp_prefs", MODE_PRIVATE)
                .getBoolean("vconsole_enabled", false);
        ThingMiniAppClient.debugClient().vConsoleDebugEnable(enabled);
    }
}
