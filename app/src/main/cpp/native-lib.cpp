#include <jni.h>
#include <opencv2/opencv.hpp>

using namespace cv;



extern "C"
JNIEXPORT void JNICALL
Java_lunchgym_first_showmeyourwork_MainActivity_ConvertRGBtoGray(JNIEnv *env, jobject thiz,
                                                                 jlong matAddrInput,
                                                                 jlong matAddrResult) {
    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;


    //엣지 검출하는 매트릭스(캐니 알고리즘으로)
    Mat matEdgeCanny = matInput.clone();
    //연산을 줄이기 위해서 그레이로 전환
    cvtColor(matInput, matEdgeCanny, COLOR_RGBA2GRAY);

    //블러 적용(노이즈 감소)
    blur(matEdgeCanny, matEdgeCanny, Size(3, 3));

    //캐니 알고리즘 적용
    //여기서 3번째 4번째가 최소 쓰레쉬홀드, 최대 쓰레쉬 홀드이다
    //이 값을 적절히 조절해서 엣지를 잘 검출하는 것도 개발자의 몫
    Canny(matEdgeCanny, matEdgeCanny, 100,200, 3);



    matResult= matEdgeCanny;


}