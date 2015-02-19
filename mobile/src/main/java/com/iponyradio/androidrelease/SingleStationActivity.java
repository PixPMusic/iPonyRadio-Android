package com.iponyradio.androidrelease;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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

    // contacts JSONArray
    JSONArray stations = null;

    // Hashmap for ListView
    ArrayList<HashMap<String, String>> stationList;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_station);
        
        // getting intent data
        Intent in = getIntent();
        
        // Get JSON values from previous intent
        station_name = in.getStringExtra(TAG_NAME);
        id_text = in.getStringExtra(TAG_ID);
        
        // Displaying all values on the screen
        TextView lblName = (TextView) findViewById(R.id.station_name);
        
        lblName.setText(station_name);

        stationList = new ArrayList<HashMap<String, String>>();

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

        @Override
        protected Void doInBackground(Void... arg0) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url, ServiceHandler.GET);

            Log.d("Response: ", "> " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    stations = jsonObj.getJSONArray(TAG_RESULT);
                    JSONObject s = stations.getJSONObject(Integer.parseInt(id_text) -1);

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
            ListAdapter adapter = new SimpleAdapter(
                    SingleStationActivity.this, stationList,
                    R.layout.list_item, new String[] {TAG_NAME, TAG_STREAM_URL}, new int[] { R.id.entry_title, R.id.list_item_stream_url });
            setListAdapter(adapter);
        }

    }
}
