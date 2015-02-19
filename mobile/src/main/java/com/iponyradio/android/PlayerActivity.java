package com.iponyradio.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


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
        PlayPause = (Button) findViewById(R.id.playpause);

        stationName.setText(station);
        streamName.setText(stream);
        streamURL.setText(url);
        PlayPause.setText("Play");

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        getPrefs();
        editor = prefs.edit();
        editor.putString("URL", url);
        editor.putString("STATION", station);
        editor.commit();
        streamService = new Intent(PlayerActivity.this, BackgroundService.class);

        PlayPause.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                if (PlayPause.getText().equals("Play")) {
                    startService(streamService);
                    PlayPause.setText("Stop");
                } else {
                    stopService(streamService);
                    PlayPause.setText("Play");
                }
            }
        });
    }

    public void getPrefs() {
        isPlaying = prefs.getBoolean("isPlaying", false);
        if (isPlaying) {
            PlayPause.setText("Stop");
        } else {
            PlayPause.setText("Play");
        }
    }

    @Override
    public void onDestroy() {
        if (isPlaying) {
            stopService(streamService);
        }
    }
}
