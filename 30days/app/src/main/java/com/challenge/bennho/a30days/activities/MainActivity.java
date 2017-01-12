package com.challenge.bennho.a30days.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.challenge.bennho.a30days.R;
import com.challenge.bennho.a30days.controls.LayoutDayCounter;
import com.challenge.bennho.a30days.helpers.AndroidUtils;
import com.challenge.bennho.a30days.helpers.CalculationHelper;
import com.challenge.bennho.a30days.helpers.OverlayBuilder;
import com.challenge.bennho.a30days.helpers.RunnableArgs;
import com.challenge.bennho.a30days.helpers.Strings;
import com.challenge.bennho.a30days.models.User;
import com.challenge.bennho.a30days.services.ExerciseService;

public class MainActivity extends MyActivity {

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private LayoutDayCounter dayCounterControl;
    private ImageView imgViewPrevious, imgViewNext;
    private int userMaxDay;
    private int currentSelectedDay;
    private RelativeLayout layoutExercise, layoutMeal;
    private TextView txtDayNumber1, txtDayNumber2, txtWeight, txtHeight, txtCalories;
    private RelativeLayout layoutLockExercise, layoutLockMeal;
    private boolean lockedExercise, lockedMeal;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        onLayoutSet();

        layoutExercise = (RelativeLayout) findViewById(R.id.layoutExercise);
        layoutMeal = (RelativeLayout) findViewById(R.id.layoutMeal);
        layoutLockExercise = (RelativeLayout) findViewById(R.id.layoutLockExercise);
        layoutLockMeal = (RelativeLayout) findViewById(R.id.layoutLockMeal);
        dayCounterControl = (LayoutDayCounter) findViewById(R.id.dayCounterControl);
        imgViewNext = (ImageView) findViewById(R.id.imgViewNext);
        imgViewPrevious = (ImageView) findViewById(R.id.imgViewPrevious);
        txtDayNumber1 = (TextView) findViewById(R.id.txtDayNumber1);
        txtDayNumber2 = (TextView) findViewById(R.id.txtDayNumber2);
        txtWeight = (TextView) findViewById(R.id.txtWeight);
        txtHeight = (TextView) findViewById(R.id.txtHeight);
        txtCalories = (TextView) findViewById(R.id.txtCalories);

        setListeners();

        refreshUserProgress();
        updateDay(userMaxDay);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //stop any orphaned ExerciseService
        Intent serviceIntent = new Intent(this, ExerciseService.class);
        stopService(serviceIntent);

        refreshUserProgress();
        checkProVersionLockedMeal();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("dayPlan", currentSelectedDay);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if(savedInstanceState != null && savedInstanceState.containsKey("dayPlan")){
            updateDay(savedInstanceState.getInt("dayPlan"));
        }
    }


    private void refreshUserProgress(){
        User user = new User(this);
        user.reload();
        userMaxDay = user.getCurrentDay();
        dayCounterControl.setMaxDayNumber(userMaxDay);

        txtCalories.setText(String.format(getString(R.string.x_kcal), String.valueOf(user.getTotalCaloriesBurnt())));
        if(user.getUnitIndex() == 0){
            txtWeight.setText(String.format(getString(R.string.x_kg),
                    String.valueOf(Strings.formatToXDec(0, user.getWeightKg()))));
            txtHeight.setText(String.format(getString(R.string.x_cm),
                    String.valueOf(Strings.formatToXDec(0, user.getHeightInCm()))));
        }
        else{
            txtWeight.setText(String.format(getString(R.string.x_pounds),
                    String.valueOf(Strings.formatToXDec(0, CalculationHelper.kgToPounds(user.getWeightKg())))));
            Pair<Double, Double> heightPair = CalculationHelper.heightCmToInch(user.getHeightInCm());
            txtHeight.setText(String.format(getString(R.string.x_foot_x_inch),
                    String.valueOf(Strings.formatToXDec(0, heightPair.first)),
                    String.valueOf(Strings.formatToXDec(0, heightPair.second))));
        }

    }

    private void updateDay(int day){
        currentSelectedDay = day;
        if(currentSelectedDay > 30){
            currentSelectedDay = 30;
        }

        dayCounterControl.updateDayNumber(day);
        txtDayNumber1.setText(String.format(getString(R.string.day_X), String.valueOf(day)));
        txtDayNumber2.setText(String.format(getString(R.string.day_X), String.valueOf(day)));

        setEnablePrevDayButton(day > 1);
        setEnableNextDayButton(day < 30);
        checkProVersionLockedMeal();
        //setLockExercise(currentSelectedDay > userMaxDay);
        setLockExercise(false);
    }


    private void startExercise(){
        if(lockedExercise){
            OverlayBuilder.build(this)
                    .setTitle(getString(R.string.running_locked_title))
                    .setContent(getString(R.string.running_locked_content))
                    .setOverlayType(OverlayBuilder.OverlayType.OkOnly)
                    .show();
        }
        else{
            Intent intent = new Intent(this, ReadyActivity.class);
            intent.putExtra("dayPlan", currentSelectedDay);
            startActivity(intent);
        }
    }

    private void showMeal(){
        if(lockedMeal){
            purchasePro();
        }
        else{
            Intent intent = new Intent(this, MealActivity.class);
            intent.putExtra("dayPlan", currentSelectedDay);
            startActivity(intent);
        }

    }

    private void checkProVersionLockedMeal(){
        if(currentSelectedDay > 7){
            getProVersionHelpers().isProPurchased(new RunnableArgs<Boolean>() {
                @Override
                public void run() {
                    setLockMeal(!this.getFirstArg());
                }
            });
        }
        else{
            setLockMeal(false);
        }
    }

    private void setListeners(){
        layoutExercise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startExercise();
            }
        });

        layoutMeal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMeal();
            }
        });
    }

    private void setEnableNextDayButton(boolean enabled){
        if(enabled){
            imgViewNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateDay(currentSelectedDay + 1);
                }
            });
            imgViewNext.setAlpha(1f);
        }
        else{
            imgViewNext.setOnClickListener(null);
            imgViewNext.setAlpha(0.3f);
        }
    }

    private void setEnablePrevDayButton(boolean enabled){
        if(enabled){
            imgViewPrevious.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateDay(currentSelectedDay - 1);
                }
            });
            imgViewPrevious.setAlpha(1f);
        }
        else{
            imgViewPrevious.setOnClickListener(null);
            imgViewPrevious.setAlpha(0.3f);
        }

    }

    private void setLockExercise(boolean locked){
        lockedExercise = locked;
        if(locked){
            layoutLockExercise.setVisibility(View.VISIBLE);
        }
        else{
            layoutLockExercise.setVisibility(View.GONE);
        }
    }

    private void setLockMeal(boolean locked){
        lockedMeal = locked;
        if(locked){
            layoutLockMeal.setVisibility(View.VISIBLE);
        }
        else{
            layoutLockMeal.setVisibility(View.GONE);
        }
    }




}
