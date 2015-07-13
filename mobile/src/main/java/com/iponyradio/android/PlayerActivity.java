package com.iponyradio.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.iponyradio.android.drawables.PlayPauseView;
import com.iponyradio.android.recycler.FeedItem;
import com.iponyradio.android.recycler.MyRecyclerAdapter;
import com.millennialmedia.android.MMAdView;
import com.millennialmedia.android.MMRequest;
import com.millennialmedia.android.MMSDK;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class PlayerActivity extends Activity {

    static Context context;
    boolean isPlaying;
    Intent streamService;
    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    String station;
    String stream;
    String url;
    int id;
    String title;
    String artist;

    TextView stationName;
    TextView streamName;
    TextView streamURL;

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
        stationName = (TextView) findViewById(R.id.player_station_name);
        streamName = (TextView) findViewById(R.id.player_stream_name);
        streamURL = (TextView) findViewById(R.id.player_stream_url);
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

        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("ipr-update-meta"));
    }

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            artist = intent.getStringExtra("artist");
            title = intent.getStringExtra("title");
            streamName.setText(title);
            streamURL.setText(artist);
        }
    };

    public void getPrefs() {
        station = prefs.getString("CURRENT_STATION_NAME", null);
        url = prefs.getString("CURRENT_STREAM_URL", null);
        stream = prefs.getString("CURRENT_STREAM_NAME", null);
        isPlaying = prefs.getBoolean("isPlaying", false);
        id = prefs.getInt("CURRENT_STATION_ID", 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        if (isPlaying) {
            stopService(streamService);
        }
    }


}