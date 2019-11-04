package lunchgym.first.showmeyourwork;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.ResourceExhaustedException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import dmax.dialog.SpotsDialog;


/*public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {*/

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "opencv";

    //Crawling 관련 변수 ==========================================================================

    private String htmlPageUrl ="https://search.naver.com/search.naver?where=video&sm=tab_jum&query="; //파싱할 홈페이지의 URL주소
    private String cardinal;
    private String name;
    private WebView fakeWb;
    private String source = "";
    private String videoSrc;
    private String playUrl = "";
    private JsoupAsyncTask jsoupAsyncTask;
    private WebviewAsyncTask webViewAsyncTask;



    //OpenCV 관련 변수===============================================================================
    private Mat matInput;
    private Mat matResult;

    private CameraBridgeViewBase mOpenCvCameraView;

    public native void FindMemberNameInPaper(long matAddrInput, long matAddrResult);


    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }


    //이미지 테스용
    private ImageView ivTest;

    //==============================================================================================

    //AR 관련 변수==================================================================================
    private ExternalTexture texture;
    private MediaPlayer mediaPlayer;
    private CustomArFragment arFragment;
    private Scene scene;
    private ModelRenderable renderable;
    private boolean isImageDetected = false;


    // Controls the height of the video in world space.
    private static final float VIDEO_HEIGHT_METERS = 0.85f;

    //==============================================================================================


    //파이어베이스 OCR 관련 변수====================================================================
    AlertDialog waitingDialog;
    Button btnCapture;
    Image imageCapture;

    //회전용 매트릭스
    Matrix rotatedMatrix;



    //==============================================================================================

    //크롤링 관련 변수==============================================================================


    //==============================================================================================


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //crawling 하기 전 fake webview 설정==========================================================================
        fakeWb = findViewById(R.id.wb_fake);
        fakeWb.setVisibility(View.INVISIBLE);

        //Webview 자바스크립트 활성화
        fakeWb.getSettings().setJavaScriptEnabled(true);
        //자바스크립트 인터페이스 연결
        fakeWb.addJavascriptInterface(new MyJavascriptInterface(), "Android");
        fakeWb.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                //자바스크립트 인터페이스로 연결되어있는 getHtml을 실행
                //자바스크립트 기본 메소드로 html소스를 통째로 지정해서 인자로 넘겨줌
                view.loadUrl("javascript:window.Android.getHtml(document.getElementsByTagName('body')[0].innerHTML);");
            }
        });






        //파이어베이스 OCR==========================================================================
        //테스트(버그 픽스용)
        ivTest = findViewById(R.id.iv_test);



        //대기 다이어로그
        waitingDialog = new SpotsDialog.Builder()
                .setCancelable(false)
                .setMessage("잠시만 기다려주세요")
                .setContext(this)
                .build();

        //캡처버튼
        btnCapture = findViewById(R.id.btn_capture);
        btnCapture.setOnClickListener(v -> {

            try {
                // ar fragment 에서 이미지 가져오기
                // 후에 opencv 와 연동하면 opencv 에서 데이터 받는것도 ar fragment 에서 받아와야할지도
                imageCapture = Objects.requireNonNull(arFragment.getArSceneView().getArFrame()).acquireCameraImage();


                //대기 다이어로그 띄어주기
                waitingDialog.show();

                //바이트 배열을 얻는다(이미지를 비트맵으로 바꾸기위해서 필요)
                byte[] byteArray = imageToByte(imageCapture);

                //얻은 비트맵
                Bitmap bitmapCapture = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, null);

                //90도 돌아가있어서 다시 세로로 돌려주기
                //안 돌려줘도 글자 인식하는데 세로로 세워주면 인식률 올라가지 않을까해서 돌림)
                //확인해봤는데 인식률 많이 올라감 굳굳
                rotatedMatrix = new Matrix();
                rotatedMatrix.postRotate(90);
                bitmapCapture = Bitmap.createBitmap(bitmapCapture, 0, 0, bitmapCapture.getWidth(), bitmapCapture.getHeight(), rotatedMatrix, true);


                ivTest.setImageBitmap(bitmapCapture);

                //파이어베이스 문자 인식
                recognizeText(bitmapCapture);

            } catch (NotYetAvailableException e  ) {
                e.printStackTrace();
                Log.w(TAG, "OCR - onCreate: btnCapture NotYetAvailableException ");
            }
            catch (ResourceExhaustedException e ){
                e.printStackTrace();
                Log.w(TAG, "OCR - onCreate: btnCapture ResourceExhaustedException ");


            }







        });

        //AR========================================================================================
        // Create an ExternalTexture for displaying the contents of the video.
        texture = new ExternalTexture();
        mediaPlayer = new MediaPlayer();
//        mediaPlayer = MediaPlayer.create(this, Uri.parse("https://serviceapi.nmv.naver.com/view/ugcPlayer.nhn?vid=7DCA747C80C640305145C42E13B6329C4660&inKey=V1268b72f809c30d1ef02c87c44d03bf0878269db9d3b7e681252ba8028ec7566512ec87c44d03bf08782&wmode=opaque&hasLink=1&autoPlay=false&beginTime=0"));
//
//        mediaPlayer = MediaPlayer.create(this, R.raw.test_video);
        try {


            mediaPlayer.setDataSource("https://media.fmkorea.com/files/attach/new/20191101/3655109/2089104173/2339677357/1cb879a979e99e75f598d2e9038bfa4e.gif.mp4?d");

            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Log.w(TAG, "mediaPlayer.setDataSource: fail" );
    }



        mediaPlayer.setSurface(texture.getSurface());
        mediaPlayer.setLooping(true);

        ModelRenderable
                .builder()
                .setSource(this, Uri.parse("video_screen.sfb"))
                .build()
                .thenAccept(modelRenderable -> {
                    modelRenderable.getMaterial().setExternalTexture("videoTexture",
                            texture);
                    modelRenderable.getMaterial().setFloat4("keyColor",
                            new Color(0.01843f, 1f, 0.098f));

                    renderable = modelRenderable;
                });



        arFragment = (CustomArFragment)
                getSupportFragmentManager().findFragmentById(R.id.ar_fragment);

        scene = arFragment.getArSceneView().getScene();

        scene.addOnUpdateListener(this::onUpdate);




        //평면 터치해서 AR 비디오 띄어주기----------------------------------------------------------
        //*참고 chromakey video
        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (renderable == null) {
                        return;
                    }

                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    // Create a node to render the video and add it to the anchor.
                    Node videoNode = new Node();
                    videoNode.setParent(anchorNode);

                    // Set the scale of the node so that the aspect ratio of the video is correct.
                    float videoWidth = mediaPlayer.getVideoWidth();
                    float videoHeight = mediaPlayer.getVideoHeight();
                    videoNode.setLocalScale(
                            new Vector3(
                                    VIDEO_HEIGHT_METERS * (videoWidth / videoHeight), VIDEO_HEIGHT_METERS, 1.0f));

                    Toast.makeText(getApplicationContext()  , "videoWidth " + videoWidth + "\n videoHeight" +videoHeight, Toast.LENGTH_LONG).show();



                    // Start playing the video when the first node is placed.
                    if (!mediaPlayer.isPlaying()) {
                        mediaPlayer.start();

                        // Wait to set the renderable until the first frame of the  video becomes available.
                        // This prevents the renderable from briefly appearing as a black quad before the video
                        // plays.
                        texture
                                .getSurfaceTexture()
                                .setOnFrameAvailableListener(
                                        (SurfaceTexture surfaceTexture) -> {
                                            videoNode.setRenderable(renderable);
                                            texture.getSurfaceTexture().setOnFrameAvailableListener(null);
                                        });
                    } else {
                        videoNode.setRenderable(renderable);
                    }
                });








        //------------------------------------------------------------------------------------------







        //==========================================================================================

        //OpenCV====================================================================================

        /*getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)*/
        //===========================================================================================

    }


    //파이어베이스 OCR==============================================================================

    //파이어베이스로 찍은 비트맵의 문자 인식
    private void recognizeText(Bitmap bitmapCapture) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmapCapture);

        //힌트 랭귀지 => 한국인데.. (ko 값들이 많아서 어떻게 적절한건지 모르겠음)
        FirebaseVisionCloudTextRecognizerOptions options =
                new FirebaseVisionCloudTextRecognizerOptions.Builder().setLanguageHints(Arrays.asList("ko"))
                .build();

        FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance()
                .getCloudTextRecognizer(options);

        textRecognizer.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
//                        Log.w(TAG, "OCR -  onSuccess: "+firebaseVisionText.getText() );

                        Log.w(TAG, "OCR -  onSuccess: "+firebaseVisionText.getTextBlocks().get(0).getText() );

                        try{
                            String inputText = firebaseVisionText.getTextBlocks().get(0).getText();

                            if(inputText.length()>0){
                                cardinal = inputText.substring(0, 1);
                                name = inputText.substring(3,6);
                            }
                            Toast.makeText(MainActivity.this, cardinal+"기 "+name, Toast.LENGTH_SHORT).show();
                            htmlPageUrl = htmlPageUrl+cardinal+"기%20"+name;

//                            Intent intent = new Intent(MainActivity.this, CrawlingActivity.class);
//                            intent.putExtra("htmlPageUrl", htmlPageUrl);
//                            startActivity(intent);

                            fakeWb.loadUrl(htmlPageUrl);

                            webViewAsyncTask = new WebviewAsyncTask();
                            webViewAsyncTask.execute();

                        }catch(IndexOutOfBoundsException | IllegalArgumentException e){
                            Toast.makeText(MainActivity.this, "사진을 다시 찍어주세요.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "OCR - onSuccess 에러내용 : "+e );
                        }

                        //인덱스 들어가니깐 아마 예외처리 해줘야할듯

                        Log.w(TAG, "OCR -  onSuccess: "+firebaseVisionText.getTextBlocks().get(0).getText() );

                        //이미지 acquired 해제해주기
                        //이거 안해주면 5번째찍을때부터 ResourceExhaustedException 뜸
                        imageCapture.close();

                        //대기 다이어로그 없애주기
                        waitingDialog.dismiss();


                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "onFailure: 파이어베이스 텍스트 인식 실패");
                //대기 다이어로그 없애주기
                waitingDialog.dismiss();
            }


        });


    }

    //==============================================================================================




    //AR============================================================================================
    private void onUpdate(FrameTime frameTime) {
        //여기서 ar 프래그먼트에서 프레임을 받아오네..
        Frame frame = arFragment.getArSceneView().getArFrame();


 /*       if (isImageDetected)
            return;




        Collection<AugmentedImage> augmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);


        for (AugmentedImage image : augmentedImages) {

            if (image.getTrackingState() == TrackingState.TRACKING) {

                if (image.getName().equals("airImage") || image.getName().equals("papers")
                ) {

                    isImageDetected = true;

                    playVideo(image.createAnchor(image.getCenterPose()), image.getExtentX(),
                            image.getExtentZ());

                    break;
                }

            }

        }*/

    }

    private void playVideo(Anchor anchor, float extentX, float extentZ) {

        mediaPlayer.start();

        AnchorNode anchorNode = new AnchorNode(anchor);

        texture.getSurfaceTexture().setOnFrameAvailableListener(surfaceTexture -> {
            anchorNode.setRenderable(renderable);
            texture.getSurfaceTexture().setOnFrameAvailableListener(null);
        });

        anchorNode.setWorldScale(new Vector3(extentX, 1f, extentZ));

        scene.addChild(anchorNode);

    }

    //==============================================================================================


    @Override
    protected void onStart() {
        super.onStart();

        //OpenCV====================================================================================
        /*boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                havePermission = false;
            }
        }
        if (havePermission) {
            onCameraPermissionGranted();
        }*/
        //===========================================================================================
    }


    @Override
    public void onPause() {
        super.onPause();
        //OpenCV====================================================================================
        /*if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();*/
        //===========================================================================================

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        htmlPageUrl ="https://search.naver.com/search.naver?where=video&sm=tab_jum&query=";

        //OpenCV====================================================================================
        /*if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }*/
        //===========================================================================================

    }


    public void onDestroy() {
        super.onDestroy();

        //OpenCV====================================================================================
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        //===========================================================================================

    }


    //OpenCV 관련 메소드============================================================================
    /*private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        matInput = inputFrame.rgba();
        if ( matResult == null )
            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());
        FindMemberNameInPaper(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
        return matResult;
    }


    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }


    //여기서부턴 퍼미션 관련 메소드
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;


    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
    }



    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted();
        }else{
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {


        AlertDialog.Builder builder = new AlertDialog.Builder( MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }*/
    //==============================================================================================


    //Image to Bitmap===============================================================================
    //(ar fragment 에서 비트맵 얻어서 파이어베이스 문자 인식하려고)
    private static byte[] imageToByte(Image image){
        byte[] byteArray = null;
        byteArray = NV21toJPEG(YUV420toNV21(image),image.getWidth(),image.getHeight(),100);
        return byteArray;
    }

    private static byte[] NV21toJPEG(byte[] nv21, int width, int height, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), quality, out);
        return out.toByteArray();
    }

    private static byte[] YUV420toNV21(Image image) {
        byte[] nv21;
        // Get the three planes.
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();


        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }

    //crawling 관련 class==============================================================================================
    public class MyJavascriptInterface {
        @JavascriptInterface
        public void getHtml(String html){
            //위 자바스크립트가 호출되면 여기로 html이 반환된다.
            source = html;
        }
    }

    private class WebviewAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            jsoupAsyncTask = new JsoupAsyncTask();
            jsoupAsyncTask.execute();
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
                //가짜 webview에서 받아온 String source를 Document로 파싱한다.
                Document doc = Jsoup.parse(source);
                Log.e("crawling", "source: "+ source);

                Elements urlElements = doc.select("li[id=item_index1]");
                //linkUrl을 가져온다.
                String linkUrl = urlElements.attr("data-cr-url");
                Log.e("crawling", "linkUrl: "+linkUrl);


                String novaLinkUrl = linkUrl.split("/")[3];
                Log.e("crawling", "novaLinkUrl : "+novaLinkUrl);

                if(novaLinkUrl.equals("teamnovaopen")){
                    //video의 source를 가져온다.
                    Elements videoSrcElements = doc.select("li[id=item_index1] div div a");
                    videoSrc = videoSrcElements.attr("data-api");
                    Log.e("crawling", "videoSrc: "+ videoSrc);

                    Document forVideo = Jsoup.connect(videoSrc).get();

                    Elements videoUrlElements = forVideo.select("body");
                    String videoUrl = videoUrlElements.text();
                    Log.e("crawling", "videoUrl: "+videoUrl);

                    String splitVideoUrl = videoUrl.split("sPlayUrl\":\"")[1];
                    playUrl = splitVideoUrl.split("\"")[0];

                    Log.e("crawling", "playUrl: "+ playUrl);
                }else{

                    playUrl="https://media.fmkorea.com/files/attach/new/20191101/3655109/2089104173/2339677357/1cb879a979e99e75f598d2e9038bfa4e.gif.mp4?d";
                }


            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Toast.makeText(MainActivity.this, "동영상 불러오기 완료", Toast.LENGTH_SHORT).show();
            htmlPageUrl ="https://search.naver.com/search.naver?where=video&sm=tab_jum&query=";

            setAR(playUrl);
        }
    }

    public void setAR(String url){
        texture = new ExternalTexture();
        mediaPlayer = new MediaPlayer();
//        mediaPlayer = MediaPlayer.create(this, Uri.parse("https://serviceapi.nmv.naver.com/view/ugcPlayer.nhn?vid=7DCA747C80C640305145C42E13B6329C4660&inKey=V1268b72f809c30d1ef02c87c44d03bf0878269db9d3b7e681252ba8028ec7566512ec87c44d03bf08782&wmode=opaque&hasLink=1&autoPlay=false&beginTime=0"));
//
//        mediaPlayer = MediaPlayer.create(this, R.raw.test_video);
        try {
//            mediaPlayer.setDataSource("https://serviceapi.nmv.naver.com/view/ugcPlayer.nhn?vid=7DCA747C80C640305145C42E13B6329C4660&inKey=V1268b72f809c30d1ef02c87c44d03bf0878269db9d3b7e681252ba8028ec7566512ec87c44d03bf08782&wmode=opaque&hasLink=1&autoPlay=false&beginTime=0");

            if(url.equals("https://media.fmkorea.com/files/attach/new/20191101/3655109/2089104173/2339677357/1cb879a979e99e75f598d2e9038bfa4e.gif.mp4?d")){
                Toast.makeText(MainActivity.this, "동영상 검색 결과가 없습니다ㅠ", Toast.LENGTH_SHORT).show();
            }
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Log.w(TAG, "mediaPlayer.setDataSource: fail" );
        }

        mediaPlayer.setSurface(texture.getSurface());
        mediaPlayer.setLooping(true);

        ModelRenderable
                .builder()
                .setSource(this, Uri.parse("video_screen.sfb"))
                .build()
                .thenAccept(modelRenderable -> {
                    modelRenderable.getMaterial().setExternalTexture("videoTexture",
                            texture);
                    modelRenderable.getMaterial().setFloat4("keyColor",
                            new Color(0.01843f, 1f, 0.098f));

                    renderable = modelRenderable;
                });

        arFragment = (CustomArFragment)
                getSupportFragmentManager().findFragmentById(R.id.ar_fragment);

        scene = arFragment.getArSceneView().getScene();

        scene.addOnUpdateListener(this::onUpdate);
    }


}

