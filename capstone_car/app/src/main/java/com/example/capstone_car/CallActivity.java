package com.example.capstone_car;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

import androidx.appcompat.app.AppCompatActivity;
import io.socket.client.IO;
import io.socket.client.Socket;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

public class CallActivity extends AppCompatActivity implements MapView.POIItemEventListener, View.OnClickListener {

    private ArrayList<Double> station_latitude;  //station's latitudes
    private ArrayList<Double> station_longitude; //station's longitudes
    private ArrayList<String> station_name;      //station's names
    private ArrayList<Double> check_latitude;    //checkpoint's latitudes
    private ArrayList<Double> check_longitude;   //checkpoint's longitudes
    private TextView notice, selected_start, selected_end, predicted_time;
    private MapView mapView;            //View for map
    private String startPoint = "";     //start point's name
    private String endPoint = "";       //end point's name
    private ArrayList<MapPOIItem> markers;  //markers
    private ViewGroup mapViewContainer;     //map view container
    private EditText input_receiver;

    private String sender_id = "";               //sender's id
    private String sender_name = "";             //sender's name
    private String receiver_id = "";             //receiver's id
    private String receiver_name = "";           //erceiver's name

    Context mContext;

    private JSONArray station_JsonArray;    //station JSONArray
    private JSONArray check_JsonArray;      //checkpoint JSONArray
    private Socket socket;

    private ProgressDialog pd;              //ProgressDialog for polyline loading

    private double distance;                //distance
    private int time;                       //predicted time

    LinearLayout relativeLayout;    //
    Animation animTransUp;   //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        new GetStation().execute("https://b8c98bce1df6.ngrok.io/api/dlvy/call");  //execute http connection for station's info

        relativeLayout = findViewById( R.id.relativeLayout );   //
        animTransUp = AnimationUtils.loadAnimation( this, R.anim.translate_up );    //

        relativeLayout.startAnimation( animTransUp );   //

        notice = findViewById(R.id.notice);
        selected_start = findViewById(R.id.selected_start);
        selected_end = findViewById(R.id.selected_end);
        input_receiver = findViewById(R.id.input_receiver);
        predicted_time = findViewById(R.id.predicted_time);

        mContext = this;
        sender_id = SharedPreferenceUtil.getString(mContext, "user_id");    //get sender's info from SharedPreference
        sender_name = SharedPreferenceUtil.getString(mContext,  "user_name");

        markers = new ArrayList<MapPOIItem>();  //

        mapView = new MapView(this);    //create map and set options
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(35.8956, 128.6220), true);
        mapView.setZoomLevel(1, true);

        mapViewContainer = findViewById(R.id.map_view);

        mapView.setPOIItemEventListener(this);      //marker's click event
        mapViewContainer.addView(mapView);          //set map for container
    }

    public void makeMarker() {      //create marker
        for (int i = 0; i < station_name.size(); i++) {
            MapPOIItem marker = new MapPOIItem();
            marker.setItemName(station_name.get(i));
            marker.setTag(i);
            marker.setMapPoint(MapPoint.mapPointWithGeoCoord(station_latitude.get(i), station_longitude.get(i)));
            marker.setMarkerType(MapPOIItem.MarkerType.BluePin);
            marker.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin);

            mapView.addPOIItem(marker);

            markers.add(marker);
        }
    }

    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) { //marker's click event
        if (startPoint.equals("")) {
            startPoint = mapPOIItem.getItemName();
            selected_start.setText(mapPOIItem.getItemName());
            notice.setText("목적지를 클릭해주세요");
        } else if (endPoint.equals("")) {
            if (mapPOIItem.getItemName().equals(startPoint)) {
                Toast.makeText(getApplicationContext(), "출발지와 목적지는 겹칠 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }
            endPoint = mapPOIItem.getItemName();
            selected_end.setText(mapPOIItem.getItemName());

            new GetCheck().execute("https://b8c98bce1df6.ngrok.io/api/dlvy/checkpoint/"+startPoint+"/"+endPoint);   //execute http connection for checkpoint's info
            pd = ProgressDialog.show(this, "로딩중", "경로 탐색 중입니다...");                 //progressdialog show

            for(int i = 0 ; i < markers.size(); i++){   //delete all markers execept selected markers
                if(!markers.get(i).getItemName().equals(startPoint) && !markers.get(i).getItemName().equals(endPoint)){
                    mapView.removePOIItem(markers.get(i));
                }
            }
            if(receiver_name != ""){
                notice.setText("모든 정보를 입력하셨습니다.");
            }else {
                notice.setText("받는 사람을 입력해주세요");
            }
        }
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) { }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) { }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) { }

    public void onClick(View view) {
        if (view.getId() == R.id.againButton) { //reset button
            markers.clear();                    //clear the option
            mapView.removeAllPOIItems();
            mapView.removeAllPolylines();
            startPoint = "";
            endPoint = "";
            distance = 0;
            time = 0;
            predicted_time.setVisibility(View.INVISIBLE);
            notice.setText("출발지를 클릭해주세요");
            selected_start.setText("마커를\n클릭해주세요");
            selected_end.setText("마커를\n클릭해주세요");
            makeMarker();  //create the markers again
        } else if (view.getId() == R.id.search) {   //when user click search button
            if (input_receiver.getText().toString().length() == 0) {
                Toast.makeText(this, "받는 사람의 이름을 입력하지 않았습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, com.example.capstone_car.Search_popup.class);
                intent.putExtra("receiver_name", input_receiver.getText().toString());
                startActivityForResult(intent, 1);
            }
        } else if (view.getId() == R.id.call) { //when user click call button
            if (startPoint.equals("")) {
                Toast.makeText(this, "출발지를 정해주세요", Toast.LENGTH_SHORT).show();
            } else if (endPoint.equals("")) {
                Toast.makeText(this, "목적지를 정해주세요", Toast.LENGTH_SHORT).show();
            } else if (receiver_name.equals("")) {
                Toast.makeText(this, "받는 사람을 정해주세요", Toast.LENGTH_SHORT).show();
            } else {
                JSONObject data = new JSONObject(); //data to send socket io server
                try{
                    data.put("sender_id", sender_id);
                    data.put("receiver_id", receiver_id);
                    data.put("start_point", selected_start.getText());
                    data.put("end_point", selected_end.getText());
                    data.put("sender_name", sender_name);
                }catch(JSONException e){
                    e.printStackTrace();
                }

                try{
                    socket = IO.socket("https://733fafcacb7f.ngrok.io");
                }catch(URISyntaxException e){
                    throw new RuntimeException(e);
                }
                socket.connect();

                socket.emit("dlvy_call", data);

                finish();
                Toast.makeText(this, "센더 : " + sender_name + ",리시버 : " + receiver_name + ", 출발지 : " + selected_start.getText() + ", 목적지 : " + selected_end.getText(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) { //get intent's data
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                if (data.getStringExtra("name") != null) {
                    receiver_id = data.getStringExtra("id");
                    receiver_name = data.getStringExtra("name");
                    StringBuilder builder = new StringBuilder(data.getStringExtra("number"));
                    builder.setCharAt(11, 'X');
                    builder.setCharAt(12, 'X');

                    input_receiver.setText(data.getStringExtra("name") + "(" + builder.toString() + ")");
                    if(!startPoint.equals("")){
                        notice.setText("모든 정보를 입력하셨습니다.");
                    }else{
                        notice.setText("출발지를 클릭해주세요.");
                    }
                }else{
                    input_receiver.setText("");
                }
            }
        }
    }

    public class GetStation extends AsyncTask<String, String, String> {   //get station
        @Override
        protected String doInBackground(String... params) {   //connect to server

            try {
                HttpURLConnection con = null;
                BufferedReader reader = null;

                try {
                    URL url = new URL(params[0]);
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Cache-Control", "no-cache");
                    con.setRequestProperty("Accept","application/json;characterset=utf-8");
                    con.setDoInput(true);
                    con.connect();

                    InputStream stream = con.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(stream,"UTF-8"));
                    StringBuffer buffer = new StringBuffer();
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }

                    JSONObject json = null;
                    json = new JSONObject(new String(buffer));
                    JSONArray stations = json.getJSONArray("station_all");

                    station_JsonArray = stations;
                    stationParse();

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
    }

    public class GetCheck extends AsyncTask<String, String, String> {  //get checkpoint
        @Override
        protected String doInBackground(String... params) {   //connect to server

            try {
                HttpURLConnection con = null;
                BufferedReader reader = null;

                try {
                    URL url = new URL(params[0]);
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Cache-Control", "no-cache");
                    con.setRequestProperty("Accept","application/json;characterset=utf-8");
                    con.setDoInput(true);
                    con.connect();

                    InputStream stream = con.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(stream,"UTF-8"));
                    StringBuffer buffer = new StringBuffer();
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }
                    JSONArray check = null;
                    check = new JSONArray(new String(buffer));

                    check_JsonArray = check;

                    checkParse();

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
    }

    public void stationParse(){ //parsing station's info
        station_latitude = new ArrayList<Double>();
        station_longitude = new ArrayList<Double>();
        station_name = new ArrayList<String>();
        for(int i=0; i < station_JsonArray.length(); i++){
            try {
                JSONObject jObject = station_JsonArray.getJSONObject(i);
                station_latitude.add(jObject.getDouble("station_lat"));
                station_longitude.add(jObject.getDouble("station_lon"));
                station_name.add(jObject.getString("station_name"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        makeMarker();
    }

    public void checkParse(){   //parsing checkpoint's info
        check_latitude = new ArrayList<Double>();
        check_longitude = new ArrayList<Double>();
        for(int i=0; i < check_JsonArray.length(); i++){
            try {
                JSONObject jObject = check_JsonArray.getJSONObject(i);
                check_latitude.add(jObject.getDouble("checkpoint_lat"));
                check_longitude.add(jObject.getDouble("checkpoint_lon"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        makePolyline();
    }

    public void makePolyline(){ //create polyline
        MapPolyline polyline = new MapPolyline();
        polyline.setLineColor(Color.argb(128, 255, 51, 0));
        int start_index = station_name.indexOf(startPoint);
        int end_index = station_name.indexOf(endPoint);
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(station_latitude.get(start_index), station_longitude.get(start_index)));
        for (int i = 0, len = check_JsonArray.length(); i < len; i++){
            polyline.addPoint(MapPoint.mapPointWithGeoCoord(check_latitude.get(i), check_longitude.get(i)));
        }
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(station_latitude.get(end_index), station_longitude.get(end_index)));
        mapView.addPolyline(polyline);
        cal_Distance();

        pd.dismiss();
    }

    public void cal_Distance(){ //calculation the distance
        for(int i = 0, len = check_JsonArray.length(); i < len-1; i++){
            double startLat = (check_latitude.get(i) * Math.PI)/180;
            double startLon = (check_longitude.get(i) * Math.PI)/180;
            double endLat = (check_latitude.get(i+1) * Math.PI)/180;
            double endLon = (check_longitude.get(i+1) * Math.PI)/180;
            distance += Math.acos(Math.sin(startLat) * Math.sin(endLat) + Math.cos(startLat) * Math.cos(endLat) * Math.cos(startLon - endLon)) * 6371;
        }
        time = (int)Math.ceil(distance / (1.0/12));
        new Thread(new Runnable() {    //To do UI working from outside UI Thread
            @Override
            public void run() {
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        predicted_time.setVisibility(View.VISIBLE);
                        predicted_time.setText("예상 소요 시간 : " + time + "분!!");
                    }
                });
            }
        }).start();
    }
}