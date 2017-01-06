package com.challenge.bennho.a30days.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.appodeal.ads.Appodeal;
import com.challenge.bennho.a30days.R;
import com.challenge.bennho.a30days.helpers.AdsMediation;
import com.challenge.bennho.a30days.helpers.Analytics;
import com.challenge.bennho.a30days.helpers.AndroidUtils;
import com.challenge.bennho.a30days.helpers.DrawerHelper;

/**
 * Created by sionglengho on 26/12/16.
 */

public abstract class MyActivity extends AppCompatActivity {

    private boolean paused;
    private RelativeLayout adsLayout;
    private Toolbar toolbar;
    private DrawerHelper drawerHelper;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.logToScreen(this);
        AdsMediation.init(this);


        AdsMediation.showBanner(this, new AdsMediation.AdsListener() {
            @Override
            public void onBannerShown(int heightPixel) {
                onAdsBannerHeightDetermined(heightPixel);
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        paused = false;

        AdsMediation.setBannerListener(this, new AdsMediation.AdsListener() {
            @Override
            public void onBannerShown(int heightPixel) {
                onAdsBannerHeightDetermined(heightPixel);
            }
        });

        Appodeal.onResume(this, Appodeal.BANNER);
        drawerHelper.refreshDayCounter();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                if(upIntent == null){
                    finish();
                }
                else{
                    if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                        // This activity is NOT part of this app's task, so create a new task
                        // when navigating up, with a synthesized back stack.
                        TaskStackBuilder.create(this)
                                // Add all of this activity's parents to the back stack
                                .addNextIntentWithParentStack(upIntent)
                                // Navigate up to the closest parent
                                .startActivities();
                    } else {
                        // This activity is part of this app's task, so simply
                        // navigate up to the logical parent activity.
                        NavUtils.navigateUpTo(this, upIntent);
                    }
                }

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void setActionBarVisibility(boolean visible){
        if(getSupportActionBar() != null){
            if(visible){
                getSupportActionBar().show();
            }
            else{
                getSupportActionBar().hide();
            }
        }
    }


    private void onAdsBannerHeightDetermined(int heightPixel){
        if(adsLayout != null){
            adsLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    heightPixel));
        }
    }

    protected void onLayoutSet(){
        toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        if(toolbar != null){
            setSupportActionBar(toolbar);
        }

        adsLayout = (RelativeLayout) findViewById(R.id.adLayout);
        if(adsLayout != null){
            adsLayout.setBackgroundColor(Color.BLACK);
        }

        drawerHelper = new DrawerHelper(this);
        drawerHelper.show();
    }


    public Toolbar getToolbar() {
        return toolbar;
    }

    protected boolean isPaused() {
        return paused;
    }


}
