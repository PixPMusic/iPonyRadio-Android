package com.iponyradio.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class PlayerActivity extends AppCompatActivity {

    private static String ALBUM_ART_URL;
    private static Bitmap ALBUM_ART;
    private static CollapsingToolbarLayout collapsingToolbar;
    static Context context;
    boolean isPlaying;
    boolean isPaused;
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
    ImageView albumArt;

    private static PlayPauseView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        context = this;

        // Set a ToolBar to replace the ActionBar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        /*<com.millennialmedia.android.MMAdView
        android:id="@+id/adView3"
        android:layout_width="320dp"
        android:layout_height="50dp"
        android:layout_above="@id/cardview"
        android:layout_centerHorizontal="true"
        mm:width="320"
        mm:height="50"
        mm:apid="193467" />

                MMSDK.initialize(this);

        //Find the ad view for reference
        MMAdView adViewFromXml = (MMAdView) findViewById(R.id.adView3);

        //MMRequest object
        MMRequest request = new MMRequest();

        adViewFromXml.setMMRequest(request);

        adViewFromXml.getAd();
        */

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        getPrefs();

        // Displaying all values on the screen
        streamName = (TextView) findViewById(R.id.player_stream_name);
        streamURL = (TextView) findViewById(R.id.player_stream_url);
        view = (PlayPauseView) findViewById(R.id.play_pause_view);
        albumArt = (ImageView) findViewById(R.id.albumart);

        collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(station + " - iPR");

        streamName.setText(stream);
        streamName.setSelected(true);
        streamURL.setText(url);
        streamURL.setSelected(true);
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_launcher);
        ALBUM_ART = icon;
        isPlaying = false;
        if (!isPlaying) view.toggle();

        streamService = new Intent(PlayerActivity.this, MediaService.class);

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

    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            boolean isPlayingTemp = intent.getBooleanExtra("isPlaying", true);
            if (!isPlayingTemp == isPlaying) {
                view.toggle();
            }
            isPlaying = isPlayingTemp;
            isPaused = intent.getBooleanExtra("isPaused", false);
            String artistTemp = intent.getStringExtra("artist");
            if (!artistTemp.equals(artist)) {
                artist = artistTemp;
                streamURL.setText(artist);
            }
            String titleTemp = intent.getStringExtra("title");
            if (!titleTemp.equals(title)) {
                title = titleTemp;
                streamName.setText(title);
            }
            String albumURLtemp = intent.getStringExtra("album_art");
            if (albumURLtemp != null && !albumURLtemp.equals("")) {
                if (!albumURLtemp.equals(ALBUM_ART_URL)) {
                    ALBUM_ART_URL = albumURLtemp;
                    new refreshView().execute();
                }
            }
            if (ALBUM_ART != null) {
                updateAlbumArt();
            }
        }
    };

    private void updateAlbumArt() {
        albumArt.setImageBitmap(ALBUM_ART);
        Palette p = Palette.from(ALBUM_ART).generate();
        int color = p.getDarkVibrantColor(0xF44336);
        Log.d("ASDF", String.valueOf(color));
        view.updateBackgroundColor(color);
        collapsingToolbar.setBackgroundColor(color);
    }

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
        ALBUM_ART_URL = null;
        ALBUM_ART = null;
        if (isPlaying) {
            stopService(streamService);
        }
    }

    class refreshView extends AsyncTask {

        Bitmap drawableFromUrl(String url) throws IOException {
            Bitmap x;
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.connect();
            InputStream input = connection.getInputStream();
            x = BitmapFactory.decodeStream(input);
            return x;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            try {
                ALBUM_ART = drawableFromUrl(ALBUM_ART_URL);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}