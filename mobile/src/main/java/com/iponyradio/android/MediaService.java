package com.iponyradio.android;

import android.annotation.TargetApi;
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
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.graphics.Palette;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MediaService extends Service{

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_STOP = "action_stop";
    public static final String ACTION_MEDIA_BUTTONS = "action_media_buttons";

    private static SharedPreferences mSharedPreferences;
    private static SharedPreferences.Editor mEditor;

    private static String STREAM_URL;
    private static int STREAM_ID = 0;

    private static String ARTIST = "Buffering";
    private static String TITLE = "iPonyRadio";
    private static String STATION = "iPonyRadio";
    private static int NOTIF_COLOR;
    private static int NOTIF_TEXT_COLOR;

    private static String ALBUM_ART_URL;
    private static String STATION_LOGO_URL;
    private static String STATION_SHORTCODE;
    private static Bitmap ALBUM_ART;

    private static final int NOTIFICATION_ID = 825;

    private static MediaPlayer mMediaPLayer;
    private static MediaSessionManager mManager;
    private static MediaSessionCompat mSession;
    private static MediaControllerCompat mController;
    private static Handler mHandler;

    private static boolean isPlaying = false;
    private static boolean isPaused = false;

    private static final String LOG_KEY = "iPonyRadio-Debug";

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        ALBUM_ART_URL = null;
        NOTIF_COLOR = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary);
        NOTIF_TEXT_COLOR = ContextCompat.getColor(getApplicationContext(), R.color.white);
    }

    private void handleIntent(Intent intent) {
        if(intent == null || intent.getAction() == null)
            return;

        String action = intent.getAction();
        if (Build.VERSION.SDK_INT >= 21 ) {
            if (action.equalsIgnoreCase(ACTION_PLAY)) {
                mController.getTransportControls().play();
            } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
                mController.getTransportControls().pause();
            } else if (action.equalsIgnoreCase(ACTION_STOP)) {
                mController.getTransportControls().stop();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private Notification.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), MediaService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new Notification.Action.Builder(icon, title, pendingIntent).build();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void buildNotification(Notification.Action action) {
        Notification.MediaStyle style = new Notification.MediaStyle();

        mSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, ALBUM_ART)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, ARTIST)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, STATION)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, TITLE)
                .build());

        Intent intent = new Intent(getApplicationContext(), MediaService.class);
        intent.setAction(ACTION_STOP);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .setContentTitle(TITLE)
                .setContentInfo(STATION)
                .setContentText(ARTIST)
                .setLargeIcon(ALBUM_ART)
                .setDeleteIntent(pendingIntent)
                .setColor(NOTIF_COLOR)
                .setStyle(style);

        builder.addAction(generateAction(android.R.drawable.ic_delete, "Stop", ACTION_STOP));
        builder.addAction(action);
        style.setShowActionsInCompactView(0, 1);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void buildNotification() {
        if (Build.VERSION.SDK_INT >= 21) {
            if (isPlaying) {
                buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
            } else {
                buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
            }
        } else {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(android.R.drawable.ic_media_play)
                            .setOngoing(true)
                            .setColor(NOTIF_COLOR)
                            .setContentTitle(TITLE)
                            .setContentText(ARTIST);
            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(getApplicationContext(), PlayerActivity.class);

            PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, 0);
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getPrefs();
        ALBUM_ART_URL = null;
        if (!isPlaying) {
            if (!isPaused) {
                if (mManager == null) {
                    try {
                        initMediaSessions();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                isPaused = false;
                isPlaying = true;
                startRepeatingTask();
                mMediaPLayer.start();
                if (Build.VERSION.SDK_INT >= 21) {
                    buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                } else {
                    buildNotification();
                }
            }
        }
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void getPrefs() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        STREAM_URL = mSharedPreferences.getString("CURRENT_STREAM_URL", "http://176.31.115.196:8214/");
        STATION = mSharedPreferences.getString("CURRENT_STATION_NAME", "FOOBAR");
        STATION_SHORTCODE = mSharedPreferences.getString("CURRENT_STATION_SHORTCODE", null);
        STREAM_ID = mSharedPreferences.getInt("CURRENT_STREAM_ID", 0);
    }

    private void initMediaSessions() throws RemoteException, IOException {
        mMediaPLayer = new MediaPlayer();
        mMediaPLayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPLayer.setDataSource(STREAM_URL);
        mMediaPLayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                isPlaying = true;
                mp.start();
            }
        });
        mMediaPLayer.prepareAsync();


        sendMessage();
        startRepeatingTask();

        // Start a new MediaSession
        ComponentName eventReceiver = new ComponentName(getPackageName(), "MediaService");
        PendingIntent buttonReceiverIntent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(ACTION_MEDIA_BUTTONS),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mSession = new MediaSessionCompat(this, "MediaService", eventReceiver, buttonReceiverIntent);
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mController = new MediaControllerCompat(getApplicationContext(), mSession.getSessionToken());

        mSession.setCallback(new MediaSessionCompat.Callback() {

            @Override
            public void onPlay() {
                //super.onPlay();
                Log.d(LOG_KEY, "onPlay");
                Log.d(LOG_KEY, "You clicked the Play button");
                if (!isPlaying) {
                    isPaused = false;
                    isPlaying = true;
                    sendMessage();
                    startRepeatingTask();
                    mMediaPLayer.start();
                    if (Build.VERSION.SDK_INT >= 21) {
                        buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                    } else {
                        buildNotification();
                    }
                }
            }

            @Override
            public void onPause() {
                //super.onPause();
                Log.d(LOG_KEY, "onPause");
                if (isPlaying) {
                    isPaused = true;
                    isPlaying = false;
                    sendMessage();
                    stopRepeatingTask();
                    mMediaPLayer.pause();
                    if (Build.VERSION.SDK_INT >= 21) {
                        buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
                    } else {
                        buildNotification();
                    }
                }
            }

            @Override
            public void onStop() {
                super.onStop();
                Log.d(LOG_KEY, "You clicked the Stop button");
                isPaused = false;
                isPlaying = false;
                Log.d(LOG_KEY, "onStop");
                sendMessage();
                NotificationManager notificationManager = (NotificationManager) getApplicationContext()
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(NOTIFICATION_ID);
                Intent intent = new Intent(getApplicationContext(), MediaService.class);
                stopService(intent);
            }
        });
        mManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mSession.release();
        isPlaying = false;
        stopRepeatingTask();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_KEY, "onDestroy");
        stopRepeatingTask();
        mMediaPLayer.stop();
        mMediaPLayer.release();
        mMediaPLayer = null;
        isPlaying = false;
        isPaused = false;
        mManager = null;
        sendMessage();
        NotificationManager n = (NotificationManager) getApplicationContext()
                .getSystemService(NOTIFICATION_SERVICE);
        n.cancel(NOTIFICATION_ID);
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
                URL url = new URL("http://ponyvillelive.com/api/nowplaying/index/station/" + STATION_SHORTCODE);
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
                Log.d(LOG_KEY, e.getLocalizedMessage());
            }
            return result; //"Failed to fetch data!";
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == 1) {
                buildNotification();
                sendMessage();
            }
        }
    }

    // Send an Intent with an action named "custom-event-name". The Intent sent should
    // be received by the ReceiverActivity.
    private void sendMessage() {
        Intent intent = new Intent("ipr-update-meta");
        // You can also include some extra data.
        intent.putExtra("isPlaying", isPlaying);
        intent.putExtra("isPaused", isPaused);
        intent.putExtra("title", TITLE);
        intent.putExtra("artist", ARTIST);
        intent.putExtra("album_art", ALBUM_ART_URL);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void parseResult(String result) {
        try {
            JSONObject json = new JSONObject(result);
            JSONObject resultObject = json.getJSONObject("result");
            JSONObject stationObject = resultObject.getJSONObject("station");
            STATION_LOGO_URL = stationObject.optString("image_url");
            JSONArray streamsArray = resultObject.getJSONArray("streams");
            JSONObject streamObject = streamsArray.getJSONObject(STREAM_ID);
            JSONObject stream = streamObject.getJSONObject("current_song");
            TITLE = stream.optString("title");
            ARTIST = stream.optString("artist");
            String songArt = stream.getString("image_url");
            String albumURLtemp;
            if (songArt.equals("")) {
                albumURLtemp = STATION_LOGO_URL;
            } else {
                albumURLtemp = songArt;
            }
            if (!albumURLtemp.equals(ALBUM_ART_URL)) {
                ALBUM_ART_URL = albumURLtemp;
                ALBUM_ART = drawableFromUrl(ALBUM_ART_URL);
                Palette p = Palette.from(ALBUM_ART).generate();
                int tempColor = p.getDarkVibrantColor(NOTIF_COLOR);
                if (tempColor != NOTIF_COLOR) {
                    NOTIF_COLOR = tempColor;
                }
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
