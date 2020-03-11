package com.example.orxan.xchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class QuickReplyReceiver extends BroadcastReceiver {

    private DatabaseReference chat = null;
    private int chat_length;
    private String message, sender, receiver;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle input = RemoteInput.getResultsFromIntent(intent);

        if(input != null){
            message = input.getString(MyNotification.getNotificationReply());
            sender = MyNotification.getSender();
            receiver = MyNotification.getFrom();

            controlChat(sender + receiver, false);
            MyNotification.getManager().cancel(MyNotification.getID());
            MyNotification.getMessages().clear();
        }
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
                        controlChat(receiver + sender,true);
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
                Log.e("chat_length",dataSnapshot.getValue() + "-");
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
