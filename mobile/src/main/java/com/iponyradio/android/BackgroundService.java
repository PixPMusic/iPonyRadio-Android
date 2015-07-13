package com.iponyradio.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BackgroundService extends Service {
    private static final String TAG = "StreamService";
    MediaPlayer mp;
    boolean isPlaying;
    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    Notification n;
    NotificationManager notificationManager;
    String url;
    String station;
    int notifId = 825;

    String title;
    String artist;
    String station_shortcode;
    private int mInterval = 5000; // 5 seconds by default, can be changed later
    private Handler mHandler;

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        // Init the SharedPreferences and Editor
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        url = prefs.getString("CURRENT_STREAM_URL", "http://176.31.115.196:8214/");
        station = prefs.getString("CURRENT_STATION_NAME", "FOOBAR");
        station_shortcode = prefs.getString("CURRENT_STATION_SHORTCODE", null);
        editor = prefs.edit();

        // Set up the buffering notification
        notificationManager = (NotificationManager) getApplicationContext()
                .getSystemService(NOTIFICATION_SERVICE);
        Context context = getApplicationContext();

        String notifTitle = context.getResources().getString(R.string.app_name);
        String notifMessage = context.getResources().getString(R.string.buffering);

        n = new Notification();
        n.icon = R.drawable.ic_launcher;
        n.tickerText = "Buffering";
        n.when = System.currentTimeMillis();

        Intent nIntent = new Intent(context, PlayerActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, nIntent, 0);

        n.setLatestEventInfo(context, notifTitle, notifMessage, pIntent);

        notificationManager.notify(notifId, n);

        // It's very important that you put the IP/URL of your ShoutCast stream here
        // Otherwise you'll get Webcom Radio
        mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mp.setDataSource(url);
            mp.prepare();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "SecurityException");
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "IllegalStateException");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "IOException");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(TAG, "onStart");
        mp.start();
        // Set the isPlaying preference to true
        editor.putBoolean("isPlaying", true);
        editor.commit();

        Context context = getApplicationContext();
        String notifTitle = context.getResources().getString(R.string.app_name);
        String notifMessage = station + " " + context.getResources().getString(R.string.now_playing);

        n.icon = R.drawable.ic_launcher;
        n.tickerText = notifMessage;
        n.flags = Notification.FLAG_NO_CLEAR;
        n.when = System.currentTimeMillis();

        mHandler = new Handler();
        startRepeatingTask();

        Intent nIntent = new Intent(context, PlayerActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, nIntent, 0);

        n.setLatestEventInfo(context, notifTitle, notifMessage, pIntent);
        notificationManager.notify(notifId, n);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopRepeatingTask();
        mp.stop();
        mp.release();
        mp = null;
        editor.putBoolean("isPlaying", false);
        editor.commit();
        notificationManager.cancel(notifId);
    }

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            new AsyncHttpTask().execute();
            mHandler.postDelayed(mStatusChecker, mInterval);
        }
    };

    public class AsyncHttpTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Integer doInBackground(String... params) {
            Integer result = 0;
            HttpURLConnection urlConnection;
            try {
                URL url = new URL("http://ponyvillelive.com/api/nowplaying/index/station/" + station_shortcode);
                urlConnection = (HttpURLConnection) url.openConnection();
                int statusCode = urlConnection.getResponseCode();

                // 200 represents HTTP OK
                if (statusCode == 200) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        response.append(line);
                    }
                    parseResult(response.toString());
                    result = 1; // Successful
                } else {
                    result = 0; //"Failed to fetch data!";
                }
            } catch (Exception e) {
                Log.d("iPonyRadioLog", e.getLocalizedMessage());
            }
            return result; //"Failed to fetch data!";
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == 1) {
                Context context = getApplicationContext();
                String notifTitle = title;
                String notifMessage = artist;
                n.icon = R.drawable.ic_launcher;
                n.tickerText = notifMessage;
                n.flags = Notification.FLAG_NO_CLEAR;
                n.when = System.currentTimeMillis();
                Intent nIntent = new Intent(context, PlayerActivity.class);
                PendingIntent pIntent = PendingIntent.getActivity(context, 0, nIntent, 0);
                n.setLatestEventInfo(context, notifTitle, notifMessage, pIntent);
                notificationManager.notify(notifId, n);
                sendMessage();
            }
        }
    }

    // Send an Intent with an action named "custom-event-name". The Intent sent should
// be received by the ReceiverActivity.
    private void sendMessage() {
        Intent intent = new Intent("ipr-update-meta");
        // You can also include some extra data.
        intent.putExtra("title", title);
        intent.putExtra("artist", artist);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void parseResult(String result) {
        try {
            JSONObject json = new JSONObject(result);
            JSONObject resultObject = json.getJSONObject("result");
            JSONArray streamsArray = resultObject.getJSONArray("streams");
            JSONObject streamObject = streamsArray.getJSONObject(0);
            JSONObject stream = streamObject.getJSONObject("current_song");
            title = stream.optString("title");
            artist = stream.optString("artist");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}