package com.challenge.bennho.a30days.helpers;

import android.app.Activity;

import com.challenge.bennho.a30days.BuildConfig;
import com.challenge.bennho.a30days.MyApplication;
import com.challenge.bennho.a30days.enums.AnalyticEvent;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;


/**
 * Created by SiongLeng on 13/9/2016.
 */
public class Analytics {

    private static Tracker _tracker;

    public static void logToScreen(Activity activity){
        if(BuildConfig.DEBUG_MODE) return;

        Tracker tracker = getTracker(activity);
        if(tracker == null) return;

        tracker.setScreenName(activity.getClass().getSimpleName());
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public static void logEvent(AnalyticEvent analyticEvent){
        logEvent(analyticEvent.name(), "");
    }

    public static void logEvent(AnalyticEvent analyticEvent, String value){
        logEvent(analyticEvent.name(), value);
    }

    private static void logEvent(String eventName, String value){
        if(BuildConfig.DEBUG_MODE) return;

        Tracker tracker = getTracker(null);
        if(tracker == null) return;

        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("30Days")
                .setAction(eventName)
                .setLabel(value)
                .build());

    }

    public static Tracker getTracker(Activity activity) {
        if(_tracker == null && activity != null){
            MyApplication application = (MyApplication) activity.getApplication();
            _tracker = application.getDefaultTracker();
        }
        return _tracker;
    }

    public static void setTracker(Tracker tracker) {
        Analytics._tracker = tracker;
    }
}
