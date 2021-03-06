package com.challenge.bennho.a30days.drawables;

import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;

import com.challenge.bennho.a30days.helpers.Logs;

/**
 * Created by sionglengho on 27/10/16.
 */
public class CustomAnimationDrawable extends AnimationDrawable {

    /** Handles the animation callback. */
    Handler mAnimationHandler;
    private AnimationListener animationListener;

    public CustomAnimationDrawable(){

    }

    public CustomAnimationDrawable(AnimationDrawable aniDrawable) {
        /* Add each frame to our animation drawable */
        for (int i = 0; i < aniDrawable.getNumberOfFrames(); i++) {
            this.addFrame(aniDrawable.getFrame(i), aniDrawable.getDuration(i));
        }
    }

    public void setAnimationListener(AnimationListener animationListener) {
        this.animationListener = animationListener;
    }

    @Override
    public void start() {
        super.start();
        /*
         * Call super.start() to call the base class start animation method.
         * Then add a handler to call onAnimationFinish() when the total
         * duration for the animation has passed
         */
        mAnimationHandler = new Handler();
        mAnimationHandler.postDelayed(new Runnable() {

            public void run() {
                onAnimationFinish(CustomAnimationDrawable.this);
            }
        }, getTotalDuration());

    }

    /**
     * Gets the total duration of all frames.
     *
     * @return The total duration.
     */
    public int getTotalDuration() {

        int iDuration = 0;

        for (int i = 0; i < this.getNumberOfFrames(); i++) {
            iDuration += this.getDuration(i);
        }

        iDuration += 30;

        return iDuration;
    }

    public void reset(){
        CustomAnimationDrawable.this.stop();
        CustomAnimationDrawable.this.selectDrawable(0);
        CustomAnimationDrawable.this.start();
    }

    /**
     * Called when the animation finishes.
     */
    public void onAnimationFinish(CustomAnimationDrawable animation){
        if(animationListener != null){
            animationListener.onComplete();
        }
    }

    public interface AnimationListener{
        void onComplete();
    }


}
