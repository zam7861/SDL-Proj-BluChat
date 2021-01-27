package com.example.bluchat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;

public class SplashActivity extends AppCompatActivity {
    // Constants
    private static final int MILLIS_IN_FUTURE = 4000;
    private static final int COUNT_DOWN_INTERVAL = 1000;

    // Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        getSupportActionBar().hide();

        // Splash Screen Appears
        new CountDownTimer(MILLIS_IN_FUTURE, COUNT_DOWN_INTERVAL){
            @Override
            public void onTick(long l) {

            }
            @Override
            public void onFinish() {
                Intent splashIntent = new Intent(SplashActivity.this, ChatActivity.class);
                startActivity(splashIntent);
                finish();
            }
        }.start();
    }
}