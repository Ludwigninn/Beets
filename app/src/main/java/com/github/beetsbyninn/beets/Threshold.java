package com.github.beetsbyninn.beets;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * author Patrik Larsson, Ludwig Ninn, Alexander Johansson.
 */
public class Threshold {
    private static final String TAG = "Threshold";

    private boolean startCheck;
    private int mCurrentMaxScore = 0;
    private double mCurrentScore = 0;
    private long mStartTime;
    private int mBPM;
    private FeedbackListener mFeedBackListener;
    private Timer timer;
    private Timer mStepTimer;
    private StepBuffer buffer;
    // private Worker worker;
    private MainActivity mListener;
    private Context mContext;
    private double[] perodicArray;

    private long mLastStep;
    private long mCurrentStep;
    private Vibrate mVibrator;
    private double mBarValue = 50;
    private int mExpoCount;
    public static final double VIBRATE_FIRST_FAIL = 40;
    public static final double VIBRATE_SECOND_FAIL = 30;
    public static final double VIBRATE_THIRD_FAIL = 20;
    public static final double VIBRATE_FOURTH_FAIL = 10;
    public static final double VIBRATE_FIFTH_FAIL = 0;

    private boolean mWarmup = false;
    /**
     * Constant used for measuring time when step should count as perfect.
     */
    private final double M_PERFECT;

    /**
     * Constant used for measuring time when step should count as good
     */
    private final double M_GOOD;
    private double intervalLength;


    /**
     * The constructor sets up the threshold with two constants. The constants are used for calculating
     * score.
     * Author Alexander & Patrik
     *
     * @param perfect           Maximum time for a step should be counting as perfect.
     * @param good              Maximum time for a step should be counting as perfect.
     * @param mBPM
     * @param mFeedBackListener A Class implemeting mFeedBackListener.
     */
    public Threshold(double perfect, double good, int mBPM, FeedbackListener mFeedBackListener) {
        this.mBPM = mBPM;
        this.mFeedBackListener = mFeedBackListener;
        M_PERFECT = perfect;
        M_GOOD = good;

        buffer = new StepBuffer();
    }

    /**
     * Constructor is only used for testing.
     * Author Alexander & Patrik
     *
     * @param perfect
     * @param good
     * @param bpm
     */
    public Threshold(double perfect, double good, int bpm, int songLength, MainActivity listener, Context context) {
        mBPM = bpm;
//        this.mFeedBackListener = mFeedBackListener;
        M_PERFECT = perfect;
        M_GOOD = good;
        buffer = new StepBuffer();
        mListener = listener;
        mFeedBackListener = new FeedbackListener() {

            @Override
            public void post10Sec(double score) {
                Log.d(TAG, "post10Sec: " + score);
            }
        };
        mListener.setThreshold(this);

        mContext = context;
        mVibrator = new Vibrate(mContext);
        mListener.setVibrator(mVibrator);
        perodicArray = getRhythmPeriodicy(mBPM, songLength);
        Log.d(TAG, "Threshold: Array" + Arrays.toString(perodicArray));
    }

    /**
     * DUNNO?
     * Author Alexander & Patrik
     *
     * @param beatsPerMinute
     * @param songLength
     * @return
     */
    public double[] getRhythmPeriodicy(double beatsPerMinute, int songLength) {
        intervalLength = 60.0 / beatsPerMinute;
        int totalBeatsInSong = (int) Math.ceil(songLength / intervalLength);
        double[] periodicyArray = new double[totalBeatsInSong];
        double currentPeriodicy = 0.0;
        for (int i = 0; i < totalBeatsInSong; i++) {
            periodicyArray[i] = currentPeriodicy;
            currentPeriodicy += intervalLength;
        }

        Log.d(TAG, "getRhythmPeriodicy:" + -(intervalLength - M_PERFECT));
        Log.d(TAG, "getRhythmPeriodicy:" + -(intervalLength - M_GOOD));

        return periodicyArray;
    }

    /**
     * Starts the socre counting
     * Author Alexander, Patrik & Ludwig
     *
     * @param startTime A long with a timestamp.
     */
    public void startThreshold(long startTime) {
        timer = new Timer();
        mStartTime = startTime;
        timer.schedule(new FeedBackTimer(), 0, 10000);
        timer.schedule(new DecresePoints(), 0, 5000);
        Log.d(TAG, "startThreshold: ");
//        worker = new Worker();
//        worker.start();
        mStepTimer = new Timer();
        mStepTimer.schedule(new StepTimer(), 0, (long) (intervalLength * 1000));
        toggleWarmup();
    }

    /**
     * @param stepTimeStamp§ Author Alexander & Patrik
     */
    public void postTimeStamp(long stepTimeStamp) {
        Log.d(TAG, "postTimeStamp: running ");
        try {
            buffer.add(stepTimeStamp);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Decrese points if a second has passed if the score is less then the set threshold a vibration will start. The vibration is stronger depending on how low the score.
     * Author Ludwig Ninn
     */
    private class DecresePoints extends TimerTask {

        /**
         * The action to be performed by this timer task.
         */
        @Override
        public void run() {
            Log.d(TAG, "DecresePoints " + mBarValue);

            if (!mWarmup) {
                long difference = Math.abs(mCurrentStep - mLastStep);
                if (1000 >= difference) {
                    mExpoCount++;
                    if (!startCheck) {
                        mBarValue -= 1.25 * mExpoCount;
                    }

                    if (mBarValue < VIBRATE_FIRST_FAIL && mBarValue > VIBRATE_SECOND_FAIL) {
                        mVibrator.vibrate(Vibrate.VIBRATION_FIRST);
                        Log.d(TAG, "Value1: " + mBarValue);
                    } else if (mBarValue < VIBRATE_SECOND_FAIL && mBarValue > VIBRATE_THIRD_FAIL) {
                        Log.d(TAG, "Value2: " + mBarValue);
                        mVibrator.vibrate(Vibrate.VIBRATION_SECOND);
                    } else if (mBarValue < VIBRATE_THIRD_FAIL && mBarValue > VIBRATE_FOURTH_FAIL) {
                        Log.d(TAG, "Value3: " + mBarValue);
                        mVibrator.vibrate(Vibrate.VIBRATION_THIRD);
                    } else if (mBarValue < VIBRATE_FOURTH_FAIL && mBarValue > VIBRATE_FIFTH_FAIL) {
                        Log.d(TAG, "Value4: " + mBarValue);
                        mVibrator.vibrate(Vibrate.VIBRATION_FOURTH);
                    } else if (mBarValue < VIBRATE_FIFTH_FAIL) {
                        Log.d(TAG, "Value5: " + mBarValue);
                        mVibrator.vibrate(Vibrate.VIBRATE_END);
                    }
                } else {
                    mExpoCount = 0;
                }
            } else {
                toggleWarmup(); // Warmup should be set to false after first time
            }
            startCheck = false;
            mLastStep = mCurrentStep;
        }
    }

    /**
     * Update score. If the score is above 100 or below 0 the score is updated to a fixed amount 100 or 0.
     * Author Ludwig Ninn
     */
    private class FeedBackTimer extends TimerTask {

        @Override
        public void run() {
            mListener.update(mCurrentScore);
            double temp = mBarValue + mCurrentScore;
            if (100 >= temp && 0 <= temp) {
                mBarValue += mCurrentScore;
                mListener.update(mBarValue);
                Log.d(TAG, "barvalue inc: " + mBarValue);
            } else {
                if (temp >= 100) {
                    mBarValue = 100;
                    mListener.update(mBarValue);
                    Log.d(TAG, "barvalue 100: " + mBarValue);
                } else if (temp <= 0) {
                    mBarValue = 0;
                    mListener.update(mBarValue);
                    Log.d(TAG, "barvalue 0: " + mBarValue);
                }
            }

            mCurrentScore = 0;

        }
    }

    /**
     * Calculates if the step was on the correct intervall as the beat
     * Author Alexander & Patrik
     */
    private class StepTimer extends TimerTask {

        @Override
        public void run() {
            try {
                mCurrentStep = buffer.remove();
                double currentTimeInSong = (mCurrentStep - mStartTime) / 1000.0;
                //int currentBeat = getBeatInSong(currentTimeInSong);
                //double differenceNext = (perodicArray[currentBeat + 1] % intervalLength) * 1000.0;
                double difference = (currentTimeInSong % intervalLength) * 1000.0;
//                Log.i(TAG, "timeStamp" + timeStamp);
//                Log.i(TAG, "currentTimeInSong: " + currentTimeInSong);
//                Log.i(TAG, "diff " + difference);
//                Log.i(TAG, "nextdiff: " + differenceNext);
                Log.i(TAG, "run: " + difference);
                if (!mWarmup) {
                    if (difference < M_PERFECT || (intervalLength * 1000) - difference <= M_PERFECT) {
                        mCurrentScore += 1;
                        Log.e(TAG, "PERFECT");
                    } else if (difference < M_GOOD || (intervalLength * 1000) - difference <= M_GOOD) {
                        mCurrentScore += 0.75;
                        Log.e(TAG, "GOOD");
                    } else {
                        Log.e(TAG, "FAIL");
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Calculates the current interval length.
     * Author Alexander & Patrik
     *
     * @param stepTime
     * @return
     */
    private int getBeatInSong(double stepTime) {
        int i = (int) (stepTime / intervalLength);
        Log.d(TAG, "getBeatInSong: " + i);
        return i;
    }

    /**
     * Calaculates the time that was paused and start the timers again.
     * Author Ludwig Ninn
     *
     * @param timePause
     */
    public void start(long timePause) {
        long dif = Math.abs(System.currentTimeMillis() - timePause);
        startThreshold(mStartTime + dif);
        Log.d(TAG, "start: " + mBarValue);
    }

    /**
     * Cancels the timer. Used in the pause function.
     */
    public void pause() {
        startCheck = true;
        Log.d(TAG, "pause: " + mBarValue);
        timer.cancel();
    }

    /**
     * Toggles the warmup flag.
     */
    public void toggleWarmup() {
        mWarmup = !mWarmup;
    }
}
