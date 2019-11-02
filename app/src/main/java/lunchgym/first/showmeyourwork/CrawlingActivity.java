package lunchgym.first.showmeyourwork;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class CrawlingActivity extends Activity {

//    private String htmlPageUrl ="http://www.yonhapnews.co.kr/"; //파싱할 홈페이지의 URL주소
//    private String htmlPageUrl ="https://section.cafe.naver.com/cafe-home/search/combinations?query="; //파싱할 홈페이지의 URL주소
    private String htmlPageUrl =""; //파싱할 홈페이지의 URL주소
    private TextView textviewHtmlDocument;
    private String htmlContentInStringFormat="";
    String input = "";
    String cardinal;
    String name;
    WebView crawlingWebview;
    WebView crawlingWebviewFinish;
    JsoupAsyncTask jsoupAsyncTask;

    int cnt=0;
    String videoUrl = "null";
    String source = "";
    String url1;
    String playUrl1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crawling);
        Intent getIntent = getIntent();
        htmlPageUrl = getIntent.getStringExtra("htmlPageUrl");
        Log.e("crawlingActivity", "htmlPageUrl : "+htmlPageUrl );

        textviewHtmlDocument = (TextView) findViewById(R.id.textView);
        textviewHtmlDocument.setMovementMethod(new ScrollingMovementMethod());

        crawlingWebviewFinish = findViewById(R.id.crawling_webview_finish);
        crawlingWebviewFinish.setVisibility(View.INVISIBLE);

        crawlingWebview = findViewById(R.id.crawling_webview);
//        crawlingWebview.setVisibility(View.INVISIBLE);

        //Webview 자바스크립트 활성화
        crawlingWebview.getSettings().setJavaScriptEnabled(true);
        //자바스크립트 인터페이스 연결
        crawlingWebview.addJavascriptInterface(new MyJavascriptInterface(), "Android");
        crawlingWebview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                //자바스크립트 인터페이스로 연결되어있는 getHtml을 실행
                //자바스크립트 기본 메소드로 html소스를 통째로 지정해서 인자로 넘겨줌
                view.loadUrl("javascript:window.Android.getHtml(document.getElementsByTagName('body')[0].innerHTML);");
            }
        });
        crawlingWebview.loadUrl(htmlPageUrl);



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


                //Document doc = Jsoup.connect(htmlPageUrl).get();
                try {
                Document doc = Jsoup.parse(source);

                Elements elements = doc.select("li[id=item_index1]");
                String url = elements.attr("data-cr-url");

                Log.e("crawlingactivity", "urli: "+url);

                Elements elements1 = doc.select("li[id=item_index1] div div a");
                url1 = elements1.attr("data-api");

                Log.e("crawlingactivity", "url1: "+url1);


                Document doc1 = Jsoup.connect(url1).get();

                Elements elements2 = doc1.select("body");
                String url2 = elements2.text();
                Log.e("crawlingactivity", "url2: "+url2);

                String playUrl = url2.split("sPlayUrl\":\"")[1];
                playUrl1 = playUrl.split("\"")[0];

                    Log.e("crawlingactivity", "playUrl1: "+playUrl1);


                } catch (IOException e) {
                    e.printStackTrace();
                }


//                    //테스트1
//                    Elements titles= doc.select("div.news-con h1.tit-news");
//
//                    System.out.println("-------------------------------------------------------------");
//                    for(Element e: titles){
//                        System.out.println("title1: " + e.text());
//                        htmlContentInStringFormat += e.text().trim() +"\n";
//                    }
//
//                    //테스트2
//                    titles= doc.select("div.news-con h2.tit-news");
//
//                    System.out.println("-------------------------------------------------------------");
//                    for(Element e: titles){
//                        System.out.println("title2: " + e.text());
//                        htmlContentInStringFormat += e.text().trim() +"\n";
//                    }
//                    //basisElement
//
//
//
//                    //테스트3
//                    titles= doc.select("li.section02 div.con h2.news-tl");
//
//                    System.out.println("-------------------------------------------------------------");
//                    for(Element e: titles){
//                        System.out.println("title3: " + e.text());
//                        htmlContentInStringFormat += e.text().trim() +"\n";
//                    }
//                    System.out.println("-------------------------------------------------------------");
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                textviewHtmlDocument.setText(source);
                crawlingWebviewFinish.setVisibility(View.VISIBLE);
                crawlingWebviewFinish.loadUrl(playUrl1);

            }
        }

        public void setText(String result){
        textviewHtmlDocument.setText(result);
        }



}
