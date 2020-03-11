package com.example.orxan.xchat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

import static android.app.Activity.RESULT_OK;

public class My_Profile extends android.support.v4.app.Fragment {

    private Context context;
    private TextView name, barText;
    private ImageView image;
    private String phone;
    private AlertDialog uploadDialog;
    private Button font, remove_photo, log_out;
    private View mainView;
    private SharedPreferences pref;

    private final int PICK_IMAGE_REQUEST = 111;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.my_profile_layout,container,false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainView = view;
        context = view.getContext();

        pref = context.getSharedPreferences("Xchat", Context.MODE_PRIVATE);

        name  = view.findViewById(R.id.name_change);
        image = view.findViewById(R.id.photo_change);
        phone = pref.getString("sender",null);
        font  = view.findViewById(R.id.font);
        remove_photo = view.findViewById(R.id.remove_photo);
        log_out = view.findViewById(R.id.log_out);

        name.setText(pref.getString("name","name"));
        handleClicks();
    }


    @SuppressLint("InflateParams")
    private void handleClicks(){
        final DatabaseReference img = FirebaseDatabase.getInstance().getReference("users/" + phone + "/image");
        img.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() != null)
                    Glide.with(context).load(dataSnapshot.getValue()).apply(RequestOptions.circleCropTransform()).into(image);
                img.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        font.setOnClickListener(v -> {
            View vi = LayoutInflater.from(context).inflate(R.layout.font,null);
            TextView small = vi.findViewById(R.id.small);
            TextView medium = vi.findViewById(R.id.medium);
            TextView large = vi.findViewById(R.id.large);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(vi);
            final AlertDialog dialog = builder.create();
            dialog.show();

            if(dialog.getWindow() != null)
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            small.setOnClickListener(v1 -> {
                setSize(14f);
                dialog.cancel();
            });
            medium.setOnClickListener(v12 -> {
                setSize(18f);
                dialog.cancel();
            });
            large.setOnClickListener(v13 -> {
                setSize(24f);
                dialog.cancel();
            });
        });

        remove_photo.setOnClickListener(v -> {
            showUploadBar();
            Glide.with(context).load(R.drawable.avatar).apply(RequestOptions.circleCropTransform()).into(image);
            final DatabaseReference image = FirebaseDatabase.getInstance().getReference("users/" + phone + "/image");
            image.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.getValue() != null) image.removeValue();
                    uploadDialog.cancel();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    uploadDialog.cancel();
                }
            });
        });

        log_out.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("are you sure?")
                    .setPositiveButton("yes", (dialog, which) -> {
                        dialog.dismiss();

                        if(FirebaseAuth.getInstance().getCurrentUser() != null){
                            pref.edit().clear().apply();
                            FirebaseAuth.getInstance().signOut();

                            startActivity(new Intent(getActivity(), MainActivity.class));
                            ((FragActivity) context).finish();
                        }
                    })
                    .setNegativeButton("no", (dialog, which) -> dialog.dismiss());
            final AlertDialog dialog = builder.create();
            dialog.show();
            if(dialog.getWindow() != null)
                dialog.getWindow().getDecorView().setBackground(ContextCompat.getDrawable(context, R.drawable.round_button));
        });

        image.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_PICK);
            startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
        });

        name.setOnClickListener(v -> {
            View vi = LayoutInflater.from(context).inflate(R.layout.name,null);
            final EditText newName = vi.findViewById(R.id.alert_name);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(vi);
            builder.setPositiveButton("ok", (dialog, which) -> {
                if(newName.getText().toString().length() > 0) {
                    String text = String.valueOf(newName.getText());
                    name.setText(text);
                    FirebaseDatabase.getInstance().getReference("users/" + phone + "/name").setValue(text);
                    pref.edit().putString("name", text).apply();
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

            if(dialog.getWindow() != null){
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri filePath = data.getData();
            showUploadBar();

            String extension = ".jpeg";

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), filePath);
                Glide.with(context).load(compress(bitmap)).apply(RequestOptions.circleCropTransform()).into(image);

                final StorageReference profile_photo = FirebaseStorage.getInstance().getReference("profile_photos/" + phone + "/photo" + extension);
                UploadTask task = profile_photo.putBytes(compress(bitmap));

                task.addOnSuccessListener(taskSnapshot -> profile_photo.getDownloadUrl().addOnSuccessListener(uri -> {
                    FirebaseDatabase.getInstance().getReference("users/" + phone).child("image").setValue(uri.toString());
                    uploadDialog.cancel();
                    Snackbar snackbar = Snackbar.make(mainView,"upload is successful", Snackbar.LENGTH_LONG).setAction("Action", null);
                    snackbar.getView().setBackgroundColor(ContextCompat.getColor(context, R.color.green));
                    snackbar.show();
                }))
                        .addOnFailureListener(e -> {
                            uploadDialog.cancel();
                            Snackbar snackbar = Snackbar.make(mainView,"upload is unsuccessful", Snackbar.LENGTH_LONG).setAction("Action", null);
                            snackbar.getView().setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
                            snackbar.show();
                        })
                        .addOnProgressListener(taskSnapshot -> {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            barText.setText(String.valueOf((int)progress).concat("%"));
                        });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] compress(Bitmap bitmap) {
        byte[] img;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
        img = baos.toByteArray();
        return img;
    }

    @SuppressLint("InflateParams")
    private void showUploadBar(){
        try {
            View vi = LayoutInflater.from(context).inflate(R.layout.upload, null);
            barText = vi.findViewById(R.id.barText);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(vi);

            uploadDialog = builder.create();
            uploadDialog.setCancelable(false);
            uploadDialog.show();

            if(uploadDialog.getWindow() != null){
                uploadDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                uploadDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            }
        } catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    private void setSize(float size){
        pref.edit().putFloat("textSize", size).apply();

        if(getFragmentManager() != null){
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame, new EmptyFrag())
                    .setTransition(android.support.v4.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
