package lunchgym.first.showmeyourwork;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class CrawlingActivity extends Activity {

    private String htmlPageUrl =""; //파싱할 홈페이지의 URL주소
    private TextView textviewHtmlDocument;

    WebView webViewForCrawling;
    WebView finalWebView;
    JsoupAsyncTask jsoupAsyncTask;

    int cnt=0;
    String videoUrl = "null";
    String source = "";
    String videoSrc;
    String playUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_crawling);
        Intent getIntent = getIntent();
        htmlPageUrl = getIntent.getStringExtra("htmlPageUrl");
        Log.e("crawlingActivity", "htmlPageUrl : "+htmlPageUrl );

        textviewHtmlDocument = (TextView) findViewById(R.id.textView);
        textviewHtmlDocument.setMovementMethod(new ScrollingMovementMethod());

        finalWebView = findViewById(R.id.crawling_webview_finish);
        finalWebView.setVisibility(View.INVISIBLE);

        webViewForCrawling = findViewById(R.id.crawling_webview);
//        webViewForCrawling.setVisibility(View.INVISIBLE);

        //Webview 자바스크립트 활성화
        webViewForCrawling.getSettings().setJavaScriptEnabled(true);
        //자바스크립트 인터페이스 연결
        webViewForCrawling.addJavascriptInterface(new MyJavascriptInterface(), "Android");
        webViewForCrawling.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                //자바스크립트 인터페이스로 연결되어있는 getHtml을 실행
                //자바스크립트 기본 메소드로 html소스를 통째로 지정해서 인자로 넘겨줌
                view.loadUrl("javascript:window.Android.getHtml(document.getElementsByTagName('body')[0].innerHTML);");
            }
        });
        webViewForCrawling.loadUrl(htmlPageUrl);

        Button htmlTitleButton =  findViewById(R.id.button);
        htmlTitleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jsoupAsyncTask = new JsoupAsyncTask();
                jsoupAsyncTask.execute();

            }
        });


    }

    public class MyJavascriptInterface {
        @JavascriptInterface
        public void getHtml(String html){
            //위 자바스크립트가 호출되면 여기로 html이 반환된다.
            source = html;
        }
    }

    private class JsoupAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                //가짜 webview에서 받아온 String source를 Document로 파싱한다.
                Document doc = Jsoup.parse(source);

                Elements urlElements = doc.select("li[id=item_index1]");
                //linkUrl을 가져온다.
                String linkUrl = urlElements.attr("data-cr-url");
                Log.e("crawlingactivity", "url: "+linkUrl);

                //video의 source를 가져온다.
                Elements videoSrcElements = doc.select("li[id=item_index1] div div a");
                videoSrc = videoSrcElements.attr("data-api");
                Log.e("crawlingactivity", "videoSrc: "+ videoSrc);


                Document forVideo = Jsoup.connect(videoSrc).get();

                Elements videoUrlElements = forVideo.select("body");
                String videoUrl = videoUrlElements.text();
                Log.e("crawlingactivity", "url2: "+videoUrl);

                String splitVideoUrl = videoUrl.split("sPlayUrl\":\"")[1];
                playUrl = splitVideoUrl.split("\"")[0];

                Log.e("crawlingactivity", "playUrl: "+ playUrl);


            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            textviewHtmlDocument.setText(source);

            finalWebView.setVisibility(View.VISIBLE);
            finalWebView.loadUrl(playUrl);
        }
    }
}
