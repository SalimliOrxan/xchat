package com.example.orxan.xchat;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String pkg = null, cls = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        SharedPreferences pref = getSharedPreferences("Xchat", MODE_PRIVATE);

        if(UtilPermission.controlContacts(this)){
            if(pref.getBoolean("isLogin", false)){
                chooseScreen(false);
            } else chooseScreen(true);
        }
    }



    private void chooseScreen(boolean nextIsLogin){
        UpdateHelper.getInstance().init(this);

        if(nextIsLogin){
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.main, new Login())
                    .commit();
        } else {
            startActivity(new Intent(this, FragActivity.class));
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        UtilPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void addAutoStart(){
        try {
            final Intent intent = new Intent();
            String manufacturer = android.os.Build.MANUFACTURER;

            if ("xiaomi".equalsIgnoreCase(manufacturer)) {
                pkg = "com.miui.securitycenter";
                cls = "com.miui.permcenter.autostart.AutoStartManagementActivity";
            } else if ("oppo".equalsIgnoreCase(manufacturer)) {
                pkg = "com.coloros.safecenter";
                cls = "com.coloros.safecenter.permission.startup.StartupAppListActivity";
            } else if ("vivo".equalsIgnoreCase(manufacturer)) {
                pkg = "com.vivo.permissionmanager";
                cls = "com.vivo.permissionmanager.activity.BgStartUpManagerActivity";
            } else if ("Letv".equalsIgnoreCase(manufacturer)) {
                pkg = "com.letv.android.letvsafe";
                cls = "com.letv.android.letvsafe.AutobootManageActivity";
            } else if ("Honor".equalsIgnoreCase(manufacturer)) {
                pkg = "com.huawei.systemmanager";
                cls = "com.huawei.systemmanager.optimize.process.ProtectActivity";
            }

            if(pkg != null){
                AlertDialog.Builder autoStart = new AlertDialog.Builder(this);
                autoStart
                        .setMessage("please enable auto start properly working of some functions")
                        .setPositiveButton("OK", (dialog, which) -> {
                            dialog.dismiss();
                            intent.setComponent(new ComponentName(pkg,cls));
                            List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                            if  (list.size() > 0) {
                                startActivity(intent);
                            }
                            finish();
                        })
                        .setNegativeButton("NO", (dialog, which) -> {
                            dialog.dismiss();
                            // checkPermissions();
                        })
                        .setCancelable(false)
                        .show();
            }
            // else checkPermissions();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}