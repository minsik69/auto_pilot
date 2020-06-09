package com.example.capstone_car;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import io.socket.client.IO;
import io.socket.client.Socket;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    Context mContext;
    Socket mSocket;

    TextView textView_userName;
    TabLayout tablayout;
    VPAdapter vpAdapter;
    ViewPager viewpager;
    Button button_call;

    String user_id, user_name, user_phone;

    long backKeyPressedTime;        // Variable to implement the app exit function when pressing back 2 times

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        user_id = SharedPreferenceUtil.getString(mContext, "user_id");
        user_name = SharedPreferenceUtil.getString(mContext, "user_name");
        user_phone = SharedPreferenceUtil.getString(mContext, "user_phone");

        textView_userName = findViewById(R.id.textView_userName);
        textView_userName.setText(user_name + "님, 반갑습니다!");

        textView_userName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // To go to a page where you can logout
                Intent intent = new Intent(MainActivity.this, UserInfoActivity.class);
                startActivity(intent);
            }
        });

        viewpager = findViewById(R.id.viewpager);
        vpAdapter = new VPAdapter(getSupportFragmentManager());
        viewpager.setOffscreenPageLimit(2);
        viewpager.setAdapter(vpAdapter);

        tablayout = findViewById(R.id.tablayout);
        tablayout.setupWithViewPager(viewpager);

        // Outgoing Delivery, Incoming Delivery, Completed Delivery tabs
        tablayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewpager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        button_call = findViewById(R.id.button_call);
        button_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CallActivity.class);

                startActivity(intent);
            }
        });

        // Get and print the current token
        // A token is required to identify the owner of the phone and to receive push messages from the server
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @SuppressLint("LongLogTag")
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (!task.isSuccessful()) {
                    Log.w("main", "getInstanceid Failed", task.getException());
                    return;
                }
                String token = task.getResult().getToken();
                Log.d("MAIN-TOKEN : ㅣㅣㅣㅣㅣㅣㅣㅣㅣㅣ", token);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = getIntent();
        String loginId = intent.getStringExtra("loginId");

        if (loginId != null && loginId.equals("300")) {
            notiAlertDialog(intent);
        }
    }

    // When to call onNewIntent : When Activity A is called while Activity A is currently floating
    // Why you should use onNewIntent -> https://knoow.tistory.com/182
    @Override
    protected void onNewIntent(Intent intent) {
        notiAlertDialog(intent);

        super.onNewIntent(intent);
    }

    private void notiAlertDialog(Intent intent) {   // Firebase push message related functions
        if (intent != null) {
            setIntent(intent);

            // Turn on the screen
            getWindow().addFlags( WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON );

            String title = intent.getStringExtra( "title" );
            String body = intent.getStringExtra( "body" );
            String waiting_num = intent.getStringExtra( "waiting_num" );
            String car_num = intent.getStringExtra( "car_num" );
            String dlvy_num = intent.getStringExtra( "dlvy_num" );
            String sender_name = intent.getStringExtra("sender_name");
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

            if (title.equals( "배달 수락" )) {
                builder.setTitle(sender_name + "님이 물품을 보내려고 합니다.");
                if (waiting_num.equals("0")) {
                    builder.setMessage( "배달을 수락하시겠습니까?" );
                } else {
                    builder.setMessage( "대기 순위는 " +  waiting_num + "번 입니다. 대기 하시겠습니까?");
                }
                builder.setPositiveButton( "예", new DialogInterface.OnClickListener() {     // Handle when positive button is pressed
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        JSONObject data = new JSONObject();

                        try {
                            data.put("accept", "yes");
                            if (waiting_num.equals( "0" )) {
                                data.put("wait", "no");
                            } else {
                                data.put("wait", "yes");
                            }
                            data.put("car_num", car_num);
                            data.put("dlvy_num", dlvy_num);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        try {
                            mSocket = IO.socket( "https://733fafcacb7f.ngrok.io" +
                                    "" );
                        } catch(URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                        mSocket.connect();

                        mSocket.emit("accept", data);
                    }
                } );
                builder.setNegativeButton( "아니오", new DialogInterface.OnClickListener() {   // Handle when negative button is pressed
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        JSONObject data = new JSONObject();

                        try {
                            data.put("accept", "no");
                            if (waiting_num.equals( "0" )) {
                                data.put("wait", "no");
                            } else {
                                data.put("wait", "yes");
                            }
                            data.put("car_num", car_num);
                            data.put("dlvy_num", dlvy_num);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        try {
                            mSocket = IO.socket( "https://733fafcacb7f.ngrok.io" );
                        } catch(URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                        mSocket.connect();

                        mSocket.emit("accept", data);
                    }
                } );
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        }
    }

    // Press the back button twice to exit the app
    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis()>backKeyPressedTime+2000) {       // Press the back button once
            backKeyPressedTime = System.currentTimeMillis();
            Toast.makeText(this, "한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
        } else {    // Press the back button twice (exit)
            AppFinish();
        }
    }

    // App exit
    public void AppFinish() {
        finish();
        System.exit(0);
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
