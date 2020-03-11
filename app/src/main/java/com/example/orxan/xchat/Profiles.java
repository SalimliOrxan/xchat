package com.example.orxan.xchat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Profiles extends android.support.v4.app.Fragment {

    private Context context;
    private DatabaseReference users;
    private HashMap<String,String> user_list;
    private Adapter adapter;
    private Object[] names;
    private Object[] phones;
    private HashMap<String,String> contacts;
    private HashMap<String,String> links;
    private SharedPreferences.Editor editor;
    private int itemCount;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.profile_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = view.getContext();

        editor = context.getSharedPreferences("Xchat",Context.MODE_PRIVATE).edit();
        setHasOptionsMenu(true);

        adapter = new Adapter();
        RecyclerView profile_list = view.findViewById(R.id.profile_list);
        LinearLayoutManager lm = new LinearLayoutManager(context);
        profile_list.setLayoutManager(lm);
        profile_list.setHasFixedSize(true);
        profile_list.setItemViewCacheSize(20);
        profile_list.setDrawingCacheEnabled(true);
        profile_list.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        profile_list.setAdapter(adapter);

        users = FirebaseDatabase.getInstance().getReference("users");
        user_list = new HashMap<>();
        Gson gson = new Gson();

        SharedPreferences pref = context.getSharedPreferences("Xchat",Context.MODE_PRIVATE);
        if(pref.getString("contacts",null) != null){
            String con = pref.getString("contacts",null);

            Type type = new TypeToken<HashMap<String, String>>(){}.getType();
            contacts = gson.fromJson(con,type);
        } else{
            contacts = Contacts.getContactList(context);
            String con = gson.toJson(contacts);
            editor.putString("contacts",con);
            editor.apply();
        }

        refreshUsers();
    }


    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {
        String name;
        String imgUri;

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.single_profile, parent, false);
            return new SingleProfile(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
            Log.e("onBindViewHolder",String.valueOf(position) + "-");
            name = String.valueOf(names[position]);
            imgUri = links.get(name);

            ((SingleProfile) holder).profile_name.setText(name);
            ((SingleProfile) holder).profile_name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SelectedProfile sp = new SelectedProfile();
                    Bundle b = new Bundle();
                    b.putString("name", names[holder.getAdapterPosition()].toString());
                    b.putString("phone", phones[holder.getAdapterPosition()].toString());
                    //b.putByteArray("image",byteArray);
                    sp.setArguments(b);

                    editor.putString("status", names[holder.getAdapterPosition()].toString());
                    editor.apply();

                    android.support.v4.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.frame, sp);
                    transaction.setTransition(android.support.v4.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    transaction.addToBackStack(null);
                    transaction.commit();

                    Intent intent = new Intent();
                    intent.putExtra("name", names[holder.getAdapterPosition()].toString());
                    intent.setAction("CLICKED");

                    phones = user_list.keySet().toArray();
                    names = user_list.values().toArray();
                    itemCount = user_list.size();
                    notifyDataSetChanged();

                    getActivity().sendBroadcast(intent);
                }
            });

            if (imgUri == null) {
                ((SingleProfile) holder).image.setImageDrawable(context.getResources().getDrawable(R.drawable.avatar));
            } else
                Glide.with(context).load(imgUri).apply(RequestOptions.circleCropTransform()).into(((SingleProfile) holder).image);
        }

        @Override
        public int getItemCount() {
            return itemCount;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public Filter getFilter() {
            return exampleFilter;
        }

        private Filter exampleFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                ArrayList<String> list = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    list.addAll(user_list.values());
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    Iterator phone = user_list.keySet().iterator();
                    HashSet<String> newPhones = new HashSet<>();

                    for(Object name : user_list.values()) {
                        String number = phone.next().toString();
                        Log.e("phone",number + "-");

                        if (name.toString().toLowerCase().contains(filterPattern)) {
                            list.add(name.toString());
                            newPhones.add(number);
                        }
                    }
                    phones = newPhones.toArray();

                }

                FilterResults results = new FilterResults();
                results.values = list;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                Log.e("results", results.values.toString() +"-");
                ArrayList<String> list = new ArrayList<>();
                list.addAll((List) results.values);
                Log.e("size", user_list.size()+"-");
                itemCount = list.size();
                names = new Object[list.size()];
                Log.e("namessize", names.length+"-");
                for(int i=0;i<list.size();i++){
                    names[i] = list.get(i);
                    Log.e("name", list.get(i)+"-");
                }

                notifyDataSetChanged();
            }
        };
    }

    private class SingleProfile extends RecyclerView.ViewHolder{
        private ImageView image;
        private TextView profile_name;

        private SingleProfile(View itemView) {
            super(itemView);

            profile_name = itemView.findViewById(R.id.profile_name);
            image = itemView.findViewById(R.id.profile_view);
        }
    }

    private void refreshUsers(){
        users.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                links = new HashMap<>();

                for(String contact : contacts.keySet()){
                    if(dataSnapshot.hasChild(contact)){
                        user_list.put(contact, contacts.get(contact));
                        if(dataSnapshot.child(contact).hasChild("image"))
                            links.put(contacts.get(contact),String.valueOf(dataSnapshot.child(contact).child("image").getValue()));
                    }
                }
                itemCount = user_list.size();
                phones = user_list.keySet().toArray();
                names = user_list.values().toArray();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_search, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                Log.e("newText", String.valueOf(newText) +"-");
                return false;
            }
        });
    }
}