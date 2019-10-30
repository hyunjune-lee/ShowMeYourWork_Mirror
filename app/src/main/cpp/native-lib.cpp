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

// comparison function object
bool compareContourAreas ( std::vector<cv::Point> contour1, std::vector<cv::Point> contour2 ) {
    double i = fabs( contourArea(cv::Mat(contour1)) );
    double j = fabs( contourArea(cv::Mat(contour2)) );
    return ( i < j );
}



extern "C"
JNIEXPORT void JNICALL
Java_lunchgym_first_showmeyourwork_MainActivity_FindMemberNameInPaper(JNIEnv *env, jobject thiz,
                                                                 jlong matAddrInput,
                                                                 jlong matAddrResult) {
    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;
    matResult = matInput.clone();

    //이미지 라시이징

    //엣지 검출하는 매트릭스(캐니 알고리즘으로)
    Mat matEdgeCanny = matInput.clone();
    //연산을 줄이기 위해서 그레이로 전환
    cvtColor(matInput, matEdgeCanny, COLOR_RGBA2GRAY);

    //블러적용 블러 한 개 고르기--------------------------------------------------------------------
/*    //블러 적용(노이즈 감소)
    blur(matEdgeCanny, matEdgeCanny, Size(3, 3));*/

    //가우시안 블러
    GaussianBlur(matEdgeCanny, matEdgeCanny, Size(3,3), 0 );
    //----------------------------------------------------------------------------------------------

    //엣지 검출 - 캐니 알고리즘 적용
    //여기서 3번째 4번째가 최소 쓰레쉬홀드, 최대 쓰레쉬 홀드이다
    //이 값을 적절히 조절해서 엣지를 잘 검출하는 것도 개발자의 몫
    Canny(matEdgeCanny, matEdgeCanny, 100,200, 3);




    //컨투어 그리는 첫번째--------------------------------------------------------------------------
    /*
    //by 웹 나우테스
    //contour를 찾는다.
    vector<vector<Point> > contours;
    //RETR_TREE 는 배열을 트리관계로 리턴, RETR_LIST 계층관계 고려X
    // 바깥쪽 컨투어만 리턴 받고 싶으면 익스터널?
    //CHAIN_APPROX_SIMPLE
    //이 패러미터는 컨투어 정보를 꼭지점만 반환할지, 모든좌표를 반환할지 설정하는것
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

    }*/
    //----------------------------------------------------------------------------------------------

    //도형 외곽 추출=============================================================================================
    //컨투어를 그리는 두번째 방법-------------------------------------------------------------------

    //contour를 찾는다.
    vector<vector<Point> > contours;
    //RETR_TREE 는 배열을 트리관계로 리턴, RETR_LIST 계층관계 고려X
    // 바깥쪽 컨투어만 리턴 받고 싶으면 익스터널?
    //CHAIN_APPROX_SIMPLE
    //이 패러미터는 컨투어 정보를 꼭지점만 반환할지, 모든좌표를 반환할지 설정하는것
    findContours(matEdgeCanny, contours, RETR_LIST, CHAIN_APPROX_SIMPLE);

    //컨투어를 외곽이 큰 1개 받아오기
    // sort contours
    std::sort(contours.begin(), contours.end(), compareContourAreas);
    std::vector<cv::Point> biggestContour = contours[contours.size()-1];
    std::vector<cv::Point> smallestContour = contours[0];


    //----------------------------------------------------------------------------------------------






    //도형을 근사해서 Overfitting 방지--------------------------------------------------------------
    //아무것도 검출되지 않으면 앱 에러뜸 아마 contours[0]이 없어서 그런듯
    //안해도 각오문이 워낙 잘 되어있어서 괜찮을듯
    //벡터 아니면 벡터<포인트>인거 같은데
    vector<Point> cnt = smallestContour;
    //둘레 길이의 10%
    double epsilon = 0.1 * arcLength(cnt, true);
/*    vector<Point2f> approx;
    //epsilon 만큼을 최대로 해서 꼭지점을 줄여나간다
    //epsilon 너무 크면
    //true 폐곡선인 개곡선인
    approxPolyDP(cnt, approx, epsilon, true);
    // approxPolyDP(Mat(contours[i]), approx, arcLength(Mat(contours[i]), true)*0.02, true);*/


    //----------------------------------------------------------------------------------------------

    //drawContours----------------------------------------------------------------------------------
    /*
    //3번째 패러미터는 컨투어를 그릴 인덱스 패러미터
    // => 컨투어가 여러개가 있는데
    //모두 그리고 싶으면 -1
    //특정 컨투어만 그리고 싶으면 그 컨투어의 인덱스를 넘어주면 됨
    //컨투어가 담긴 순서는 알고리즘이 탐색한 순서
    //근데 이게 내가 의도한 순서와 탐색했는지 몰라(변칙적임)
    //그래서 내가 원하는 순서대로 정렬해야됨
    //ex 가장 큰 컨투어 순서대로 정렬을 한다
    //여기서 0번 인덱스를 본다 하면 가장큰 인덱스를 보는것
    //4번째는 RGB 패러미터
    //지금 G(초록)가 255 이다
    //5번째 패러미터는 컨투어를 그릴 선의 두께
    */
    drawContours(matInput, contours, -1, Scalar(0,255,0), 2);

    //----------------------------------------------------------------------------------------------






    //이미지를 잘게 쪼개서 각 부분에서 쓰레쉬 홀드를 구해서 이진화 하기-----------------------------
    //리사이징을 해야될까?(현재 리사이징 생략됨)

    adaptiveThreshold(matEdgeCanny,matEdgeCanny,255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 21,10);




    //----------------------------------------------------------------------------------------------

    //==================================================================================================

    matResult= matEdgeCanny;

}


