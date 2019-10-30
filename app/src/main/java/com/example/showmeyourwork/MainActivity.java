package com.example.showmeyourwork;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button videoVidsBtn;
    Button webViewBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoVidsBtn = findViewById(R.id.activity_mainactivity_video_btn);
        videoVidsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ActivityVideoPlay.class);
                startActivity(intent);
            }
        });

        webViewBtn = findViewById(R.id.webview_btn);
        webViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ActivityWebViewPlay.class);
                startActivity(intent);
            }
        });




    }
}
