package com.example.capstone_car;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import java.net.Socket;
import java.net.URL;

import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.disposables.CompositeDisposable;

public class SignUpActivity extends AppCompatActivity {
    CompositeDisposable compositeDisposable = new CompositeDisposable();

    EditText editText_id, editText_password, editText_name, editText_tel;
    AutoCompleteTextView editText_univ;
    Button button_login, button_signup;

    // List for autocomplete
    String[] arWords = new String[] {
            "경북대학교", "경운대학교", "경일대학교", "계명대학교 대명캠퍼스", "대구교육대학교", "대구대학교",
            "대구가톨릭대학교 효성캠퍼스", "영남대학교 경산캠퍼스", "영진전문대학교 복현캠퍼스", "한동대학교"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Create an adapter and associate it with an autocomplete string list
        ArrayAdapter<String> adWord = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, arWords);
        editText_univ = (AutoCompleteTextView)findViewById(R.id.editText_univ);

        // Adapter settings
        editText_univ.setAdapter(adWord);

        editText_id = findViewById(R.id.editText_id);
        editText_password = findViewById(R.id.editText_password);
        editText_name = findViewById(R.id.editText_name);
        editText_tel = findViewById(R.id.editText_tel);

        button_login = findViewById(R.id.button_login);
        button_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        button_signup = findViewById(R.id.button_signup);
        button_signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new JSONTask().execute("https://b8c98bce1df6.ngrok.io"); // Start AsyncTask
            }
        });
    }

    @Override
    protected void onStop() {
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    public class JSONTask extends AsyncTask<String, String, String> {

        String user_id = editText_id.getText().toString().trim();
        String user_password = editText_password.getText().toString().trim();
        String user_name = editText_name.getText().toString().trim();
        String user_phone_number = editText_tel.getText().toString().trim();
        String user_univ = editText_univ.getText().toString().trim();

        @Override
        protected String doInBackground(String... urls) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("user_id", user_id);
                jsonObject.accumulate("password", user_password);
                jsonObject.accumulate("name", user_name);
                jsonObject.accumulate("phone_number", user_phone_number);
                jsonObject.accumulate("univ", user_univ);
                HttpURLConnection con = null;
                BufferedReader reader = null;

                try {
                    URL url = new URL(urls[0]);
                    con = (HttpURLConnection)url.openConnection();
                    con.setRequestMethod("POST"); // Send by poast method
                    con.setRequestProperty("Cache-Control", "no-cache"); // Cache settings
                    con.setRequestProperty("Content-Type", "application/json"); // Send in application JSON format
                    con.setRequestProperty("Accept", "text/html"); // Server receives response data in HTML
                    con.setDoOutput(true); // Means to pass post data to outstream
                    con.setDoInput(true); // Means to receive a response from the server as an input stream
                    con.connect();
                    OutputStream outStream = con.getOutputStream(); // Create stream to send to server

                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream)); // Create and put buffer

                    writer.write(jsonObject.toString());
                    writer.flush();
                    writer.close(); // Receive buffer

                    // Receive data from server
                    InputStream stream = con.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(stream)); // Declare buffers to speed up and reduce load
                    StringBuffer buffer = new StringBuffer();
                    String line = ""; // Temp variable for receiving a string per line
                    while ((line = reader.readLine()) != null) { // The part that gets data from the actual reader. That is, fetching data from the server
                        buffer.append(line);
                    }

                    return buffer.toString(); // Returns the value received from the server
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
                            reader.close(); // Close buffer
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

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result.equals("ok")) {
                Toast.makeText(getApplicationContext(), "회원가입에 성공했습니다", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }
}