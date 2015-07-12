package com.iponyradio.android;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.millennialmedia.android.MMAdView;
import com.millennialmedia.android.MMRequest;
import com.millennialmedia.android.MMSDK;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class SingleStationActivity extends ListActivity {

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
    private static String id_text;
    private String station_name;
    private String jsonStr;
    private Drawable d;
    private ImageView StationLogo;

    // contacts JSONArray
    JSONArray stations = null;

    // Hashmap for ListView
    ArrayList<HashMap<String, String>> stationList;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_station);

        MMSDK.initialize(this);

        //Find the ad view for reference
        MMAdView adViewFromXml = (MMAdView) findViewById(R.id.adView2);

        //MMRequest object
        MMRequest request = new MMRequest();

        // getting intent data
        Intent in = getIntent();
        
        // Get JSON values from previous intent
        station_name = in.getStringExtra(TAG_NAME);
        id_text = in.getStringExtra(TAG_ID);
        jsonStr = in.getStringExtra("json");

        stationList = new ArrayList<HashMap<String, String>>();

        StationLogo = (ImageView)findViewById(R.id.stationlogo);
        ListView lv = getListView();

        // Listview on item click listener
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // getting values from selected ListItem
                String stream_name = ((TextView) view.findViewById(R.id.entry_title))
                        .getText().toString();

                String stream_url = ((TextView) view.findViewById(R.id.list_item_stream_url))
                        .getText().toString();
                // Starting single contact activity
                Intent in = new Intent(getApplicationContext(),
                        PlayerActivity.class);
                in.putExtra("STATION_NAME", station_name);
                in.putExtra("STREAM_NAME", stream_name);
                in.putExtra("STREAM_URL", stream_url);
                startActivity(in);

            }
        });

        // Calling async task to get json
        new GetContacts().execute();
    }

    /**
     * Async task class to get json by making HTTP call
     * */
    private class GetContacts extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(SingleStationActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        Drawable drawableFromUrl(String url) throws IOException {
            Bitmap x;

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.connect();
            InputStream input = connection.getInputStream();

            x = BitmapFactory.decodeStream(input);
            return new BitmapDrawable(x);
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    stations = jsonObj.getJSONArray(TAG_RESULT);
                    JSONObject s = stations.getJSONObject(Integer.parseInt(id_text));
                    String imageURL = s.optString("image_url");
                    Log.d("test", imageURL);
                    d = drawableFromUrl(imageURL);

                        // Stream node is JSON Array
                        JSONArray streams = s.getJSONArray(TAG_STREAMS);
                        for (int j = 0; j < streams.length(); j++) {
                            JSONObject st = streams.getJSONObject(j);

                            String stream_id = st.getString(TAG_STREAM_ID);
                            String stream_name = st.getString(TAG_STREAM_NAME);
                            String stream_url = st.getString(TAG_STREAM_URL);

                            HashMap<String, String> stream = new HashMap<String, String>();

                            stream.put(TAG_STREAM_ID, stream_id);
                            stream.put(TAG_STREAM_NAME, stream_name);
                            stream.put(TAG_STREAM_URL, stream_url);

                            // adding contact to contact list
                            stationList.add(stream);
                        }

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
            /**
             * Updating parsed JSON data into ListView
             * */
            StationLogo.setImageDrawable(d);
            ListAdapter adapter = new SimpleAdapter(
                    SingleStationActivity.this, stationList,
                    R.layout.list_item, new String[] {TAG_NAME, TAG_STREAM_URL}, new int[] { R.id.entry_title, R.id.list_item_stream_url });
            setListAdapter(adapter);
        }

    }
}
