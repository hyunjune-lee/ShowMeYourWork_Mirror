package lunchgym.first.showmeyourwork;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class MainActivity extends AppCompatActivity{
    Button cameraBtn;
    Button exoplayerBtn;
    Button webviewBtn;
    Button crawlingBtn;
    Intent intent;
    String input = "";
    String cardinal;
    String name;
    private String htmlPageUrl ="https://search.naver.com/search.naver?where=video&sm=tab_jum&query="; //파싱할 홈페이지의 URL주소


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
                final EditText editText = new EditText(MainActivity.this);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("입력");
                builder.setView(editText);
                builder.setPositiveButton("입력", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        input = editText.getText().toString();

                        if(input.length()>0){
                            cardinal = input.substring(0, 2);
                            name = input.substring(3);
                        }
                        Log.e("cralingActivity", "cardinal :"+cardinal);
                        Log.e("cralingActivity", "name :"+name);

                        htmlPageUrl = htmlPageUrl+cardinal+"%20"+name;

                        intent = new Intent(MainActivity.this, CrawlingActivity.class);
                        intent.putExtra("htmlPageUrl", htmlPageUrl);
                        startActivity(intent);

                    }
                });

                builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                builder.show();




            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        htmlPageUrl ="https://search.naver.com/search.naver?where=video&sm=tab_jum&query=";
    }
}


