package lunchgym.first.showmeyourwork;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity{
    Button cameraBtn;
    Button exoplayerBtn;
    Button webviewBtn;
    Button crawlingBtn;
    Intent intent;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraBtn = findViewById(R.id.camera_btn);
        exoplayerBtn = findViewById(R.id.exoplayer_btn);
        webviewBtn = findViewById(R.id.webview_btn);
        crawlingBtn = findViewById(R.id.crawling_btn);

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        webviewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(MainActivity.this, WebviewActivity.class);
                startActivity(intent);
            }
        });
        exoplayerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(MainActivity.this, ExoplayerActivity.class);
                startActivity(intent);
            }
        });

        crawlingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(MainActivity.this, CrawlingActivity.class);
                startActivity(intent);
            }
        });


    }
}


