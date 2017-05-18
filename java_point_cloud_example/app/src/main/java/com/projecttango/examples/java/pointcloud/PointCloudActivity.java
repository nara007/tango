/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.projecttango.examples.java.pointcloud;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.rajawali3d.scene.ASceneFrameCallback;
import org.rajawali3d.surface.RajawaliSurfaceView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

import com.projecttango.tangosupport.TangoPointCloudManager;
import com.projecttango.tangosupport.TangoSupport;
import com.projecttango.tangosupport.ux.TangoUx;
import com.projecttango.tangosupport.ux.UxExceptionEvent;
import com.projecttango.tangosupport.ux.UxExceptionEventListener;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Main Activity class for the Point Cloud Sample. Handles the connection to the {@link Tango}
 * service and propagation of Tango point cloud data to OpenGL and Layout views. OpenGL rendering
 * logic is delegated to the {@link PointCloudRajawaliRenderer} class.
 */
public class PointCloudActivity extends Activity {
    private static final String TAG = PointCloudActivity.class.getSimpleName();

    private static final String UX_EXCEPTION_EVENT_DETECTED = "Exception Detected: ";
    private static final String UX_EXCEPTION_EVENT_RESOLVED = "Exception Resolved: ";

    private static final int SECS_TO_MILLISECS = 1000;
    private static final DecimalFormat FORMAT_THREE_DECIMAL = new DecimalFormat("0.000");
    private static final double UPDATE_INTERVAL_MS = 100.0;

    private Tango mTango;
    private TangoConfig mConfig;
    private TangoUx mTangoUx;

    private TangoPointCloudManager mPointCloudManager;
    private PointCloudRajawaliRenderer mRenderer;
    private RajawaliSurfaceView mSurfaceView;
    private TextView mPointCountTextView;

    private TextView mAverageZTextView;
    private double mPointCloudPreviousTimeStamp;

    private boolean mIsConnected = false;

    private double mPointCloudTimeToNextUpdate = UPDATE_INTERVAL_MS;

    private int mDisplayRotation = 0;


    // by Ye Song
    public FloatBuffer myPointCloud;
    public int pointsNum;
    public static float azimuth;
    public int frameIndex=0;
    public float curAzimuth;
    public float startAzimuth;
    public float nextFrameAngle;

    private FileUtils fileUtils = new FileUtils();

    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder.Callback callback;
    //TTS
    public static TextToSpeech tts;


    //message
    public static MyHandler myHandler=null;

    //http
    private OkHttpClient mOkHttpClient=new OkHttpClient();



    private Camera.PictureCallback myCallback = new Camera.PictureCallback(){
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            System.out.println("helloPic2");
            try {
                File filePath = new File(Environment.getExternalStorageDirectory(), "myPic");
                if(!filePath.exists()) {
                    filePath.mkdirs();
                }
//                File fileName = new File(filePath, System.currentTimeMillis() + ".jpg");
                File fileName = new File(filePath, frameIndex + ".jpg");
                fileName.createNewFile();
                FileOutputStream fos = new FileOutputStream(fileName);
                fos.write(data);
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


            System.out.println("nach my call");
            stopCamera();
            System.out.println("stopCamera");


//            mSurfaceView.onResume();



            synchronized (PointCloudActivity.this) {
                try {
                    mTangoUx.start();
                    // Check and request camera permission at run time.
                    bindTangoService();
                } catch (TangoErrorException e) {
                    Log.e(TAG, getString(R.string.exception_tango_error), e);
                }
            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            try{
        File filePath = new File(Environment.getExternalStorageDirectory(), "myPCD");
        if(!filePath.exists()) {
            filePath.mkdirs();
        }
        File fileName = new File(filePath, frameIndex + ".pcd");
        fileName.createNewFile();

        PrintStream ps=null;
        try {
                ps = new PrintStream(fileName);
        }catch(FileNotFoundException e) {
            e.printStackTrace();
        }
            if (pointsNum != 0) {
                ps.println(nextFrameAngle);
                int numFloats = 4 * pointsNum;
                for (int i = 0; i < pointsNum; i++) {

                    String str = myPointCloud.get(i)+" "+myPointCloud.get(i+1)+" "+myPointCloud.get(i+2)+" "+myPointCloud.get(i+3);
                    ps.println(str);
                }
            }

            ps.close();
        }

        catch(IOException e) {
            e.printStackTrace();
        }

            PointCloudActivity.this.frameIndex = (PointCloudActivity.this.frameIndex+1) % 6;

            if(PointCloudActivity.this.frameIndex == 0){

                PointCloudActivity.this.speak("work done");
            }
            else{
                PointCloudActivity.this.speak("next frame");
            }
        }


    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_cloud);

        mPointCountTextView = (TextView) findViewById(R.id.point_count_textview);
        mAverageZTextView = (TextView) findViewById(R.id.average_z_textview);
        mSurfaceView = (RajawaliSurfaceView) findViewById(R.id.gl_surface_view);


        mPointCloudManager = new TangoPointCloudManager();
        mTangoUx = setupTangoUxAndLayout();
        mRenderer = new PointCloudRajawaliRenderer(this);
        setupRenderer();

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    synchronized (this) {
                        setDisplayRotation();
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }
            }, null);
        }


        // Song Ye
        surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        callback = new SurfaceHolder.Callback(){
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
//                startCamera();
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
//                stopCamera();
            }
        };

        surfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        surfaceView.getHolder().addCallback(callback);
        surfaceView.getHolder().setFixedSize(300,300);
        surfaceView.setZOrderOnTop(true);


        //TTS
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // TTS初期化
                if (TextToSpeech.SUCCESS == status) {
                    System.out.println("initialized");
                } else {
                    System.out.println("failed to initialize");
                }
            }
        });
        tts.setLanguage(Locale.UK);

        myHandler = new MyHandler();

        new ClientSocket().start();
    }


    public void shutDownTTS(){
        if (null != tts) {
            // to release the resource of TextToSpeech
            tts.shutdown();
        }
    }
    public void startCamera(){
        camera = Camera.open();
        try {
            camera.setPreviewDisplay(surfaceView.getHolder());
//            camera.setDisplayOrientation(90);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void stopCamera(){
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSurfaceView.onResume();
        mTangoUx.start();
        // Check and request camera permission at run time.
        bindTangoService();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Synchronize against disconnecting while the service is being used in the OpenGL
        // thread or in the UI thread.
        // NOTE: DO NOT lock against this same object in the Tango callback thread.
        // Tango.disconnect will block here until all Tango callback calls are finished.
        // If you lock against this object in a Tango callback thread it will cause a deadlock.
        synchronized (this) {
            try {
                mTangoUx.stop();
                mTango.disconnect();
                mIsConnected = false;
            } catch (TangoErrorException e) {
                Log.e(TAG, getString(R.string.exception_tango_error), e);
            }
        }
    }

    @Override
    protected void onDestroy() {

        shutDownTTS();
        super.onDestroy();
    }

    /**
     * Initialize Tango Service as a normal Android Service.
     */
    private void bindTangoService() {
        // Initialize Tango Service as a normal Android Service. Since we call mTango.disconnect()
        // in onPause, this will unbind Tango Service, so every time onResume gets called we
        // should create a new Tango object.
        mTango = new Tango(PointCloudActivity.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there are no
            // UI thread changes involved.
            @Override
            public void run() {
                // Synchronize against disconnecting while the service is being used in the OpenGL
                // thread or in the UI thread.
                synchronized (PointCloudActivity.this) {
                    try {
                        TangoSupport.initialize();
                        mConfig = setupTangoConfig(mTango);
                        mTango.connect(mConfig);
                        startupTango();
                        mIsConnected = true;
                        setDisplayRotation();

                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, getString(R.string.exception_out_of_date), e);
                    } catch (TangoErrorException e) {
                        Log.e(TAG, getString(R.string.exception_tango_error), e);
                        showsToastAndFinishOnUiThread(R.string.exception_tango_error);
                    } catch (TangoInvalidException e) {
                        Log.e(TAG, getString(R.string.exception_tango_invalid), e);
                        showsToastAndFinishOnUiThread(R.string.exception_tango_invalid);
                    }
                }
            }
        });
    }

    /**
     * Sets up the Tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setupTangoConfig(Tango tango) {
        // Use the default configuration plus add depth sensing.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
//        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);
        return config;
    }

    /**
     * Set up the callback listeners for the Tango Service and obtain other parameters required
     * after Tango connection.
     * Listen to updates from the Point Cloud and Tango Events and Pose.
     */
    private void startupTango() {
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();

        framePairs.add(new TangoCoordinateFramePair(TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));

        mTango.connectListener(framePairs, new Tango.TangoUpdateCallback() {
            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                // Passing in the pose data to UX library produce exceptions.
                if (mTangoUx != null) {
                    mTangoUx.updatePoseStatus(pose.statusCode);
                }


                //by Song Ye
//                float myRotation[] = pose.getRotationAsFloats();
//                float w = myRotation[TangoPoseData.INDEX_ROTATION_W];
//                float x = myRotation[TangoPoseData.INDEX_ROTATION_X];
//                float y = myRotation[TangoPoseData.INDEX_ROTATION_Y];
//                float z = myRotation[TangoPoseData.INDEX_ROTATION_Z];
//                float matrix[] = new float[9];
//                matrix[0] = 1 - 2*y*y - 2*z*z;
//                matrix[1] =2*x*y - 2*w*z;
//                matrix[2] =2*x*z + 2*w*y;
//                matrix[3] =2*x*y + 2*w*z;
//                matrix[4] =1 - 2*x*x -2*z*z;
//                matrix[5] =2*y*z - 2*w*x;
//                matrix[6] =2*x*z - 2*y*w;
//                matrix[7] =2*y*z + 2*w*x;
//                matrix[8] =1 - 2*x*x - 2*y*y;
//                float orientation[] = new float[3];
//                SensorManager.getOrientation(matrix, orientation);
//
//                float azimuth= (float) (Math.toDegrees(SensorManager.getOrientation(matrix, orientation)[0]) + 360) % 360;
//                PointCloudActivity.this.azimuth = azimuth;
//
//                System.out.println(""+azimuth);
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData pointCloud) {
                if (mTangoUx != null) {
                    mTangoUx.updatePointCloud(pointCloud);
                }
                mPointCloudManager.updatePointCloud(pointCloud);

                final double currentTimeStamp = pointCloud.timestamp;
                final double pointCloudFrameDelta =
                        (currentTimeStamp - mPointCloudPreviousTimeStamp) * SECS_TO_MILLISECS;
                mPointCloudPreviousTimeStamp = currentTimeStamp;
                final double averageDepth = getAveragedDepth(pointCloud.points,
                        pointCloud.numPoints);

                // by Ye Song
                PointCloudActivity.this.myPointCloud = pointCloud.points;
                PointCloudActivity.this.pointsNum = pointCloud.numPoints;

                mPointCloudTimeToNextUpdate -= pointCloudFrameDelta;

                if (mPointCloudTimeToNextUpdate < 0.0) {
                    mPointCloudTimeToNextUpdate = UPDATE_INTERVAL_MS;
                    final String pointCountString = Integer.toString(pointCloud.numPoints);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            mPointCountTextView.setText(pointCountString);
                            mPointCountTextView.setText(""+PointCloudActivity.azimuth);
                            mAverageZTextView.setText(FORMAT_THREE_DECIMAL.format(averageDepth));
                        }
                    });
                }
            }

            @Override
            public void onTangoEvent(TangoEvent event) {
                if (mTangoUx != null) {
                    mTangoUx.updateTangoEvent(event);
                }
            }

            @Override
            public void onFrameAvailable(int cameraId) {
                super.onFrameAvailable(cameraId);

                System.out.println("camera id:" + cameraId);
            }
        });
    }

    /**
     * Sets Rajawali surface view and its renderer. This is ideally called only once in onCreate.
     */
    public void setupRenderer() {
        mSurfaceView.setEGLContextClientVersion(2);
        mRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                // NOTE: This will be executed on each cycle before rendering; called from the
                // OpenGL rendering thread.

                // Prevent concurrent access from a service disconnect through the onPause event.
                synchronized (PointCloudActivity.this) {
                    // Don't execute any Tango API actions if we're not connected to the service.
                    if (!mIsConnected) {
                        return;
                    }

                    // Update point cloud data.
                    TangoPointCloudData pointCloud = mPointCloudManager.getLatestPointCloud();
                    if (pointCloud != null) {
                        // Calculate the depth camera pose at the last point cloud update.
                        TangoSupport.TangoMatrixTransformData transform =
                                TangoSupport.getMatrixTransformAtTime(pointCloud.timestamp,
                                        TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                        TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                        TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                                        TangoSupport.ROTATION_IGNORED);
                        if (transform.statusCode == TangoPoseData.POSE_VALID) {
                            mRenderer.updatePointCloud(pointCloud, transform.matrix);
                        }
                    }

                    // Update current camera pose.
                    try {
                        // Calculate the device pose. This transform is used to display
                        // frustum in third and top down view, and used to render camera pose in
                        // first person view.
                        TangoPoseData lastFramePose = TangoSupport.getPoseAtTime(0,
                                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                TangoPoseData.COORDINATE_FRAME_DEVICE,
                                TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                mDisplayRotation);
                        if (lastFramePose.statusCode == TangoPoseData.POSE_VALID) {
                            mRenderer.updateCameraPose(lastFramePose);
                        }
                    } catch (TangoErrorException e) {
                        Log.e(TAG, "Could not get valid transform");
                    }
                }
            }

            @Override
            public boolean callPreFrame() {
                return true;
            }

            @Override
            public void onPreDraw(long sceneTime, double deltaTime) {

            }

            @Override
            public void onPostFrame(long sceneTime, double deltaTime) {

            }
        });
        mSurfaceView.setSurfaceRenderer(mRenderer);
    }

    /**
     * Sets up TangoUX and sets its listener.
     */
    private TangoUx setupTangoUxAndLayout() {
        TangoUx tangoUx = new TangoUx(this);
        tangoUx.setUxExceptionEventListener(mUxExceptionListener);
        return tangoUx;
    }

    /*
    * Set a UxExceptionEventListener to be notified of any UX exceptions.
    * In this example we are just logging all the exceptions to logcat, but in a real app,
    * developers should use these exceptions to contextually notify the user and help direct the
    * user in using the device in a way Tango Service expects it.
    * <p>
    * A UxExceptionEvent can have two statuses: DETECTED and RESOLVED.
    * An event is considered DETECTED when the exception conditions are observed, and RESOLVED when
    * the root causes have been addressed.
    * Both statuses will trigger a separate event.
    */
    private UxExceptionEventListener mUxExceptionListener = new UxExceptionEventListener() {
        @Override
        public void onUxExceptionEvent(UxExceptionEvent uxExceptionEvent) {
            String status = uxExceptionEvent.getStatus() == UxExceptionEvent.STATUS_DETECTED ?
                    UX_EXCEPTION_EVENT_DETECTED : UX_EXCEPTION_EVENT_RESOLVED;

            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_LYING_ON_SURFACE) {
                Log.i(TAG, status + "Device lying on surface");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_DEPTH_POINTS) {
                Log.i(TAG, status + "Too few depth points");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_FEATURES) {
                Log.i(TAG, status + "Too few features");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOTION_TRACK_INVALID) {
                Log.i(TAG, status + "Invalid poses in MotionTracking");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOVING_TOO_FAST) {
                Log.i(TAG, status + "Moving too fast");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FISHEYE_CAMERA_OVER_EXPOSED) {
                Log.i(TAG, status + "Fisheye Camera Over Exposed");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FISHEYE_CAMERA_UNDER_EXPOSED) {
                Log.i(TAG, status + "Fisheye Camera Under Exposed");
            }
        }
    };

    /**
     * First Person button onClick callback.
     */
    public void onFirstPersonClicked(View v) {
        mRenderer.setFirstPersonView();
    }

    /**
     * Third Person button onClick callback.
     */
    public void onThirdPersonClicked(View v) {
        mRenderer.setThirdPersonView();
    }

    /**
     * Top-down button onClick callback.
     */
    public void onTopDownClicked(View v) {
        mRenderer.setTopDownView();
    }


    public boolean isLocatedInRange(float begin, float end, float angle){


        if(end > begin){
            if(angle>begin && angle<end){
                return true;
            }
            else{
                return false;
            }
        }

        else{

            if((angle>begin && angle<360) || (angle>=0 && angle<end)){
                return true;
            }

            else{
                return false;
            }
        }

    }


    private void speak(String text) {

        if (tts.isSpeaking()) {
            tts.stop();
            return;
        }

        if (null != tts) {
            tts.setSpeechRate(0.5f);
        }

        if (null != tts) {
            tts.setPitch(1.0f);
        }

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);

        if (Build.VERSION.SDK_INT >= 15) {
            int listenerResult = tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId) {
                    System.out.println("progress on Done " + utteranceId);
                }

                @Override
                public void onError(String utteranceId) {
                    System.out.println("progress on Error " + utteranceId);
                }

                @Override
                public void onStart(String utteranceId) {
                    System.out.println("progress on Start " + utteranceId);
                }

            });
            if (listenerResult != TextToSpeech.SUCCESS) {
                System.out.println("failed to add utterance progress listener");
            }
        } else {
            System.out.println("Build VERSION is less than API 15");
        }


    }

    public boolean shoudTurnLeft(float begin, float end, float angle){

            if(end<begin){
                float dis = angle - end;
                if(dis>=0 && dis<=150){
                    return true;
                }
                else{
                    return false;
                }
            }

            else{
                float dis;
                if(angle>=end){
                    dis = angle - end;
                    if(dis>=0 && dis<=150){
                        return true;
                    }
                    else{
                        return false;
                    }
                }
                else{
                    dis = begin - angle;
                    if(dis>=0 && dis<=150){
                        return false;
                    }
                    else{
                        return true;
                    }
                }
            }
    }

    public void onSaveClicked(View v){

        if(frameIndex==0){
            startAzimuth = azimuth;
            nextFrameAngle = startAzimuth;
        }

        else{
            nextFrameAngle = (startAzimuth+60*frameIndex)>=360? startAzimuth+60*frameIndex-360:startAzimuth+60*frameIndex;
            float startAngle = (nextFrameAngle - 5)<0 ? nextFrameAngle - 5+360:nextFrameAngle - 5;
            float endAngle = (nextFrameAngle + 5>=360)? nextFrameAngle+5-360:nextFrameAngle+5;

            if(!isLocatedInRange(startAngle, endAngle, azimuth)){
                // TODO: 17/4/23  turn left of turn right

                if(shoudTurnLeft(startAngle, endAngle, azimuth)){
                    speak("turn left");
                }
                else{
                    speak("turn right");
                }

                return;
            }
        }

        synchronized (this) {
            try {
                mTangoUx.stop();
                mTango.disconnect();
                mIsConnected = false;
            } catch (TangoErrorException e) {
                Log.e(TAG, getString(R.string.exception_tango_error), e);
            }
        }

        startCamera();
//        camera.startPreview();
        camera.takePicture(null, null, myCallback);

//        System.out.println("nach my call");
//        stopCamera();
//        System.out.println("stopCamera");
//
//
//        mSurfaceView.onResume();
//        mTangoUx.start();
//        // Check and request camera permission at run time.
//        bindTangoService();

//        try{
//        File filePath = new File(Environment.getExternalStorageDirectory(), "myPCD");
//        if(!filePath.exists()) {
//            filePath.mkdirs();
//        }
//        File fileName = new File(filePath, System.currentTimeMillis() + ".pcd");
//        fileName.createNewFile();
//
//        PrintStream ps=null;
//        try {
//                ps = new PrintStream(fileName);
//        }catch(FileNotFoundException e) {
//            e.printStackTrace();
//        }
//            if (pointsNum != 0) {
//                int numFloats = 4 * pointsNum;
//                for (int i = 0; i < pointsNum; i++) {
//
//                    String str = myPointCloud.get(i)+" "+myPointCloud.get(i+1)+" "+myPointCloud.get(i+2)+" "+myPointCloud.get(i+3);
//                    ps.println(str);
//                }
//            }
//
//            ps.close();
//        }
//
//        catch(IOException e) {
//            e.printStackTrace();
//        }




        System.out.println("save clicked 123456");
    }



    public void onSave(){

        if(frameIndex==0){
            startAzimuth = azimuth;
            nextFrameAngle = startAzimuth;
        }

        else{
            nextFrameAngle = (startAzimuth+60*frameIndex)>=360? startAzimuth+60*frameIndex-360:startAzimuth+60*frameIndex;
            float startAngle = (nextFrameAngle - 5)<0 ? nextFrameAngle - 5+360:nextFrameAngle - 5;
            float endAngle = (nextFrameAngle + 5>=360)? nextFrameAngle+5-360:nextFrameAngle+5;

            if(!isLocatedInRange(startAngle, endAngle, azimuth)){
                // TODO: 17/4/23  turn left of turn right

                if(shoudTurnLeft(startAngle, endAngle, azimuth)){
                    speak("turn left");
                }
                else{
                    speak("turn right");
                }

                return;
            }
        }

        synchronized (this) {
            try {
                mTangoUx.stop();
                mTango.disconnect();
                mIsConnected = false;
            } catch (TangoErrorException e) {
                Log.e(TAG, getString(R.string.exception_tango_error), e);
            }
        }

        startCamera();
//        camera.startPreview();
        camera.takePicture(null, null, myCallback);

    }


    public void onUpload(){

        if(fileUtils.isFileExist("myPic/0.jpg") && fileUtils.isFileExist("myPic/1.jpg") &&
                fileUtils.isFileExist("myPic/2.jpg") && fileUtils.isFileExist("myPic/3.jpg") &&
                fileUtils.isFileExist("myPic/4.jpg") && fileUtils.isFileExist("myPic/5.jpg") &&
                fileUtils.isFileExist("myPCD/0.pcd") && fileUtils.isFileExist("myPCD/1.pcd") &&
                fileUtils.isFileExist("myPCD/2.pcd") && fileUtils.isFileExist("myPCD/3.pcd") &&
                fileUtils.isFileExist("myPCD/4.pcd") && fileUtils.isFileExist("myPCD/5.pcd")){

//            speak("all files exist");
            File pic0 =fileUtils.getFile("myPic/0.jpg");
            File pic1 =fileUtils.getFile("myPic/1.jpg");
            File pic2 =fileUtils.getFile("myPic/2.jpg");
            File pic3 =fileUtils.getFile("myPic/3.jpg");
            File pic4 =fileUtils.getFile("myPic/4.jpg");
            File pic5 =fileUtils.getFile("myPic/5.jpg");

            File pcd0 =fileUtils.getFile("myPCD/0.pcd");
            File pcd1 =fileUtils.getFile("myPCD/1.pcd");
            File pcd2 =fileUtils.getFile("myPCD/2.pcd");
            File pcd3 =fileUtils.getFile("myPCD/3.pcd");
            File pcd4 =fileUtils.getFile("myPCD/4.pcd");
            File pcd5 =fileUtils.getFile("myPCD/5.pcd");

            MultipartBody.Builder builder=  new MultipartBody.Builder().setType(MultipartBody.FORM);
            builder.addFormDataPart("pic", pic0.getName(), RequestBody.create(MediaType.parse("image/jpg"), pic0));
            builder.addFormDataPart("pic", pic1.getName(), RequestBody.create(MediaType.parse("image/jpg"), pic1));
            builder.addFormDataPart("pic", pic2.getName(), RequestBody.create(MediaType.parse("image/jpg"), pic2));
            builder.addFormDataPart("pic", pic3.getName(), RequestBody.create(MediaType.parse("image/jpg"), pic3));
            builder.addFormDataPart("pic", pic4.getName(), RequestBody.create(MediaType.parse("image/jpg"), pic4));
            builder.addFormDataPart("pic", pic5.getName(), RequestBody.create(MediaType.parse("image/jpg"), pic5));

            builder.addFormDataPart("pcd", pcd0.getName(), RequestBody.create(MediaType.parse("text/plain"), pcd0));
            builder.addFormDataPart("pcd", pcd1.getName(), RequestBody.create(MediaType.parse("text/plain"), pcd1));
            builder.addFormDataPart("pcd", pcd2.getName(), RequestBody.create(MediaType.parse("text/plain"), pcd2));
            builder.addFormDataPart("pcd", pcd3.getName(), RequestBody.create(MediaType.parse("text/plain"), pcd3));
            builder.addFormDataPart("pcd", pcd4.getName(), RequestBody.create(MediaType.parse("text/plain"), pcd4));
            builder.addFormDataPart("pcd", pcd5.getName(), RequestBody.create(MediaType.parse("text/plain"), pcd5));

            RequestBody requestBody = builder.build();

            Request request = new Request.Builder()
//                .header("Authorization", "Client-ID " +"9199fdef135c122")
                    .url("http://172.26.144.64:3000/uploadfiles")
                    .post(requestBody)
                    .build();


            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
//                Log.i("wangshu",response.body().string());

                    System.out.println(response.body().string());
                }
            });

        }

        else{
            speak("lack files");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mRenderer.onTouchEvent(event);
        return true;
    }

    /**
     * Calculates the average depth from a point cloud buffer.
     *
     * @param pointCloudBuffer
     * @param numPoints
     * @return Average depth.
     */
    private float getAveragedDepth(FloatBuffer pointCloudBuffer, int numPoints) {
        float totalZ = 0;
        float averageZ = 0;
        if (numPoints != 0) {
            int numFloats = 4 * numPoints;
            for (int i = 2; i < numFloats; i = i + 4) {
                totalZ = totalZ + pointCloudBuffer.get(i);
            }
            averageZ = totalZ / numPoints;
        }
        return averageZ;
    }

    /**
     * Query the display's rotation.
     */
    private void setDisplayRotation() {
        Display display = getWindowManager().getDefaultDisplay();
        mDisplayRotation = display.getRotation();
    }

    /**
     * Display toast on UI thread.
     *
     * @param resId The resource id of the string resource to use. Can be formatted text.
     */
    private void showsToastAndFinishOnUiThread(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PointCloudActivity.this,
                        getString(resId), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }


    class MyHandler extends Handler {
        public MyHandler() {
        }

        public MyHandler(Looper L) {
            super(L);
        }

        // 子类必须重写此方法，接受数据
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);

            if(msg.what==0x3){
                System.out.println("mykey "+msg.obj);

                int key = (int)(msg.obj);
                if(key == 29){
                    onSave();
                }

                else if(key == 30){
                    onUpload();
                }
            }

        }
    }
}
