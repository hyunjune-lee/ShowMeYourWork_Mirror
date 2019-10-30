#include <jni.h>
#include <opencv2/opencv.hpp>
#include <iostream>
#include <string>


using namespace cv;



using namespace std;

//Contour 영역 내에 텍스트 쓰기
//https://github.com/bsdnoobz/opencv-code/blob/master/shape-detect.cpp
void setLabel(Mat& image, string str, vector<Point> contour)
{
    int fontface = FONT_HERSHEY_SIMPLEX;
    double scale = 0.5;
    int thickness = 1;
    int baseline = 0;

    Size text = getTextSize(str, fontface, scale, thickness, &baseline);
    Rect r = boundingRect(contour);

    Point pt(r.x + ((r.width - text.width) / 2), r.y + ((r.height + text.height) / 2));
    rectangle(image, pt + Point(0, baseline), pt + Point(text.width, -text.height), CV_RGB(200, 200, 200), FILLED);
    putText(image, str, pt, fontface, scale, CV_RGB(0, 0, 0), thickness, 8);
}




extern "C"
JNIEXPORT void JNICALL
Java_lunchgym_first_showmeyourwork_MainActivity_FindMemberNameInPaper(JNIEnv *env, jobject thiz,
                                                                 jlong matAddrInput,
                                                                 jlong matAddrResult) {
    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;
    matResult = matInput.clone();


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

    //contour를 찾는다.
    vector<vector<Point> > contours;
    findContours(matEdgeCanny, contours, RETR_LIST, CHAIN_APPROX_SIMPLE);

    //contour를 근사화한다.
    vector<Point2f> approx;

    for (size_t i = 0; i < contours.size(); i++)
    {
        approxPolyDP(Mat(contours[i]), approx, arcLength(Mat(contours[i]), true)*0.02, true);

        if (fabs(contourArea(Mat(approx))) > 1000 && fabs(contourArea(Mat(approx)))< 10000)  // <-----------------------   parameter 3
        {

            int size = approx.size();
            const char *name[]= { "none", "none", "none", "triangle", "quadrangle", "pentagon", "hexagon", "heptagon", "octagon", "nonagon", "decagon"  };

            switch (size){

                case 3: case 4: case 5:
                case 6: case 10:
                    if (isContourConvex(Mat(approx))) { // convex 인지 검사


                        //Contour를 근사화한 직선을 그린다.
                        if (size % 2 == 0) {
                            line(matEdgeCanny, approx[0], approx[approx.size() - 1], Scalar(0, 255, 0), 3);

                            for (int k = 0; k < size - 1; k++)
                                line(matEdgeCanny, approx[k], approx[k + 1], Scalar(0, 255, 0), 3);

                            for (int k = 0; k < size; k++)
                                circle(matEdgeCanny, approx[k], 3, Scalar(0, 0, 255));
                        }
                        else {
                            line(matEdgeCanny, approx[0], approx[approx.size() - 1], Scalar(0, 255, 0), 3);

                            for (int k = 0; k < size - 1; k++)
                                line(matEdgeCanny, approx[k], approx[k + 1], Scalar(0, 255, 0), 3);

                            for (int k = 0; k < size; k++)
                                circle(matEdgeCanny, approx[k], 3, Scalar(0, 0, 255));
                        }


                        //검출된 도형에 대한 라벨을 출력한다.

                        setLabel(matEdgeCanny, name[size], contours[i]);
                    }
                    break;
                deafult:
                    break;
            }
        }

    }

    matResult= matEdgeCanny;




}


