package com.example.orxan.xchat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MyNotification extends FirebaseMessagingService {

    private static final int ID = 12345;
    private static final String NOTIFICATION_REPLY = "NOTIFICATION_REPLY";

    private static String from, sender, name, tempFrom = "from";
    private static NotificationManager manager;
    private static List<String>messages = new ArrayList<>();

    private NotificationCompat.Builder builder;
    private String channel = "Channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if(remoteMessage.getData() != null){
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            builder = new NotificationCompat.Builder(this,channel);
            builder.setAutoCancel(true);

            String msg = remoteMessage.getData().get("body");
            from = remoteMessage.getData().get("phone");
            sender = getSharedPreferences("Xchat",MODE_PRIVATE).getString("sender", null);

            HashMap <String,String> contacts = Contacts.getContactList(this);
            name = contacts.get(from);

            if(name != null && !getSharedPreferences("Xchat",MODE_PRIVATE).getString("status",null).equals(name)){
                if(!tempFrom.equals(from)) messages.clear();
                messages.add(msg);
                tempFrom = from;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    oreo();
                }else{
                    preOreo();
                }
            }
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        SharedPreferences pref = getSharedPreferences("Xchat",MODE_PRIVATE);
        if(pref.getString("sender",null) != null){
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users/" + pref.getString("sender",null) + "/token");
            ref.setValue(token);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void oreo(){
        NotificationChannel myChannel;

        String channel_id = "my channel";
        if (manager.getNotificationChannel(channel_id) == null) {
            myChannel = new NotificationChannel(channel_id,channel, NotificationManager.IMPORTANCE_HIGH);
            //AudioAttributes attributes = new AudioAttributes.Builder()
                    //.setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    //.build();
            //myChannel.setSound(uri,attributes);
            myChannel.enableLights(true);
            myChannel.enableVibration(true);

            manager.createNotificationChannel(myChannel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteInput remoteInput = new RemoteInput.Builder(NOTIFICATION_REPLY).setLabel("enter message").build();
        Intent i = new Intent(this, QuickReplyReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 2, i, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.mipmap.xchat, "REPLY", pi).addRemoteInput(remoteInput).setAllowGeneratedReplies(true).build();

        builder
                .setStyle(getMessagingStyle())
                .setColor(Color.GREEN)
                .setChannelId(channel_id)
                .setSmallIcon(R.mipmap.notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.notification))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .addAction(action);

        manager.notify(ID, builder.build());
    }

    private void preOreo(){
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteInput remoteInput;
        Intent i;
        PendingIntent pi;
        NotificationCompat.Action action;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            remoteInput = new RemoteInput.Builder(NOTIFICATION_REPLY).setLabel("enter message").build();
            i = new Intent(this, QuickReplyReceiver.class);
            pi = PendingIntent.getBroadcast(this, 1, i, PendingIntent.FLAG_UPDATE_CURRENT);
            action = new NotificationCompat.Action.Builder(R.mipmap.xchat, "REPLY", pi).addRemoteInput(remoteInput).setAllowGeneratedReplies(true).build();
        }else{
            i = new Intent(this, QuickReplyActivity.class);
            i.setFlags(FLAG_ACTIVITY_NEW_TASK|FLAG_ACTIVITY_CLEAR_TASK);
            pi = PendingIntent.getActivity(this, 2, i, PendingIntent.FLAG_UPDATE_CURRENT);
            action = new NotificationCompat.Action.Builder(R.mipmap.xchat, "REPLY", pi).setAllowGeneratedReplies(true).build();
        }

        builder
                .setStyle(getMessagingStyle())
                .setColor(Color.GREEN)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setSmallIcon(R.mipmap.notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.notification))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .addAction(action);

        manager.notify(ID, builder.build());
    }

    private NotificationCompat.MessagingStyle getMessagingStyle() {
        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle("Me");
        for (String message : messages) {
            messagingStyle.addMessage(message,System.currentTimeMillis(),name);
        }
        return messagingStyle;
    }


    public static int getID() {
        return ID;
    }

    public static String getFrom() {
        return from;
    }

    public static String getSender() {
        return sender;
    }

    public static String getName() {
        return name;
    }

    public static NotificationManager getManager() {
        return manager;
    }

    public static String getNotificationReply() {
        return NOTIFICATION_REPLY;
    }

    public static List<String> getMessages() {
        return messages;
    }
}