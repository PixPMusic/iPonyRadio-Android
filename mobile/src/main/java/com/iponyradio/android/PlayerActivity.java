package com.iponyradio.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


public class PlayerActivity extends Activity {

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // getting intent data
        Intent in = getIntent();

        // Get JSON values from previous intent
        String station = in.getStringExtra("STATION_NAME");
        String stream = in.getStringExtra("STREAM_NAME");
        final String url = in.getStringExtra("STREAM_URL");

        // Displaying all values on the screen
        TextView stationName = (TextView) findViewById(R.id.player_station_name);
        TextView streamName = (TextView) findViewById(R.id.player_stream_name);
        TextView streamURL = (TextView) findViewById(R.id.player_stream_url);
        final Button PlayPause = (Button) findViewById(R.id.playpause);

        final Intent intent = new Intent(this, BackgroundService.class);
        // add infos for the service which file to download and where to store
        intent.putExtra("STREAM_URL", url);
        startService(intent);

        stationName.setText(station);
        streamName.setText(stream);
        streamURL.setText(url);
        PlayPause.setText("Play");

        PlayPause.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(!BackgroundService.checkPlaying()) {
                    intent.putExtra("PLAY_PAUSE", 1);
                    PlayPause.setText("Pause");
                } else {
                    intent.putExtra("PLAY_PAUSE", 0);
                    PlayPause.setText("Play");
                }
                startService(intent);
            }
        });
    }

}
