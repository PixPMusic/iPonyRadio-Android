package com.iponyradio.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.iponyradio.android.drawables.PlayPauseView;
import com.millennialmedia.android.MMAdView;
import com.millennialmedia.android.MMRequest;
import com.millennialmedia.android.MMSDK;

public class PlayerActivity extends Activity {

    Button PlayPause;
    static Context context;
    boolean isPlaying;
    Intent streamService;
    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    String station;
    String stream;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        context = this;

        MMSDK.initialize(this);

        //Find the ad view for reference
        MMAdView adViewFromXml = (MMAdView) findViewById(R.id.adView3);

        //MMRequest object
        MMRequest request = new MMRequest();

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        getPrefs();

        // Displaying all values on the screen
        TextView stationName = (TextView) findViewById(R.id.player_station_name);
        TextView streamName = (TextView) findViewById(R.id.player_stream_name);
        TextView streamURL = (TextView) findViewById(R.id.player_stream_url);
        final PlayPauseView view = (PlayPauseView) findViewById(R.id.play_pause_view);

        stationName.setText(station);
        streamName.setText(stream);
        streamURL.setText(url);
        isPlaying = false;
        if (!isPlaying) view.toggle();

        streamService = new Intent(PlayerActivity.this, BackgroundService.class);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlaying) {
                    startService(streamService);
                } else {
                    stopService(streamService);
                }
                isPlaying = !isPlaying;
                view.toggle();
            }
        });
    }

    public void getPrefs() {
        station = prefs.getString("CURRENT_STATION_NAME", null);
        url = prefs.getString("CURRENT_STREAM_URL", null);
        stream = prefs.getString("CURRENT_STREAM_NAME", null);
        isPlaying = prefs.getBoolean("isPlaying", false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isPlaying) {
            stopService(streamService);
        }
    }
}