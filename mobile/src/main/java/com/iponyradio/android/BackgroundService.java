package com.iponyradio.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSession.Callback;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BackgroundService extends Service {
    private static final String TAG = "iPonyRadio-debug";
    MediaPlayer mp;
    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    Notification n;
    NotificationManager notificationManager;
    int notifId = 825;
    Context context;
    String title;
    String artist;
    String albumURL;
    Bitmap albumArt;
    private static final String ACTION_TOGGLE_PLAYBACK = "com.iponyradio.android.TOGGLE_PLAYBACK";
    MediaSession mediaSession;
    private Handler mHandler;

    Notification.Action action;

    String url;
    String station;
    String station_shortcode;
    int stream_id;
    Boolean isPlaying;

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        // Init the SharedPreferences and Editor
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        url = prefs.getString("CURRENT_STREAM_URL", "http://176.31.115.196:8214/");
        station = prefs.getString("CURRENT_STATION_NAME", "FOOBAR");
        station_shortcode = prefs.getString("CURRENT_STATION_SHORTCODE", null);
        stream_id = prefs.getInt("CURRENT_STREAM_ID", 0);
        editor = prefs.edit();

        context = getApplicationContext();
        title = context.getResources().getString(R.string.app_name);
        artist = context.getResources().getString(R.string.buffering);
        if (Build.VERSION.SDK_INT >= 21 ) {
            mediaSession = new MediaSession(this, TAG);
            notificationManager = (NotificationManager) getApplicationContext()
                    .getSystemService(NOTIFICATION_SERVICE);
            mediaSession.setActive(true);

            mediaSession.setCallback(new Callback() {
                @Override
                public void onPlay() {
                    super.onPlay();
                    Log.e(TAG, "onPlay");
                    NotificationMaker(title, artist);
                }

                @Override
                public void onPause() {
                    super.onPause();
                    Log.e(TAG, "onPause");
                    NotificationMaker(title, artist);
                }
            });
        } else {
            NotificationMaker(title, artist);
        }

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
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent i, int a, int b) {
        super.onStartCommand(i,a,b);
        Log.d(TAG, "onStart");
        mp.start();
        isPlaying = true;
        editor.putBoolean("isPlaying", true);
        editor.commit();
        String notifTitle = context.getResources().getString(R.string.app_name);
        String notifMessage = station + " " + context.getResources().getString(R.string.now_playing);
        NotificationMaker(notifTitle, notifMessage);
        mHandler = new Handler();
        startRepeatingTask();
        return a;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopRepeatingTask();
        mp.stop();
        mp.release();
        mp = null;
        isPlaying = false;
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
            int mInterval = 5000;
            mHandler.postDelayed(mStatusChecker, mInterval);
        }
    };

    public class AsyncHttpTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected void onPreExecute() { }

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
                Log.d(TAG, e.getLocalizedMessage());
            }
            return result; //"Failed to fetch data!";
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == 1) {
                NotificationMaker(title, artist);
                sendMessage();
            }
        }
    }

    private void NotificationMaker(String notifTitle, String notifMessage) {
        Context context = getApplicationContext();
        n = new Notification();
        if (Build.VERSION.SDK_INT >= 21 ) {
            // Update the current metadata
            mediaSession.setMetadata(new MediaMetadata.Builder()
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, station)
                    .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                    .build());


                // Indicate you want to receive transport controls via your Callback
                mediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

                // Implement Mediastyle Notifications
                // Create a new Notification
            Intent paction;
            paction = new Intent(ACTION_TOGGLE_PLAYBACK);
            PendingIntent pendingIntent;
            final ComponentName serviceName = new ComponentName(this, BackgroundService.class);
            paction.setComponent(serviceName);
            pendingIntent = PendingIntent.getService(this, 1, paction, 0);
            final Notification noti = new Notification.Builder(this)
                        // Hide the timestamp
                    .setShowWhen(false) // Set the Notification style
                    .setStyle(new Notification.MediaStyle() // Attach our MediaSession token
                            .setMediaSession(mediaSession.getSessionToken()) // Show our playback controls in the compat view
                            .setShowActionsInCompactView(0)) // Set the Notification color
                    .setColor(getResources().getColor(R.color.colorPrimaryDark)) // Set the large and small icons
                    .setLargeIcon(albumArt)
                    .setSmallIcon(R.drawable.ic_launcher) // Set Notification content information
                    .setContentText(artist)
                    .setContentInfo(station)
                    .setContentTitle(title) // Add some playback controls
                    .addAction(android.R.drawable.ic_media_play, "Play", pendingIntent)
                    .build();

            // Do something with your TransportControls
            final MediaController.TransportControls controls = mediaSession.getController().getTransportControls();

            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(notifId, noti);
        } else {
            // Classic Notifications
            n.icon = R.drawable.ic_launcher;
            n.tickerText = notifMessage;
            n.flags = Notification.FLAG_NO_CLEAR;
            n.when = System.currentTimeMillis();
            Intent nIntent = new Intent(context, PlayerActivity.class);
            PendingIntent pIntent = PendingIntent.getActivity(context, 0, nIntent, 0);
            n.setLatestEventInfo(context, notifTitle, notifMessage, pIntent);
            notificationManager.notify(notifId, n);
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
            JSONObject stationObject = resultObject.getJSONObject("station");
            String stationLogo = stationObject.optString("image_url");
            JSONArray streamsArray = resultObject.getJSONArray("streams");
            JSONObject streamObject = streamsArray.getJSONObject(stream_id);
            JSONObject stream = streamObject.getJSONObject("current_song");
            title = stream.optString("title");
            artist = stream.optString("artist");
            String songArt = stream.getString("image_url");
            String albumURLtemp;
            if (songArt.equals("")) {
                albumURLtemp = stationLogo;
            } else {
                albumURLtemp = songArt;
            }
            if (!albumURLtemp.equals(albumURL)) {
                albumURL = albumURLtemp;
                albumArt = drawableFromUrl(albumURL);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Bitmap drawableFromUrl(String url) throws IOException {
        Bitmap x;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.connect();
        InputStream input = connection.getInputStream();
        x = BitmapFactory.decodeStream(input);
        return x;
    }
}