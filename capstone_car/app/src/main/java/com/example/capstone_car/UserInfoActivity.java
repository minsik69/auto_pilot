package com.example.capstone_car;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class UserInfoActivity extends AppCompatActivity {

    Context mContext;
    Button button_logout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_userinfo);

        mContext = this;

        button_logout = findViewById(R.id.button_logout);
        button_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserInfoActivity.this, LoginActivity.class);
                startActivity(intent);
                SharedPreferenceUtil.clear(mContext);
                intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
                intent.addFlags( Intent.FLAG_ACTIVITY_SINGLE_TOP );
                finish();
            }
        });
    }
}
