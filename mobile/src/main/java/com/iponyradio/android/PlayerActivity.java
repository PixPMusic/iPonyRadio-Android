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

        // getting intent data
        Intent in = getIntent();

        // Get JSON values from previous intent
        String station = in.getStringExtra("STATION_NAME");
        String stream = in.getStringExtra("STREAM_NAME");
        String url = in.getStringExtra("STREAM_URL");

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

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        getPrefs();
        editor = prefs.edit();
        editor.putString("URL", url);
        editor.putString("STATION", station);
        editor.commit();
        streamService = new Intent(PlayerActivity.this, BackgroundService.class);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.toggle();
                if (!isPlaying) {
                    startService(streamService);
                } else {
                    stopService(streamService);
                }
                isPlaying = !isPlaying;
            }
        });
    }

    public void getPrefs() {
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
