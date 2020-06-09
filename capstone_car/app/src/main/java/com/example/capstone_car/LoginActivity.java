package com.example.capstone_car;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.disposables.CompositeDisposable;

public class LoginActivity extends AppCompatActivity {
    Context mContext;

    EditText editText_id, editText_password;
    Button button_login, button_signup;

    String user_id, user_password;

    long backKeyPressedTime;    // Variable to implement the app exit function when pressing back 2 times

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mContext = this;

        editText_id = findViewById(R.id.editText_id);
        editText_password = findViewById(R.id.editText_password);
        button_signup = findViewById(R.id.button_signup);

        user_id = SharedPreferenceUtil.getString(mContext, "user_id");
        user_password = SharedPreferenceUtil.getString(mContext, "user_password");

        // Implement login retention
        if (!user_id.equals("") && !user_password.equals("")) {             // If the current user_id and user_password are not empty
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else if (user_id.equals("") && user_password.equals("")) {        // If there is no value in the current user_id and user_password
            button_signup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getApplicationContext(), SignUpActivity.class);
                    startActivity(intent);
                    finish();
                }
            });

            button_login = findViewById(R.id.button_login);
            button_login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new JSONTask().execute("https://b8c98bce1df6.ngrok.io/api/app/login");
                }
            });
        }
    }

    public class JSONTask extends AsyncTask<String, String, String> {       // POST method
        String user_id = editText_id.getText().toString().trim();
        String user_password = editText_password.getText().toString().trim();
        String user_name, user_phone;

        @Override
        protected String doInBackground(String... urls) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("login_id", user_id);
                jsonObject.accumulate("login_password", user_password);
                HttpURLConnection con = null;
                BufferedReader reader = null;

                try {
                    URL url = new URL(urls[0]);
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Cache-Control", "no-cache");
                    con.setRequestProperty("Content-Type", "application/json");
                    con.setRequestProperty("Accept", "text/html");
                    con.setDoOutput(true);
                    con.setDoInput(true);
                    con.connect();

                    OutputStream outStream = con.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream));

                    writer.write(jsonObject.toString());
                    writer.flush();
                    writer.close();

                    InputStream stream = con.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuffer buffer = new StringBuffer();
                    String line = "";
                    while ((line = reader.readLine()) != null) {
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
            super.onPostExecute( result );

            try {
                JSONObject obj = new JSONObject( result );

                if (obj.getString( "success" ).equals( "FALSE" )) {
                    Toast.makeText( getApplicationContext(), "아이디나 비밀번호를 다시 확인해주세요!", Toast.LENGTH_SHORT ).show();
                    editText_id.setText( "" );
                    editText_password.setText( "" );
                } else {
                    Intent intent = new Intent( LoginActivity.this, MainActivity.class );
                    user_name = obj.getString( "user_info" );

                    // Store in user information app
                    SharedPreferenceUtil.setString( mContext, "user_id", user_id );
                    SharedPreferenceUtil.setString(mContext, "user_password", user_password);
                    SharedPreferenceUtil.setString( mContext, "user_name", user_name );

                    intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
                    startActivity( intent );
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // Press the back button twice to exit the app
    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis()>backKeyPressedTime+2000) {   // Press the back button once
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
