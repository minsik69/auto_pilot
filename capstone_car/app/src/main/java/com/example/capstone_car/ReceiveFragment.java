package com.example.capstone_car;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class ReceiveFragment extends Fragment {
    private ArrayList<List> list = new ArrayList<>();
    private RecyclerView receiveList;
    private ReceiveListAdapter mAdapter;
    private RelativeLayout layout_nothing;

    private Button button_refresh;

    String user_id;

    Context mContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_receive, container, false);

        mContext = container.getContext();      // Fragment can't use 'this' to get context differently from activities

        user_id = SharedPreferenceUtil.getString(mContext, "user_id");      // User ID logged in

        layout_nothing = (RelativeLayout) v.findViewById(R.id.layout_nothing);
        button_refresh = (Button) v.findViewById(R.id.button_refresh);

        //RecyclerView
        receiveList = (RecyclerView) v.findViewById(R.id.receiveList);
        receiveList.setHasFixedSize(true);
        mAdapter = new ReceiveListAdapter(list);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        receiveList.setLayoutManager(mLayoutManager);
        receiveList.setItemAnimator(new DefaultItemAnimator());
        receiveList.setAdapter(mAdapter);

        button_refresh.setOnClickListener( new View.OnClickListener() {     // Manually refresh data
            @Override
            public void onClick(View v) {
                prepareData();
            }
        } );

        return v;
    }

    @Override
    public void onResume() {
        prepareData();

        super.onResume();
    }

    private void prepareData() {
        new JSONTask().execute("https://b8c98bce1df6.ngrok.io/api/dlvy/receivdlvy/"+user_id);
    }

    public class JSONTask extends AsyncTask<String, String, String> {
        String user_id = SharedPreferenceUtil.getString(mContext, "user_id");

        @Override
        protected String doInBackground(String... urls) {   // GET method
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate( "user_id", user_id );
                HttpURLConnection con = null;
                BufferedReader reader = null;

                try {
                    URL url = new URL( urls[0] );
                    con = (HttpURLConnection) url.openConnection();
                    con.connect();
                    InputStream stream = con.getInputStream();
                    reader = new BufferedReader( new InputStreamReader(stream) );
                    StringBuffer buffer = new StringBuffer();
                    String line = "";

                    while((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }

                    return buffer.toString();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }

                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @SuppressLint("LongLogTag")
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            try {
                JSONObject obj = new JSONObject(result);

                if (obj.has("value")) {     // If there is no list
                    list.clear();
                    layout_nothing.setVisibility( View.VISIBLE );
                    mAdapter.notifyDataSetChanged();
                } else {
                    layout_nothing.setVisibility( View.GONE );
                    JSONArray diarray = obj.getJSONArray("dlvy_info");
                    JSONArray dwnarray = obj.getJSONArray("dlvy_wait_num");
                    JSONArray usarray = obj.getJSONArray("user_name");
                    JSONArray rgarray = obj.getJSONArray("rc_gps");
                    JSONArray ssarray = obj.getJSONArray("station_start");
                    JSONArray searray = obj.getJSONArray("station_end");

                    int[] dlvy_wait_num = new int[dwnarray.length()];

                    list.clear();       // Deleting a delivered list

                    for (int i = 0; i < diarray.length(); i++) {
                        dlvy_wait_num[i] = dwnarray.getInt(i);

                        JSONObject dlvy_info = diarray.getJSONObject(i);
                        JSONObject rc_gps = rgarray.getJSONObject(i);
                        JSONObject station_start = ssarray.getJSONObject(i);
                        JSONObject station_end = searray.getJSONObject(i);

                        switch (dlvy_info.getString( "dlvy_status" )) {
                            case "대기중":
                                list.add( new List( dlvy_info.getInt( "dlvy_num" ), dlvy_wait_num[i], usarray.get( i ).toString(), station_start.getString( "station_name" ), station_end.getString( "station_name" ), dlvy_info.getString( "dlvy_status" ), ReceiveCode.ViewType.WAIT ) );
                                break;
                            case "호출중": {
                                double distance = predictTime( rc_gps.getDouble( "car_lat" ), rc_gps.getDouble( "car_lon" ), station_start.getDouble( "station_lat" ), station_start.getDouble( "station_lon" ) );
                                int time = (int) Math.ceil( distance / (1.0 / 12) );
                                list.add( new List( dlvy_info.getInt( "dlvy_num" ), rc_gps.getInt("car_num"), usarray.get( i ).toString(), station_start.getString( "station_name" ), station_end.getString( "station_name" ), time, dlvy_info.getString( "dlvy_status" ), ReceiveCode.ViewType.DLVYWAIT ) );
                                break;
                            }
                            case "배달중": {
                                double distance = predictTime( station_start.getDouble( "station_lat" ), station_start.getDouble( "station_lon" ), station_end.getDouble( "station_lat" ), station_end.getDouble( "station_lon" ) );
                                int time = (int) Math.ceil( distance / (1.0 / 12) );
                                list.add( new List( dlvy_info.getInt( "dlvy_num" ), rc_gps.getInt("car_num"), usarray.get( i ).toString(), station_start.getString( "station_name" ), station_end.getString( "station_name" ), time, dlvy_info.getString( "dlvy_status" ), ReceiveCode.ViewType.DLVY ) );
                                break;
                            }
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // Finding the Estimated Time
    private double predictTime(double car_lat, double car_lon, double station_lat, double station_lon) {
        double startLat = degreesToRadians(car_lat);
        double startLon = degreesToRadians(car_lon);
        double endLat = degreesToRadians(station_lat);
        double endLon = degreesToRadians(station_lon);
        int radius = 6371;

        return Math.acos(Math.sin(startLat) * Math.sin(endLat) + Math.cos(startLat) * Math.cos(endLat) * Math.cos(startLon - endLon)) * radius;
    }

    // Finding the Estimated Time
    private double degreesToRadians(double data) {
        return (data * Math.PI)/180;
    }
}