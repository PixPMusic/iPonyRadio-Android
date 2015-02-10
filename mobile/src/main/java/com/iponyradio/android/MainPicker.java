package com.iponyradio.android;

import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;


public class MainPicker extends ListActivity {

    private ProgressDialog pDialog;

    // URL to get contacts JSON
    private static String url = "http://iponyradio.com/android-api";

    // JSON Node names
    private static final String TAG_RESULT = "result";
    private static final String TAG_ID = "id";
    private static final String TAG_NAME = "name";

    // contacts JSONArray
    JSONArray stations = null;

    // Hashmap for ListView
    ArrayList<HashMap<String, String>> stationList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_picker);

        stationList = new ArrayList<HashMap<String, String>>();

        ListView lv = getListView();

        // Listview on item click listener
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // getting values from selected ListItem
                String name = ((TextView) view.findViewById(R.id.entry_title))
                        .getText().toString();

                String station_id = ((TextView) view.findViewById(R.id.id))
                        .getText().toString();
                // Starting single contact activity
                Intent in = new Intent(getApplicationContext(),
                        SingleStationActivity.class);
                in.putExtra(TAG_NAME, name);
                in.putExtra("id", station_id);
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
            pDialog = new ProgressDialog(MainPicker.this);
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

                    // looping through All Contacts
                    for (int i = 0; i < stations.length(); i++) {
                        JSONObject s = stations.getJSONObject(i);

                        String id = s.getString(TAG_ID);
                        String name = s.getString(TAG_NAME);

                        // tmp hashmap for single station
                        HashMap<String, String> station = new HashMap<String, String>();

                        // adding each child node to HashMap key => value
                        station.put(TAG_ID, id);
                        station.put(TAG_NAME, name);

                        // adding contact to contact list
                        stationList.add(station);
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
                    MainPicker.this, stationList,
                    R.layout.list_item, new String[] { TAG_NAME, TAG_ID,}, new int[] { R.id.entry_title, R.id.id });

            setListAdapter(adapter);
        }

    }

}