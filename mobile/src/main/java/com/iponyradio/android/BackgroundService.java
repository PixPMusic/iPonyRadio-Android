package com.iponyradio.android;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;

import java.io.IOException;

/**
 * Created by Pixel on 2/9/2015.
 */
public class BackgroundService extends IntentService implements MediaPlayer.OnPreparedListener {

    // Fields
    // Stream Related
    private static String currentStreamUrl;
    private static boolean isPlaying;

    // Notification Stuff
    private static String streamTitle;
    private static String artist;
    private static String song;
    private static String coverURL;
    private static String notificationTitle;
    private static String notificationBody;

    // More MediaPlayer and WifiLock stuff
    private MediaPlayer m;
    private WifiManager.WifiLock wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
            .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
    private Boolean wifiLocked;

    public BackgroundService() {
        super("BackgroundService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        String stream_url = intent.getStringExtra("STREAM_URL");
        if (!stream_url.equals("null")) {
            try {
                setCurrentStreamUrl(stream_url);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int playpause = intent.getIntExtra("PLAY_PAUSE", 0);
        switch (playpause) {
            case 0:
                break;
            case 1:
                try {
                    play();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            case 2:
                stop();
            default:
                break;
        }
    }


    // Accessor Methods

    /**
     * @return Playining Variable as Boolean
     */
    public static boolean checkPlaying() {
        return isPlaying;
    }

    /**
     * Change the Stream URL and restart the stream if it's running
     * @param url
     */
    public void setCurrentStreamUrl(String url) throws IOException {
        currentStreamUrl = url;
        restartWithNewURL();
    }

    /**
     * @return the current Stream URL
     */
    public String checkURL() {
        return currentStreamUrl;
    }

    /**
     * Enables Wifi Lock and sets
     * wifiLocked boolean true
     */
    public void lockWifi() {
        if (!checkWifiLock()) {
            wifiLock.acquire();
            wifiLocked = true;
        }
    }

    /**
     * Disables Wifi Lock and
     * Sets wifiLocked boolean false
     */
    public void unlockWifi() {
        if (checkWifiLock()) {
            wifiLock.release();
            wifiLocked = false;
        }
    }

    /**
     * returns the value of boolean wifiLocked
     */
    public boolean checkWifiLock() {
        return wifiLocked;
    }

    /**
     * Toggles MainActivity/Pause state
     */
    public void togglePlayPause() throws IOException {
        if (checkPlaying()) {
            pause();
        } else {
            play();
        }
    }

    /**
     * Stops the Media Player
     */
    public void stopPlaying() {
        if (checkPlaying()) {
            stop();
        }
    }

    /**
     * Check player state and kill the mediaplayer
     */
    public void killMedia() {
        if (isPlaying) {
            stop();
        }
        destroy();
    }


    // Private Methods

    /**
     * Sets Playing variable to true
     */
    private void setPlaying() {
        isPlaying = true;
    }

    /**
     * Sets Playing variable to False
     */
    private void setStopped() {
        isPlaying = false;
    }

    /**
     * Restarts the Stream
     * (Called by <code>setCurrentStreamURL()</code>
     */
    private void restartWithNewURL() throws IOException {
        if (checkPlaying()) {
            stop();
        }
        play();
    }

    public void play() throws IOException {
        if (!checkPlaying()) {
            setPlaying();
        } else {
            stop();
        }
        m.setAudioStreamType(AudioManager.STREAM_MUSIC);
        m.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        lockWifi();
        m.setDataSource(currentStreamUrl);
        m.setOnPreparedListener(this);
        m.prepareAsync();
    }

    private void stop() {
        if (checkPlaying()) {
            m.stop();
            wifiLock.release();
            destroy();
        }
    }

    private void pause() {
        if (checkPlaying()) {
            m.pause();
        }
    }

    private void destroy() {
        if (checkPlaying()) {
            stop();
        }
        m.release();
        m = null;
        unlockWifi();
    }


    // Public Methods

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
    }
}
