package com.example.orxan.xchat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;

class UtilPermission {

    private UtilPermission(){}

    private static final int CAMERA_REQUEST        = 10;
    private static final int READ_CONTACTS_REQUEST = 20;
    private static final int WRITE_STORAGE         = 30;
    private static final int READ_STORAGE          = 40;




    static boolean controlCamera(@NonNull Activity activity){
        boolean camera = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if(!camera){
            activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
            return false;
        }
        return true;
    }

    static boolean controlContacts(@NonNull Activity activity){
        boolean contacts = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        if(!contacts){
            activity.requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, READ_CONTACTS_REQUEST);
            return false;
        }
        return true;
    }

    static boolean controlStorage(@NonNull Activity activity){
        boolean storage = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if(!storage){
            activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_STORAGE);
            return false;
        }
        return true;
    }

    static void onRequestPermissionsResult(Activity activity, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case CAMERA_REQUEST:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // granted

                } else {
                    if(ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
                        // denied
                    } else {
                        // never ask again
                        showSettings(activity);
                    }
                }
                break;

            case READ_CONTACTS_REQUEST:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    activity.getFragmentManager().beginTransaction().add(R.id.main, new Login()).commit();
                } else {
                    if(ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS)) {
                        // denied
                        activity.finish();
                    } else {
                        // never ask again
                        showSettings(activity);
                    }
                }
                break;

            case READ_STORAGE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_STORAGE);
                } else {
                    if(ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        // denied
                        activity.finish();
                    } else {
                        // never ask again
                        showSettings(activity);
                    }
                }
                break;

            case WRITE_STORAGE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                } else {
                    if(ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        // denied
                        activity.finish();
                    } else {
                        // never ask again
                        showSettings(activity);
                    }
                }
                break;
        }
    }

    private static void showSettings(Activity activity){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder
                .setMessage("Need Permission")
                .setPositiveButton("setting", (dialog, which) -> {
                    dialog.dismiss();

                    Intent i = new Intent();
                    i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    i.addCategory(Intent.CATEGORY_DEFAULT);
                    i.setData(Uri.parse("package:" + activity.getPackageName()));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    activity.startActivity(i);
                })
                .setNegativeButton("close", (dialog, which) -> {
                    dialog.dismiss();
                    activity.finish();
                })
                .setCancelable(false)
                .show();
    }

    @SuppressLint("InflateParams")
    static AlertDialog showUploadBar(Context context){
        View view = LayoutInflater.from(context).inflate(R.layout.upload_screen, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);

        AlertDialog uploadDialog = builder.create();
        uploadDialog.setCancelable(false);
        uploadDialog.show();

        if(uploadDialog.getWindow() != null){
            uploadDialog.getWindow().getDecorView().setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        }

        return uploadDialog;
    }
}