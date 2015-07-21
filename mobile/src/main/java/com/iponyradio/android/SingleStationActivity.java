package com.iponyradio.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.iponyradio.android.recycler.FeedItem;
import com.iponyradio.android.recycler.MyRecyclerAdapter;
import com.iponyradio.android.recycler.RecyclerItemClickListener;
import com.millennialmedia.android.MMAdView;
import com.millennialmedia.android.MMRequest;
import com.millennialmedia.android.MMSDK;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import wseemann.media.FFmpegMediaMetadataRetriever;
import wseemann.media.Metadata;

public class SingleStationActivity extends Activity {

    private List<FeedItem> feedsList;
    private RecyclerView mRecyclerView;
    private MyRecyclerAdapter adapter;
    private ProgressBar progressBar;
    private JSONObject json;

    private ProgressDialog pDialog;

    // URL to get contacts JSON
    private static String url = "http://iponyradio.com/android-api";

    // JSON Node names
    private static final String TAG_RESULT = "result";
    private static final String TAG_ID = "id";
    private static final String TAG_STREAMS = "streams";
    private static final String TAG_STREAM_ID = "id";
    private static final String TAG_STREAM_NAME = "name";
    private static final String TAG_STREAM_URL = "url";
	private static final String TAG_NAME = "name";
    private static int id;
    private String station_name;
    private String jsonStr;
    private Drawable d;
    private ImageView StationLogo;
    private static ArrayList<String> StreamURLs;
    private static String station_shortcode;

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_station);

        MMSDK.initialize(this);

        //Find the ad view for reference
        MMAdView adViewFromXml = (MMAdView) findViewById(R.id.adView2);

        //MMRequest object
        MMRequest request = new MMRequest();

        adViewFromXml.setMMRequest(request);

        adViewFromXml.getAd();

        // getting intent data
        Intent in = getIntent();
        
        // Get JSON values from SharedPrefs
        Context c = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(c);
        station_name = prefs.getString("CURRENT_STATION_NAME", null);
        id = prefs.getInt("CURRENT_STATION_ID", 0);
        jsonStr = prefs.getString("JSON_DATA", null);

        StationLogo = (ImageView)findViewById(R.id.stationlogo);

        // Initialize recycler view
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        // Downloading data from below url
        new AsyncHttpTask().execute();

        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(c, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {

                        // getting values from selected ListItem
                        String stream_name = ((TextView) view.findViewById(R.id.title))
                                .getText().toString();

                        String stream_url = StreamURLs.get(position);
                        // Create Intent for Player
                        Intent in = new Intent(getApplicationContext(),
                                PlayerActivity.class);

                        //Commit selection to prefs for better resuming
                        editor = prefs.edit();
                        editor.putString("CURRENT_STREAM_NAME", stream_name);
                        editor.putString("CURRENT_STREAM_URL", stream_url);
                        editor.putString("CURRENT_STATION_SHORTCODE", station_shortcode);
                        editor.putInt("CURRENT_STREAM_IDq", position);
                        editor.commit();

                        //Start the activity
                        startActivity(in);
                    }
                })
        );
    }


    public class AsyncHttpTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected Integer doInBackground(String... params) {
            parseResult();
            return 1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            // Download complete. Let us update UI
            progressBar.setVisibility(View.GONE);

            if (result == 1) {
                StationLogo.setImageDrawable(d);
                adapter = new MyRecyclerAdapter(SingleStationActivity.this, feedsList);
                mRecyclerView.setAdapter(adapter);
            }
        }
    }

    private void parseResult() {
        try {
            StreamURLs = new ArrayList<String>();
            json = new JSONObject(jsonStr);
            JSONArray stations = json.optJSONArray("result");
            feedsList = new ArrayList<>();
            JSONObject s = stations.optJSONObject(id);
            JSONArray streams = s.getJSONArray(TAG_STREAMS);

            station_shortcode = s.optString("shortcode");
            String imageURL = s.optString("image_url");

            d = drawableFromUrl(imageURL);

            for (int j = 0; j < streams.length(); j++) {
                JSONObject st = streams.getJSONObject(j);

                FeedItem stream = new FeedItem();
                stream.setTitle(st.optString(TAG_STREAM_NAME));
                stream.setThumbnail("ignore");
                StreamURLs.add(st.optString(TAG_STREAM_URL));
                feedsList.add(stream);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Drawable drawableFromUrl(String url) throws IOException {
        Bitmap x;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.connect();
        InputStream input = connection.getInputStream();

        x = BitmapFactory.decodeStream(input);
        return new BitmapDrawable(x);
    }


}


