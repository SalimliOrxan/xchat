package com.example.orxan.xchat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class SelectedProfile extends android.support.v4.app.Fragment {

    private static final int SENT = 1;
    private static final int RECEIVE = 2;

    private Context context;
    private DatabaseReference chat_length_ref;
    private DatabaseReference chat = null;
    private EditText message;
    private String sender;
    private String receiver;
    private HashMap<Integer,String> messages;
    private HashMap<Integer,Integer> who;
    private RecyclerView recyclerView;
    private Adapter adapter;
    private SharedPreferences pref;
    private int chat_length = 1;
    private boolean control;
    private float textSize;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        try {
            context = container.getContext();
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        return inflater.inflate(R.layout.selected_profile_frag, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        control = false;
        adapter = new Adapter();

        pref = context.getSharedPreferences("Xchat",Context.MODE_PRIVATE);
        sender = pref.getString("sender",null);
        if(getArguments() != null){
            receiver = getArguments().getString("phone");
        }

        message = view.findViewById(R.id.message);
        ImageButton send = view.findViewById(R.id.send);
        ImageButton attach = view.findViewById(R.id.attach);

        messages = new HashMap<>();
        who = new HashMap<>();

        recyclerView = view.findViewById(R.id.recycler);
        LinearLayoutManager lm = new LinearLayoutManager(context);
        lm.setStackFromEnd(true);//for keyboard
        recyclerView.setLayoutManager(lm);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setAdapter(adapter);

        attach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, String.valueOf("will be available soon"), Toast.LENGTH_LONG).show();
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(message.getText().length() > 0){
                    chat.child(String.valueOf(chat_length)).child("from").setValue(sender);
                    chat.child(String.valueOf(chat_length)).child("to").setValue(receiver);
                    chat.child(String.valueOf(chat_length)).child("message").setValue(message.getText().toString());
                    chat_length++;
                    chat_length_ref.setValue(chat_length);
                    message.setText("");
                }
            }
        });

        controlChat(sender + receiver, false);
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;

            switch (viewType){
                case 0:
                    view = LayoutInflater.from(context).inflate(R.layout.sent_messages,parent,false);
                    return new SentMessageHolder(view);
                default:
                    view = LayoutInflater.from(context).inflate(R.layout.received_messages,parent,false);
                    return new ReceivedMessageHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position){
            textSize = pref.getFloat("textSize", 0f);
            if(textSize == 0f) textSize = 18f;

            switch (holder.getItemViewType()){
                case 0:
                    ((SentMessageHolder) holder).sent.setText(String.valueOf(messages.get(position)));
                    ((SentMessageHolder) holder).sent.setTextSize(textSize); break;
                default:
                    ((ReceivedMessageHolder) holder).received.setText(String.valueOf(messages.get(position)));
                    ((ReceivedMessageHolder) holder).received.setTextSize(textSize); break;
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        @Override
        public int getItemViewType(int position) {
            int row;
            if(who.get(position) == SENT) row = 0;
            else row = 1;
            return row;
        }

        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder{
        TextView received;
        private ReceivedMessageHolder(View itemView) {
            super(itemView);
            received = itemView.findViewById(R.id.received_message_body);
        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder{
        TextView sent;
        private SentMessageHolder(View itemView) {
            super(itemView);
            sent = itemView.findViewById(R.id.sent_message_body);
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
                    refreshLength();
                }else{
                    if(!both){
                        controlChat(receiver + sender,true);
                    }else{
                        chat1.removeEventListener(this);
                        chat = FirebaseDatabase.getInstance().getReference("chat/" + path);
                        refreshLength();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("onCancelled","databaseError");
            }
        });
    }

    private void refreshLength(){
        chat_length_ref = chat.child("chat_length");

        chat_length_ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() != null){
                    chat_length = Integer.parseInt(String.valueOf(dataSnapshot.getValue()));
                    control = true;
                    refreshMessages();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void refreshMessages(){
        chat.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(control) {
                    for (int i = 1; i < chat_length; i++) {
                        if (String.valueOf(dataSnapshot.child(String.valueOf(i)).child("to").getValue()).equals(receiver)) {
                            String msg = String.valueOf(dataSnapshot.child(String.valueOf(i)).child("message").getValue());
                            messages.put(i - 1, msg);
                            who.put(i - 1, SENT);
                        } else {
                            String msg = String.valueOf(dataSnapshot.child(String.valueOf(i)).child("message").getValue());
                            messages.put(i - 1, msg);
                            who.put(i - 1, RECEIVE);
                        }
                    }

                    control = false;
                    chat.removeEventListener(this);
                    adapter.notifyItemInserted(messages.size() - 1);
                    recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        TabLayout tabs = getActivity().findViewById(R.id.tabs);
        SharedPreferences.Editor editor = context.getSharedPreferences("Xchat",Context.MODE_PRIVATE).edit();
        editor.putString("status", String.valueOf(tabs.getTabAt(1).getText()));
        editor.apply();
    }
}
