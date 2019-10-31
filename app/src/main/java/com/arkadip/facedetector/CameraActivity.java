package com.arkadip.facedetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Used for logging success or failure messages
    private static final String TAG = "OCVSample::Activity";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0,255,0);

    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;

    // Mat for Image processing
    Mat mRgba;
    Mat mGray;

    // Haar Cascade Classifier
    private CascadeClassifier mJavaDetector;

    // Absolute Face size
    private int mAbsoluteFaceSize = 0;

    // VideoWriter Variables
    private VideoWriter videoWriter = null;
    private int noOfFrame = 0;
    private int maxNoOfFrame = 40;
    private Size videoSize = null;

    // Video Writer Naming Variables
    private String videoPrefix;
    private int videoSuffix = 0;

    //Image naming Variables
    private int imageSuffix = 0;

    // Network Related Variables
    public static String BASE_URL = "http://192.168.43.50:5000/file";
    public String RequestTag = "VideoVollyRequest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.opencv_camera);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        Random r = new Random( System.currentTimeMillis());
        videoPrefix = String.valueOf(10000 + r.nextInt(20000));
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");


                    try {
                        InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);

                        // File to load Haar Cascade
                        File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;

                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }

                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if(mJavaDetector.empty()){
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        }
                        else {
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                            cascadeDir.delete();
                        }

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Cascade File not Found. Exception thrown: " + e);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.setCameraIndex(1);
                    mOpenCvCameraView.enableView();
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public CameraActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

//    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
//        return Collections.singletonList(mOpenCvCameraView);
//    }

    @Override
    public void onPause(){
        super.onPause();
        if(mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if(!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        }
        else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if(mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //Relative Face size
        float mRelativeFaceSize = 0.4f;
        MatOfRect faces = new MatOfRect();
        Mat out;

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        // Minimum face size to be determined
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
                videoSize = new Size(mAbsoluteFaceSize * 1.3, mAbsoluteFaceSize * 1.3);
            }
        }

        // Video Writer Init
//        initVideoWriter();

        // Face Detector (Haar Cascade Classifier)
        if (mJavaDetector != null)
            mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2,
                    new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), videoSize);
        else{
            Log.e(TAG, "Detector is null");
        }


        Rect[] facesArray = faces.toArray();
        if(facesArray.length > 0){
            Log.e(TAG, "Find:" + facesArray.length + "Faces");
            for (Rect rect : facesArray){
                Imgproc.rectangle(mRgba, rect.tl(), rect.br(), FACE_RECT_COLOR, 3);
                out = mRgba.submat(rect);
                Imgproc.resize(out, out, videoSize);

                // Bitmap convertion
                Bitmap image = Bitmap.createBitmap(out.cols(),
                        out.rows(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(out, image);

                //Byte array convertion
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream .toByteArray();

                // Sending
                videoUpload(byteArray);
                imageSuffix++;


//                // VideoWriter Write Frames
//                if(maxNoOfFrame > noOfFrame){
//                    if (videoWriter != null) {
//                        videoWriter.write(out);
//                        noOfFrame++;
//                        Log.i("VIDEOWRITER","Writing Frame " + noOfFrame);
//                    }
//                }
//                else {
//                    videoWriter.release();
//                    Log.i("VIDEOWRITER","Saving Video");
//
//                    videoUpload(getVideoName());
//                    noOfFrame = 0;
//                    videoWriter = null;
//                    videoSuffix++;
//                }
            }
        }
        Core.flip(mRgba, mRgba, 1);
        return mRgba;
    }

//    private void initVideoWriter() {
//        if ( maxNoOfFrame > noOfFrame && videoWriter == null) {
//            videoWriter = new VideoWriter();
//
//            videoWriter.open(getVideoName(), VideoWriter.fourcc('M','J', 'P', 'G'),
//                    30.0, videoSize);
//            Log.d(TAG, "Opened Video writer " + getVideoName());
//        }
//    }

//    private String getVideoName() {
//        String dataDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/mat_videos";
//        String videoName = videoPrefix + "_" + videoSuffix;
//
//        return dataDir + "/" + videoName + ".avi";
//    }

    /**
     * Uploads any Byte data
     *
     * @param imageByte
     */
    private void videoUpload(final byte[] imageByte){
        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST,
                BASE_URL,
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        String resultResponse = new String(response.data);
                        try {
                            JSONObject result = new JSONObject(resultResponse);
                            //String status = result.getString("status");
                            String message = result.getString("message");

                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        NetworkResponse networkResponse = error.networkResponse;
                        String errorMessage = "Unknown error";
                        if (networkResponse == null) {
                            if (error.getClass().equals(TimeoutError.class)) {
                                errorMessage = "Request timeout";
                            }
                            else if (error.getClass().equals(NoConnectionError.class)) {
                                errorMessage = "Failed to connect server";
                            }
                        }
                        else {
                            String result = new String(networkResponse.data);
                            try {
                                JSONObject response = new JSONObject(result);
                                String status = response.getString("status");
                                String message = response.getString("message");

                                Log.e("Error Status", status);
                                Log.e("Error Message", message);

                                if (networkResponse.statusCode == 404) {
                                    errorMessage = "Resource not found";
                                }
                                else if (networkResponse.statusCode == 401) {
                                    errorMessage = message+" Please login again";
                                }
                                else if (networkResponse.statusCode == 400) {
                                    errorMessage = message+ " Check your inputs";
                                }
                                else if (networkResponse.statusCode == 500) {
                                    errorMessage = message+" Something is getting wrong";
                                }
                            }
                            catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.i("Error", errorMessage);
                        error.printStackTrace();
                    }
                }) {
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> params = new HashMap<>();
                        params.put("api_token", "gh659gjhvdyudo973823tt9gvjf7i6ric75r76");
                        return params;
                    }

                    @Override
                    protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();

                    params.put("file", new DataPart(imageSuffix + ".png",imageByte, "image/png"));

                    return params;
            }
        };

        RequestQueueApp.getInstance().addToRequestQueue(multipartRequest, RequestTag);
    }



}
