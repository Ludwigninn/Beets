package com.github.beetsbyninn.beets;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.widget.Toast;

import android.view.View;
import android.widget.Button;

import java.io.IOException;


/**
 *
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private BeetsService mBeetsService;
    private boolean mBound = false;
    private BeetsServiceConnection mServiceConnection;
    private GaugeFragment gaugeFragment;

    private MusicPlayer mMusicPlayer;
    private boolean mIsplaying = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mServiceConnection = new BeetsServiceConnection(this);
        bindService();

        mMusicPlayer = new MusicPlayer(this);
        mMusicPlayer.initSongMediaPlayer();
         gaugeFragment = new GaugeFragment();
        setFragment(gaugeFragment, false);
    }

    /**
     * Sets a reference to the service
     * @param beetsService
     *      A Reference to the service.
     */
    public void setService(BeetsService beetsService) {
        mBeetsService = beetsService;
    }

    /**
     * Sets a boolean flag if the service is bound.
     * @param bound
     */
    public void setBound(boolean bound) {
        mBound = bound;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mServiceConnection);
        }
    }

    /**
     * Tries to bind the service.
     */
    private void bindService() {
        mServiceConnection = new BeetsServiceConnection(this);
        Intent intent = new Intent(this, BeetsService.class);
        if (bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
            Log.d(TAG, "bindService: Service connectin Succsed");
        } else {
            Toast.makeText(mBeetsService, "Service connection failed", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "bindService: Service connection failed");




        }
    }



    /**
     *
     */
    public void initalizaise() {
        if(mIsplaying == false) {
            mBeetsService.startSong(120,System.currentTimeMillis());
            mMusicPlayer.playSong();
            mIsplaying = true;
        }else if(mIsplaying){
            mMusicPlayer.stopSong();
            mIsplaying = false;
        }
    }


    public void setFragment(Fragment fragment, boolean backstack) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container, fragment);
        if (backstack) {
            ft.addToBackStack(null);
        }
        ft.commit();
    }

    public void update(final double score){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                gaugeFragment.updateScore(score);
            }
        });


    }
}
