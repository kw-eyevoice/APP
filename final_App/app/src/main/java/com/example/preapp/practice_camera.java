package com.example.preapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class practice_camera extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    Boolean startYolo=false;
    boolean firstTimeYolo=false;
    Net tinyYolo;
    TextView text1;
    ImageView hand,circle;
    String id;
    ImageButton imageButton;
//    String colorid;
//    Intent colorintent = new Intent(getApplicationContext(), consonant.class);


    private static String getPath(String file, Context context){
        AssetManager assetManager =context.getAssets();
        BufferedInputStream inputStream=null;
        try {
            inputStream=new BufferedInputStream(assetManager.open(file));
            byte[] data=new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            File outFile=new File(context.getFilesDir(),file);
            FileOutputStream os=new FileOutputStream(outFile);
            os.write(data);
            os.close();
            return outFile.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

//
//    public void YOLO(View Button){
//
//        if (startYolo == false){
//
//            startYolo = true;
//
//            if (firstTimeYolo == false){
//
//                firstTimeYolo = true;
//                String tinyYoloCfg = getPath("yolov3-tiny.cfg",this);
//                String tinyYoloWeights = getPath("yolov3-tiny_64000.weights",this);
//
//                tinyYolo = Dnn.readNetFromDarknet(tinyYoloCfg, tinyYoloWeights);
//
//            }
//
//        }
//
//        else{
//
//            startYolo = false;
//        }
//
//    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.practice_camera);

        byte[] byteArray = getIntent().getByteArrayExtra("image");
        Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        imageButton = (ImageButton) findViewById(R.id.imageButton);

        ImageView ivImage = findViewById(R.id.imageView1);
        ivImage.setImageBitmap(image);

        Intent intent = getIntent();
        id=intent.getExtras().getString("id");

        circle=findViewById(R.id.circle);

        cameraBridgeViewBase=(JavaCameraView)findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.setCameraIndex(1);  // 셀카모드



        //system.loadLibrary(Core.NATIVE_LIBRARY_NAME)
        baseLoaderCallback=new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);

                switch (status){

                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };


        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backbutton();
            }
        });

        Toast.makeText(getApplicationContext(), "혼잡한 배경은 피해주시고, 오른손으로 인식해주세요.",Toast.LENGTH_SHORT).show();

    }



    //가장 중요한 함수, 여기서 캡쳐하거나 다른 이미지를 삽입하거나 rgb 바꾸거나 등등 수행(여러 트리거를 줄 수 있음)
    //Mat을 활용하여 이미지를 파이썬의 매트릭스 배열처럼 저장할 수 있다
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat frame = inputFrame.rgba();//프레임 받기기

        if (startYolo==true) {
            //Imgproc을 이용해 이미지 프로세싱을 한다.
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);//rgba 체계를 rgb로 변경

            //Imgproc.Canny(frame, frame, 100, 200);
            //Mat gray=Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY)
            Mat imageBlob = Dnn.blobFromImage(frame, 0.00392, new Size(288, 288), new Scalar(0, 0, 0),/*swapRB*/false, /*crop*/false);
            //뉴런 네트워크에 이미지 넣기

            tinyYolo.setInput(imageBlob);

            //cfg 파일에서 yolo layer number을 확인하여 이를 순전파에 넣어준다.
            //yolv3-tiny는 yolo layer가 2개라서 initialCapacity를 2로 준다.
            java.util.List<Mat> result = new java.util.ArrayList<Mat>(2);

            List<String> outBlobNames = new java.util.ArrayList<>();
            outBlobNames.add(0, "yolo_16");//확인필요
            outBlobNames.add(1, "yolo_23");

            //순전파를 진행
            tinyYolo.forward(result, outBlobNames);

            //30%이상의 확률만 출력해준다.
            float confThreshold = 0.3f;

            //class id
            List<Integer> clsIds = new ArrayList<>();
            //
            List<Float> confs = new ArrayList<>();
            //draw rectanglelist
            List<Rect> rects = new ArrayList<>();

///////////////////////////////////////////////////////////////////////
            //int i=0
//            smile=(ImageView) findViewById(R.id.ans);
//            hand=(ImageView) findViewById(R.id.imageView1);
            //            if clsid=사진아이디하고 같으면 스마일 sad로 변환
//            i=1
//             if(i==0){smile.setImageResource(R.drawable.smile);
//             else{smile.setImageResource(R.drawable.sad);

///////////////////////////////////////////////////////////////////////
            for (int i = 0; i < result.size(); ++i) {

                Mat level = result.get(i);

                for (int j = 0; j < level.rows(); ++j) { //iterate row
                    Mat row = level.row(j);
                    Mat scores = row.colRange(5, level.cols());
                    Core.MinMaxLocResult mm = Core.minMaxLoc(scores);


                    float confidence = (float) mm.maxVal;


                    ///////////////////////////////////////////
                    //confidence그 버튼 선택한거 하고 같으면 동작 처리

                    //여러개의 클래스들 중에 가장 정확도가 높은(유사한) 클래스 아이디를 찾아낸다.
                    Point classIdPoint = mm.maxLoc;


                    if (confidence > confThreshold) {
                        int centerX = (int) (row.get(0, 0)[0] * frame.cols());
                        int centerY = (int) (row.get(0, 1)[0] * frame.rows());
                        int width = (int) (row.get(0, 2)[0] * frame.cols());
                        int height = (int) (row.get(0, 3)[0] * frame.rows());

                        int left = (int)(centerX - width * 0.5);
                        int top = (int) (centerY - height * 0.5);

                        clsIds.add((int) classIdPoint.x);
                        confs.add((float) confidence);


                        rects.add(new Rect(left, top, width, height));
                    }
                }
            }
            int ArrayLength = confs.size();

            if (ArrayLength >= 1) {
                // Apply non-maximum suppression procedure.
                float nmsThresh = 0.2f;


                MatOfFloat confidences = new MatOfFloat(Converters.vector_float_to_Mat(confs));


                Rect[] boxesArray = rects.toArray(new Rect[0]);

                MatOfRect boxes = new MatOfRect(boxesArray);

                MatOfInt indices = new MatOfInt();


                Dnn.NMSBoxes(boxes, confidences, confThreshold, nmsThresh, indices);


                // Draw result boxes:
                int[] ind = indices.toArray();
                for (int i = 0; i < ind.length; ++i) {

                    int idx = ind[i];
                    Rect box = boxesArray[idx];

                    int idGuy = clsIds.get(idx);

                    float conf = confs.get(idx);

                    List<String> cocoNames = Arrays.asList("r","s" , "e" , "f" , "a" , "q" , "t" , "d" ,
                            "w" , "c" , "z" , "x" , "v" , "g" , "k" , "i" , "j" , "u" ,
                            "h" , "y" , "n" , "b" , "m" , "l", "o", "p", "hl", "nl",
                            "oo", "pp", "ml", "delete", "space");

                    List<String> hangul = Arrays.asList("ㄱ","ㄴ" , "ㄷ" , "ㄹ" , "ㅁ" , "ㅂ" , "ㅅ" , "ㅇ" ,
                            "ㅈ" , "ㅊ" , "ㅋ" , "ㅌ" , "ㅍ" , "ㅎ" , "ㅏ" , "ㅑ" , "ㅓ" , "ㅕ" ,
                            "ㅗ" , "ㅛ" , "ㅜ" , "ㅠ" , "ㅡ" , "ㅣ", "ㅐ", "ㅔ", "ㅚ", "ㅟ",
                            "ㅒ", "ㅖ", "ㅢ", "delete", "space");
                    int intConf = (int) (conf * 100);

                    Imgproc.putText(frame, cocoNames.get(idGuy) + " " + intConf + "%", box.tl(), Core.FONT_HERSHEY_SIMPLEX, 2, new Scalar(255, 255, 0), 2);

                    Imgproc.rectangle(frame, box.tl(), box.br(), new Scalar(255, 0, 0), 2);

                    if(cocoNames.get(idGuy).equals(id))
                    {
                        circle.setColorFilter(Color.parseColor("#EE0000"), PorterDuff.Mode.SRC_IN);

//                        colorintent.putExtra(colorid, "1");
//                        setContentView(R.layout.consonant);
//                        Button consonantBtn=(Button) findViewById(imgs[idGuy]);


                    }

                }
            }
        }

        return frame; //프레임 리턴
    }


    @Override
    //카메라뷰 시작될때
    public void onCameraViewStarted(int width, int height) {
        if (startYolo=true){
            String tinyYoloCfg = getPath("yolov3-tiny.cfg",this);
            String tinyYoloWeights = getPath("yolov3-tiny_64000.weights",this);

            tinyYolo = Dnn.readNetFromDarknet(tinyYoloCfg, tinyYoloWeights);
        }

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"There's a problem", Toast.LENGTH_SHORT).show();
        }

        else
        {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }

    }

    @Override
    protected void onPause() {
        //카메라뷰 중지
        super.onPause();
        if (cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        //카메라뷰 종료
        super.onDestroy();
        if (cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }

    void backbutton()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(practice_camera.this);
        builder.setTitle("뒤로 가기");
        builder.setMessage("뒤로 가시겠습니까?");
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(getApplicationContext(), practice1.class);
                finish();
                startActivity(intent);
            }
        });
        builder.setNegativeButton("아니오", null);
        builder.create().show();

    }

}






//아이콘 xml에서 사진 추가
// 카메라 java에서 코드 짜서 진행 후 xml 코드 지우기
// 카메라 밑에 통과 여부 표시 카메라 설치 후 진행