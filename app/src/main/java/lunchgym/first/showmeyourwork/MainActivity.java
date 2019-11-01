package lunchgym.first.showmeyourwork;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.ResourceExhaustedException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
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


    //==============================================================================================


    //파이어베이스 OCR 관련 변수====================================================================
    AlertDialog waitingDialog;
    Button btnCapture;
    Image imageCapture;



    //==============================================================================================

    //크롤링 관련 변수==============================================================================


    //==============================================================================================


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //파이어베이스 OCR==========================================================================
        //테스트(버그 픽스용)
        ivTest = findViewById(R.id.iv_test);



        //대기 다이어로그
        waitingDialog = new SpotsDialog.Builder()
                .setCancelable(false)
                .setMessage("잠시만 기달려주세요")
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

                //테스트용(확인해 봤는데 90도 돌아가 있네ㄷㄷ)
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
        texture = new ExternalTexture();
        mediaPlayer = new MediaPlayer();
//        mediaPlayer = MediaPlayer.create(this, Uri.parse("https://serviceapi.nmv.naver.com/view/ugcPlayer.nhn?vid=7DCA747C80C640305145C42E13B6329C4660&inKey=V1268b72f809c30d1ef02c87c44d03bf0878269db9d3b7e681252ba8028ec7566512ec87c44d03bf08782&wmode=opaque&hasLink=1&autoPlay=false&beginTime=0"));
//
//        mediaPlayer = MediaPlayer.create(this, R.raw.test_video);
        try {
//            mediaPlayer.setDataSource("https://serviceapi.nmv.naver.com/view/ugcPlayer.nhn?vid=7DCA747C80C640305145C42E13B6329C4660&inKey=V1268b72f809c30d1ef02c87c44d03bf0878269db9d3b7e681252ba8028ec7566512ec87c44d03bf08782&wmode=opaque&hasLink=1&autoPlay=false&beginTime=0");
            mediaPlayer.setDataSource("https://media.fmkorea.com/files/attach/new/20191101/486263/1651469947/2337202584/508a93d482fc2fcec3d50429dfc17cbc.gif.mp4?d\n");
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
                        Log.w(TAG, "OCR -  onSuccess: "+firebaseVisionText.getText() );
                        //인덱스 들어가니깐 아마 예외처리 해줘야할듯
                        Toast.makeText(getApplicationContext()  , firebaseVisionText.getTextBlocks().get(0).toString(), Toast.LENGTH_LONG).show();

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

        if (isImageDetected)
            return;


        //여기서 ar 프래그먼트에서 프레임을 받아오네..
        Frame frame = arFragment.getArSceneView().getArFrame();

        Collection<AugmentedImage> augmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);


        for (AugmentedImage image : augmentedImages) {

            if (image.getTrackingState() == TrackingState.TRACKING) {

                if (image.getName().equals("image")) {

                    isImageDetected = true;

                    playVideo(image.createAnchor(image.getCenterPose()), image.getExtentX(),
                            image.getExtentZ());

                    break;
                }

            }

        }

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

    //==============================================================================================


}

