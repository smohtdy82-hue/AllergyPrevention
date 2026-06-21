package com.example.volunteerapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

/**
 * شاشة البداية (Splash) التي تُعرض لفترة قصيرة عند تشغيل التطبيق
 * قبل الانتقال التلقائي إلى النشاط الرئيسي {@link MainActivity}.
 */
public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 1500);
    }
}
