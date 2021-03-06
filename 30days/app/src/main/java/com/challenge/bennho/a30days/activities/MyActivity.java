package com.challenge.bennho.a30days.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.appodeal.ads.Appodeal;
import com.challenge.bennho.a30days.MyApplication;
import com.challenge.bennho.a30days.R;
import com.challenge.bennho.a30days.helpers.AdsMediation;
import com.challenge.bennho.a30days.helpers.Analytics;
import com.challenge.bennho.a30days.helpers.DrawerHelper;
import com.challenge.bennho.a30days.helpers.Logs;
import com.challenge.bennho.a30days.helpers.OverlayBuilder;
import com.challenge.bennho.a30days.helpers.ProVersionHelpers;

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

        showBanner();
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
        drawerHelper.onResume();

    }

    @Override
    public void onBackPressed() {

        Intent upIntent = NavUtils.getParentActivityIntent(this);
        if(upIntent == null){
            super.onBackPressed();
        }
        else{
            finish();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }



//        Intent upIntent = NavUtils.getParentActivityIntent(this);
//        if(upIntent == null){
//            finish();
//        }
//        else{
//            if(NavUtils.shouldUpRecreateTask(this, upIntent)){
//                // This activity is NOT part of this app's task, so create a new task
//                // when navigating up, with a synthesized back stack.
//                TaskStackBuilder.create(this)
//                        // Add all of this activity's parents to the back stack
//                        .addNextIntentWithParentStack(upIntent)
//                        // Navigate up to the closest parent
//                        .startActivities();
//            }
//            else {
//                // This activity is part of this app's task, so simply
//                // navigate up to the logical parent activity.
//                //NavUtils.navigateUpTo(this, upIntent);
//
//                super.onBackPressed();
//            }
//        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                this.onBackPressed();
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

    protected void hideBanner(){
        AdsMediation.hideBanner(this);
        onAdsBannerHeightDetermined(0);
    }

    protected void showBanner(){
        AdsMediation.showBanner(this, new AdsMediation.AdsListener() {
            @Override
            public void onBannerShown(int heightPixel) {
                onAdsBannerHeightDetermined(heightPixel);
            }
        });
    }

    private void onAdsBannerHeightDetermined(int heightPixel){
        if(adsLayout != null){
            adsLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    heightPixel));
            adsLayout.setVisibility(View.VISIBLE);
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

    public ProVersionHelpers getProVersionHelpers(){
        return ((MyApplication) getApplication()).getProVersionHelpers();
    }

    public void purchasePro(){
        OverlayBuilder.build(this)
                .setTitle(getString(R.string.pro_version_title))
                .setContent(getString(R.string.pro_version_content))
                .setOverlayType(OverlayBuilder.OverlayType.OkCancel)
                .setRunnables(new Runnable() {
                    @Override
                    public void run() {
                        getProVersionHelpers().purchasePro(MyActivity.this,
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        AdsMediation.hideBanner(MyActivity.this);
                                        if(adsLayout != null){
                                            adsLayout.setVisibility(View.GONE);
                                        }
                                    }
                                });
                    }
                })
                .show();



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(!((MyApplication) getApplication()).getProVersionHelpers()
                .onActivityResult(requestCode, resultCode, data)){
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    protected boolean isPaused() {
        return paused;
    }


}
