package com.example.orxan.xchat;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class QuickReplyActivity extends AppCompatActivity {

    private String message, sender, receiver;
    private DatabaseReference chat = null;
    private int chat_length;
    private EditText reply_message;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quick_reply_layout);
        setTitle(MyNotification.getName());

        sender = MyNotification.getSender();
        receiver = MyNotification.getFrom();

        ImageButton reply = findViewById(R.id.reply);
        reply_message = findViewById(R.id.reply_message);

        reply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(reply_message.getText().length() > 0){
                    message = reply_message.getText().toString();
                    controlChat(sender + receiver, false);
                    MyNotification.getManager().cancel(MyNotification.getID());
                    MyNotification.getMessages().clear();
                    finish();
                }
            }
        });
    }

    private void controlChat(final String path, final boolean both){
        final DatabaseReference chat1 = FirebaseDatabase.getInstance().getReference("chat");

        chat1.orderByKey().equalTo(path).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(path)){
                    chat1.removeEventListener(this);
                    chat = FirebaseDatabase.getInstance().getReference("chat/" + path);
                    getChat_length();
                }else{
                    if(!both){
                        controlChat( receiver + sender,true);
                    }else{
                        chat1.removeEventListener(this);
                        chat = FirebaseDatabase.getInstance().getReference("chat/" + path);
                        getChat_length();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("onCancelled","databaseError");
            }
        });
    }

    private void getChat_length(){
        chat.child("chat_length").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chat_length = Integer.parseInt(dataSnapshot.getValue().toString());
                chat.removeEventListener(this);
                sendMessage();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void sendMessage(){
        chat.child(String.valueOf(chat_length)).child("from").setValue(sender);
        chat.child(String.valueOf(chat_length)).child("to").setValue(receiver);
        chat.child(String.valueOf(chat_length)).child("message").setValue(message);
        chat_length++;
        chat.child("chat_length").setValue(chat_length);
    }
}
