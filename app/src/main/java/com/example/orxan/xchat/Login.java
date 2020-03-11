package com.example.orxan.xchat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.jwang123.flagkit.FlagKit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.content.Context.MODE_PRIVATE;

public class Login extends Fragment {

    private EditText phone,code,username;
    private Button send_verification_code,verify;
    private Spinner codeSpinner;
    private ImageButton start;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private FirebaseAuth mAuth;
    private String verificationId;
    private String token, selectedPrefix;
    private FirebaseUser user;
    private Context context;
    private List<PrefixModel> prefixList;
    private AlertDialog uploadDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.login_frag, container,false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = view.getContext();

        addCodeForTest();

        codeSpinner = view.findViewById(R.id.codeSpinner);
        username = view.findViewById(R.id.username);
        phone = view.findViewById(R.id.your_phone);
        code = view.findViewById(R.id.code);
        send_verification_code = view.findViewById(R.id.send_verification_code);
        verify = view.findViewById(R.id.verify);
        start = view.findViewById(R.id.start);

        mAuth = FirebaseAuth.getInstance();

        verify.setOnClickListener(v -> {
            if(code.getText().length() == 6){
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code.getText().toString());
                signInWithPhoneAuthCredential(credential);
            } else printToast("code must be 6 digits");
        });

        send_verification_code.setOnClickListener(v -> {
            if(phone.getText().toString().length() > 5){
                FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
                    if(!task.isSuccessful()) {
                        Log.e("token", String.valueOf(task.getException()));
                        printToast("invalid number");
                        return;
                    }
                    token = task.getResult().getToken();

                    DatabaseReference user = FirebaseDatabase.getInstance().getReference("users/" + selectedPrefix.concat(phone.getText().toString()));
                    user.child("token").setValue(token);
                });


                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        selectedPrefix.concat(phone.getText().toString()),
                        60,
                        TimeUnit.SECONDS,
                        getActivity(),
                        mCallbacks);
            } else printToast("invalid number");
        });

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                System.out.println(e.getLocalizedMessage());
                printToast("try again later");
            }

            @Override
            public void onCodeSent(String id, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(id, forceResendingToken);

                verificationId = id;

                codeSpinner.setVisibility(View.GONE);
                phone.setVisibility(View.GONE);
                send_verification_code.setVisibility(View.GONE);
                code.setVisibility(View.VISIBLE);
                verify.setVisibility(View.VISIBLE);
            }
        };

        setCodeSpinner();
    }




    private void addCodeForTest(){
        prefixList = new ArrayList<>();

        PrefixModel pre = new PrefixModel();
        pre.setPhonePrefix("+994");
        pre.setCountryCode("az");
        prefixList.add(pre);

        pre = new PrefixModel();
        pre.setPhonePrefix("+90");
        pre.setCountryCode("tr");
        prefixList.add(pre);

        pre = new PrefixModel();
        pre.setPhonePrefix("+7");
        pre.setCountryCode("ru");
        prefixList.add(pre);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        uploadDialog = UtilPermission.showUploadBar(context);

        mAuth.signInWithCredential(credential).addOnCompleteListener(getActivity(), task -> {
            if(task.isSuccessful()){
                uploadDialog.cancel();
                codeSpinner.setVisibility(View.GONE);
                phone.setVisibility(View.GONE);
                send_verification_code.setVisibility(View.GONE);
                code.setVisibility(View.GONE);
                verify.setVisibility(View.GONE);
                username.setVisibility(View.VISIBLE);
                start.setVisibility(View.VISIBLE);

                printToast("successfully signed");
                user = task.getResult().getUser();

                start.setOnClickListener(v -> {
                    String name;
                    if(username.getText().length() > 1) {
                        name = username.getText().toString();
                    } else name = "user";

                    SharedPreferences.Editor editor = getActivity().getApplicationContext().getSharedPreferences("Xchat",MODE_PRIVATE).edit();
                    editor.putBoolean("isLogin", true);
                    editor.putString("sender", user.getPhoneNumber());
                    editor.putString("name", name);
                    editor.apply();

                    DatabaseReference database = FirebaseDatabase.getInstance().getReference("users");
                    database.child(String.valueOf(user.getPhoneNumber())).child("name").setValue(name);

                    startActivity(new Intent(getActivity(), FragActivity.class));
                    getActivity().finish();
                });
            } else {
                uploadDialog.cancel();
                if(task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                    printToast("code is invalid");
                }
            }
        });
    }

    private void printToast(String message){
        Toast.makeText(getActivity().getApplicationContext(),message,Toast.LENGTH_LONG).show();
    }

    private void setCodeSpinner(){
        codeSpinner.setAdapter(new MySpinnerAdapter());

        codeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPrefix = prefixList.get(position).getPhonePrefix();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private class MySpinnerAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return prefixList.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Holder holder;

            View view = convertView;
            if(view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.country_item, null);
                holder = new Holder();

                holder.flag   = view.findViewById(R.id.itemPrefixFlag);
                holder.prefix = view.findViewById(R.id.itemPrefix);

                view.setTag(holder);
            } else holder = (Holder) view.getTag();

            holder.prefix.setText("(".concat(prefixList.get(position).getPhonePrefix()).concat(")"));
            holder.flag.setImageDrawable(FlagKit.drawableWithFlag(context, prefixList.get(position).getCountryCode().toLowerCase()));

            return view;
        }
    }

    private static class Holder{
        private ImageView flag;
        private TextView  prefix;
    }
}
