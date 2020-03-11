package com.example.orxan.xchat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

class UpdateHelper {
    private static final String VERSION_NAME_KEY = "version";
    private static final String URL_KEY = "url";
    private static final String APP_NAME = "Xchat.apk";
    private static final String FILE_LENGTH = "file_length";

    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private static String target_url;
    private static TextView bar;
    private static double file_length;
    private static AlertDialog dialog;


    private UpdateHelper() {}

    static UpdateHelper getInstance(){
        return new UpdateHelper();
    }

    void init(Context context){
        HashMap<String, Object> firebaseDefaultMap = new HashMap<>();
        firebaseDefaultMap.put(VERSION_NAME_KEY, getCurrentVersionName(context));
        firebaseDefaultMap.put(URL_KEY, URL_KEY);
        firebaseDefaultMap.put(FILE_LENGTH, FILE_LENGTH);

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseRemoteConfig.setDefaults(firebaseDefaultMap);
        mFirebaseRemoteConfig.setConfigSettings(new FirebaseRemoteConfigSettings.Builder().setDeveloperModeEnabled(BuildConfig.DEBUG).build());
        mFirebaseRemoteConfig.fetch(5).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                mFirebaseRemoteConfig.activateFetched();
                checkForUpdate(context);
            } else {
                Log.e("RemoteConfig fetch","error");
            }
        });
    }

    private void checkForUpdate(final Context context) {
        String latestAppVersion = mFirebaseRemoteConfig.getString(VERSION_NAME_KEY);

        if (!latestAppVersion.equalsIgnoreCase(getCurrentVersionName(context))) {
            AlertDialog.Builder update = new AlertDialog.Builder(context);
            update
                    .setTitle("Update is available")
                    .setMessage("update app")
                    .setPositiveButton("update", (dialog, which) -> {
                        target_url = mFirebaseRemoteConfig.getString(URL_KEY);
                        file_length = mFirebaseRemoteConfig.getDouble(FILE_LENGTH);
                        downloadProcess(context);
                        new DownloadApkTask(context).execute();
                        dialog.dismiss();
                    })
                    .setNegativeButton("later", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .show();
        }
    }

    private void downloadProcess(Context context){
        View view = LayoutInflater.from(context).inflate(R.layout.upload,null);
        AlertDialog.Builder percentage = new AlertDialog.Builder(context);
        percentage
                .setView(view)
                .setCancelable(false);
        dialog = percentage.create();
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        bar = view.findViewById(R.id.barText);
    }

    private String getCurrentVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class DownloadApkTask extends AsyncTask<Void, Void, Void> {

        WeakReference<Context> context;

        private DownloadApkTask(Context cont){
            context = new WeakReference<>(cont);
        }

        @Override
        protected Void doInBackground(Void... params) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(target_url);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    String error =  "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                    Log.e("error",error);
                }

                input = connection.getInputStream();
                File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APP_NAME);
                output = new FileOutputStream(f);
                byte data[] = new byte[4096];
                int count;
                int percent = 0;
                while ((count = input.read(data)) != -1) {
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }

                    percent+=count;
                    //bar.setText(String.valueOf((int)(percent * 100 / file_length)).concat("%"));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                    ignored.printStackTrace();
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APP_NAME);
            Intent intent;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri fileUri = FileProvider.getUriForFile(context.get(), context.get().getPackageName() + ".myFileProvider", file);
                intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                intent.setData(fileUri);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                Uri fileUri = Uri.fromFile(file);
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            dialog.cancel();
            context.get().startActivity(intent);
        }
    }
}