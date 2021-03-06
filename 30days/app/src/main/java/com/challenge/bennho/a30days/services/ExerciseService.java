package com.challenge.bennho.a30days.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.challenge.bennho.a30days.R;
import com.challenge.bennho.a30days.activities.RunningActivity;
import com.challenge.bennho.a30days.enums.PreferenceType;
import com.challenge.bennho.a30days.helpers.CalculationHelper;
import com.challenge.bennho.a30days.helpers.MediaHelper;
import com.challenge.bennho.a30days.helpers.PreferenceUtils;
import com.challenge.bennho.a30days.helpers.TextSpeak;
import com.challenge.bennho.a30days.helpers.Threadings;
import com.challenge.bennho.a30days.models.ExerciseModel;
import com.challenge.bennho.a30days.models.ExercisePartModel;
import com.challenge.bennho.a30days.models.User;

/**
 * Created by sionglengho on 27/12/16.
 */

public class ExerciseService extends Service {

    private Binder binder;
    private ExerciseModel exerciseModel;
    private boolean skippingExercisePart;
    private float previousStatesElapsedMs, currentTotalElapsedMs, realElapsedMs, currentCaloriesBurnt;
    private long accSleepMs;
    private ExercisePartModel currentExercisePartModel;
    private ExerciseListener exerciseListener;
    private boolean paused, stopped, completed;
    private final int SERVICE_ID = 7012;
    private String lastNotificationText;
    private TextSpeak textSpeak;
    private User user;
    private PowerManager.WakeLock wakeLock;

    public ExerciseService() {
        this.binder = new LocalBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //initiate text speech first, as it will take a while
        getTextSpeak();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(wakeLock != null && wakeLock.isHeld()){
            wakeLock.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     *
     * @param exerciseModel
     * @param exerciseListener
     * @return true if start successfully, else false(cause already have a exist instance running)
     */
    public boolean startExercise(ExerciseModel exerciseModel, ExerciseListener exerciseListener){
        this.exerciseModel = exerciseModel;
        this.exerciseListener = exerciseListener;

        boolean exerciseIsRunning = PreferenceUtils.getBoolean(getBaseContext(), PreferenceType.ExerciseRunning);
        if(exerciseIsRunning){
            return false;
        }

        resetService();  //dispose previous unfinish exercise

        PreferenceUtils.putBoolean(getBaseContext(), PreferenceType.ExerciseRunning, true);
        this.user = new User(getBaseContext());
        user.reload();
        this.exerciseModel = exerciseModel;
        this.exerciseListener = exerciseListener;
        this.stopped = false;
        this.paused = false;
        this.completed = false;
        this.skippingExercisePart = false;
        this.previousStatesElapsedMs = 0;
        this.currentTotalElapsedMs = 0;
        this.currentCaloriesBurnt = 0;
        this.realElapsedMs = 0;
        this.lastNotificationText = "";
        exercising(0);
        startAsForeground();

        if(wakeLock != null && wakeLock.isHeld()){
            wakeLock.release();
        }

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                         "ExerciseServiceWakeLock");
        wakeLock.acquire();

        return true;
    }

    private void exercising(final int index){
        if(index >= exerciseModel.getExercisePartModels().size()){
            exerciseCompleted();
            return;
        }

        skippingExercisePart = false;

        previousStatesElapsedMs = currentTotalElapsedMs;
        currentExercisePartModel = exerciseModel.getExercisePartModels().get(index);

        if(exerciseListener != null){
            exerciseListener.onExercisePartChanged(currentExercisePartModel);
            exerciseListener.onTimeChanged(currentTotalElapsedMs, 0, currentCaloriesBurnt,
                                                currentExercisePartModel);
            speakOutCurrentExercisePart(currentExercisePartModel);
            updateNotification(0, currentExercisePartModel);
        }


        Threadings.runInBackground(new Runnable() {
            @Override
            public void run() {

                final long sleepMs = 100;
                accSleepMs = 0;

                while (accSleepMs < currentExercisePartModel.getDurationSecs() * 1000){
                    if(skippingExercisePart){
                        currentTotalElapsedMs = previousStatesElapsedMs +
                                currentExercisePartModel.getDurationSecs() * 1000;
                        exercising(index + 1);
                        return;
                    }

                    Threadings.sleep(sleepMs);

                    if(paused){
                        continue;
                    }

                    if(stopped){
                        return;
                    }

                    accSleepMs += sleepMs;
                    currentTotalElapsedMs += sleepMs;
                    realElapsedMs += sleepMs;

                    updateNotification(accSleepMs, currentExercisePartModel);
                    updateCaloriesBurnt(sleepMs, currentExercisePartModel);
                    if(exerciseListener != null) {
                        exerciseListener.onTimeChanged(currentTotalElapsedMs,
                                accSleepMs, currentCaloriesBurnt, currentExercisePartModel);
                    }
                }

                exercising(index + 1);
            }
        });
    }

    private void startAsForeground(){
        startForeground(SERVICE_ID, getNotification(getString(R.string.app_name), "", -1, false));
    }

    private void updateNotification(float currentExercisePartElapsedMs,
                                                ExercisePartModel currentExercisePartModel){
        float remainingSecs = currentExercisePartModel.getDurationSecs() - (currentExercisePartElapsedMs / 1000);

        String notificationTime = CalculationHelper.prettifySeconds(remainingSecs);
        if(!notificationTime.equals(lastNotificationText)){
            lastNotificationText = notificationTime;
            if(Math.floor(remainingSecs) == 2){
                MediaHelper.playAnnouncementSoundAndVibrate(getBaseContext());
            }

            NotificationManager mNotificationManager = (NotificationManager)
                                        getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(SERVICE_ID, getNotification(
                    currentExercisePartModel.getExerciseText(this.getBaseContext()), notificationTime,
                    currentExercisePartModel.getExerciseIcon(), false));
        }
    }

    private void updateCaloriesBurnt(float deltaMs, ExercisePartModel currentExercisePartModel){
        currentCaloriesBurnt += currentExercisePartModel.getCaloriesBurnt(deltaMs, user.getBMIValue(),
                            user.getWeightKg(), user.getAge(), user.getGenderEnum());
    }

    private void speakOutCurrentExercisePart(ExercisePartModel currentExercisePartModel){
        getTextSpeak().speak(currentExercisePartModel.getExerciseSpeech(getBaseContext()));
    }

    public void exerciseCompleted(){
        getTextSpeak().speak(getString(R.string.speech_running_finished));
        stopped = true;
        completed = true;
        stopForeground(true);
        NotificationManager mNotificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(SERVICE_ID, getNotification(getString(R.string.notf_running_finished_title),
                                getString(R.string.notf_running_finished_content), -1, true));
        if(exerciseListener != null) {
            exerciseListener.onExerciseEnded(realElapsedMs, currentCaloriesBurnt, completed);
        }
    }

    public void exerciseGaveUp(){
        stopped = true;
        if(exerciseListener != null) {
            exerciseListener.onExerciseEnded(realElapsedMs, currentCaloriesBurnt, false);
        }
    }

    public void pauseExercise(){
        paused = true;
    }

    public void resumeExercise(){
        paused = false;
    }



    public void requestTriggerStateChangedOnce(){
        if(exerciseListener != null) {
            exerciseListener.onTimeChanged(currentTotalElapsedMs,
                    accSleepMs, currentCaloriesBurnt, currentExercisePartModel);

            if(stopped){
                exerciseListener.onExerciseEnded(currentTotalElapsedMs, currentCaloriesBurnt, completed);
            }
        }
    }

    public void goToNextExercisePart(){
        skippingExercisePart = true;
    }

    public void resetService(){
        stopForeground(true);
        NotificationManager mNotificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(SERVICE_ID);
        stopped = true;
    }

    public void disposeExercise(){
        resetService();
        stopSelf();
        exerciseListener = null;
        if(wakeLock != null && wakeLock.isHeld()){
            wakeLock.release();
        }
    }


    public TextSpeak getTextSpeak() {
        if(textSpeak == null){
            textSpeak = TextSpeak.getInstance(getApplicationContext());
        }
        return textSpeak;
    }

    private Notification getNotification(String title, String content, int icon, boolean autoCancel){
        Intent notificationIntent = new Intent(this, RunningActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        if(icon < 0){
            icon = R.drawable.ic_stat_runs;
        }

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(autoCancel)
                .setContentIntent(pendingIntent).build();

        return notification;
    }


    public void setExerciseListener(ExerciseListener exerciseListener) {
        this.exerciseListener = exerciseListener;
    }

    public boolean isPaused() {
        return paused;
    }

    public class LocalBinder extends Binder {
        public ExerciseService getServiceInstance(){
            return ExerciseService.this;
        }
    }

    public interface ExerciseListener{

        void onTimeChanged(float totalElapsedMs, float currentExerciseElapsedMs, float caloriesBurnt,
                                  ExercisePartModel currentExercisePartModel);

        void onExercisePartChanged(ExercisePartModel newExercisePartModel);

        void onExerciseEnded(float totalElapsedMs, float caloriesBurnt, boolean isCompleted);
    }
}
