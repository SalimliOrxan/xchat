package com.example.orxan.xchat;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

public class Photo_Storage {
    private static boolean success;
    private static Bitmap my_image;
    private static File localFile;

    public void setImage(String phone){
        Uri filePath = Uri.parse("android.resource://com.example.orxan.xchat/" + R.drawable.avatar);

        StorageReference profile_photos = FirebaseStorage.getInstance().getReference("profile_photos");
        //StorageReference photo = profile_photos.child(phone).child("profile.jpg");
        UploadTask task = profile_photos.putFile(filePath);
        success = false;
        task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.e("upload","success");
            }
        });
    }



}
