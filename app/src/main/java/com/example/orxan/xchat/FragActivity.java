package com.example.orxan.xchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class FragActivity extends AppCompatActivity {

    private ViewPager pager;
    private TabLayout tabLayout;
    private Toolbar toolbar;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.frag_activity);

        Adapter adapter = new Adapter(getSupportFragmentManager());
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pager = findViewById(R.id.pager);
        pager.setAdapter(adapter);

        tabLayout = findViewById(R.id.tabs);
        tabLayout.getTabAt(0).setText("Profiles");
        tabLayout.getTabAt(1).setText("Chat");
        tabLayout.getTabAt(2).setText("My Profile");

        pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(pager));

        registerReceiver(clickedReceiver, new IntentFilter("CLICKED"));
    }

    private class Adapter extends FragmentPagerAdapter {

        private Adapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position){
                case 0: return new Profiles();
                case 1: return new Frame();
                default: return new My_Profile();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

    private BroadcastReceiver clickedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent.getAction().equals("CLICKED")) {
                    tabLayout.getTabAt(1).setText(intent.getStringExtra("name"));
                    pager.setCurrentItem(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        MyNotification.getMessages().clear();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                AppBarLayout.LayoutParams p = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
                switch(tab.getPosition()){
                    case 0:
                        p.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
                        toolbar.setLayoutParams(p);break;
                    default:
                        p.setScrollFlags(0);
                        toolbar.setLayoutParams(p);break;
                }

                editor = getSharedPreferences("Xchat",MODE_PRIVATE).edit();
                editor.putString("status", String.valueOf(tab.getText()));
                editor.apply();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        editor = getSharedPreferences("Xchat",MODE_PRIVATE).edit();
        editor.putString("status","onPause");
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(clickedReceiver);
        editor = getSharedPreferences("Xchat",MODE_PRIVATE).edit();
        editor.putString("status","onDestroy");
        editor.apply();
    }
}