#include <jni.h>
#include <opencv2/opencv.hpp>

using namespace cv;



extern "C"
JNIEXPORT void JNICALL
Java_lunchgym_first_showmeyourwork_CameraActivity_ConvertRGBtoGray(JNIEnv *env, jobject thiz,
                                                                 jlong matAddrInput,
                                                                 jlong matAddrResult) {
    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;
    cvtColor(matInput, matResult, COLOR_RGBA2GRAY);


}