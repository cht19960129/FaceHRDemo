package com.cht.facehrdemo;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKVersion;
import com.guo.android_extend.java.AbsLoop;
import com.guo.android_extend.java.ExtByteArrayOutputStream;
import com.guo.android_extend.tools.CameraHelper;
import com.guo.android_extend.widget.CameraFrameData;
import com.guo.android_extend.widget.CameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/4/20.
 */

public class DetectActivity extends Activity implements View.OnTouchListener,CameraSurfaceView.OnCameraListener,Camera.AutoFocusCallback {
    private static final String TAG = "DetectActivity";

    private CameraSurfaceView mSurfaceView;
    private CameraGLSurfaceView mGLSurfaceView;
    private int mWidth, mHeight, mFormat;
    private Camera mCamera;

    //人脸跟踪
    AFT_FSDKVersion version = new AFT_FSDKVersion();
    AFT_FSDKEngine engine = new AFT_FSDKEngine();
    // 用来存放检测到的人脸信息列表
    List<AFT_FSDKFace> result = new ArrayList<>();

    List<FaceDB.FaceRegist> mResgist;

    int mCameraID;
    int mCameraRotate;
    boolean mCameraMirror;
    byte[] mImageNV21 = null;
    FRAbsLoop mFRAbsLoop = null;
    AFT_FSDKFace mAFT_FSDKFace = null;
    Handler mHandler;

    Runnable hide = new Runnable() {
        @Override
        public void run() {
//            mTextView.setAlpha(0.5f);
//            mImageView.setImageAlpha(128);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResgist = ((Application)DetectActivity.this.getApplicationContext()).mFaceDB.mRegister;
        Log.e(TAG,"mRegister："+mResgist.size());

        mCameraID = getIntent().getIntExtra("Camera", 0) == 0 ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
        Log.e(TAG,"摄像机"+String.valueOf(mCameraID));
        mCameraRotate = getIntent().getIntExtra("Camera", 0) == 0 ? 90 : 270;
        mCameraMirror = getIntent().getIntExtra("Camera", 0) == 0 ? false : true;
        mWidth = 1280;
        mHeight = 960;
        mFormat = ImageFormat.NV21;
        mHandler = new Handler();
        setContentView(R.layout.activity_detect);

        mGLSurfaceView = findViewById(R.id.glsurfaceView);
        mGLSurfaceView.setOnTouchListener(this);
        mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceView.setOnCameraListener(this);
        mSurfaceView.setupGLSurafceView(mGLSurfaceView, true, mCameraMirror, mCameraRotate);
        mSurfaceView.debug_print_fps(true, false);


        //人脸识别函数
        AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine(FaceUtils.appid, FaceUtils.ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        Log.e(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
        err = engine.AFT_FSDK_GetVersion(version);
        Log.e(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());
        mFRAbsLoop = new FRAbsLoop();
        mFRAbsLoop.start();
    }

    @Override  //mGLSurfaceView的方法
    public boolean onTouch(View v, MotionEvent event) {
        CameraHelper.touchFocus(mCamera, event, v, this);
        return false;
    }

    @Override // AutoFocusCallback
    public void onAutoFocus(boolean success, Camera camera) {
        if (success) {
            Log.e(TAG, "Camera Focus SUCCESS!");
        }
    }

    @Override //OnCameraListener
    public Camera setupCamera() {
        // TODO Auto-generated method stub
        mCamera = Camera.open(mCameraID);
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mWidth, mHeight);
            parameters.setPreviewFormat(mFormat);

            for( Camera.Size size : parameters.getSupportedPreviewSizes()) {
                Log.d(TAG, "SIZE:" + size.width + "x" + size.height);
            }
            for( Integer format : parameters.getSupportedPreviewFormats()) {
                Log.d(TAG, "FORMAT:" + format);
            }

            List<int[]> fps = parameters.getSupportedPreviewFpsRange();
            for(int[] count : fps) {
                Log.d(TAG, "T:");
                for (int data : count) {
                    Log.d(TAG, "V=" + data);
                }
            }
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mCamera != null) {
            mWidth = mCamera.getParameters().getPreviewSize().width;
            mHeight = mCamera.getParameters().getPreviewSize().height;
        }
        return mCamera;
    }

    @Override//OnCameraListener
    public void setupChanged(int format, int width, int height) {

    }

    @Override//OnCameraListener  开始预览后
    public boolean startPreviewLater() {
        return false;
    }

    @Override//OnCameraListener  预览
    public Object onPreview(byte[] data, int width, int height, int format, long timestamp) {
        AFT_FSDKError err = engine.AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, result);
        Log.d(TAG, "AFT_FSDK_FaceFeatureDetect =" + err.getCode());
        Log.d(TAG, "Face=" + result.size());
        for (AFT_FSDKFace face : result) {
            Log.d(TAG, "Face:" + face.toString());
        }
        if (mImageNV21 == null) {
            if (!result.isEmpty()) {
                mAFT_FSDKFace = result.get(0).clone();
                mImageNV21 = data.clone();
            } else {
                mHandler.postDelayed(hide, 3000);
            }
        }
        //copy rects
        Rect[] rects = new Rect[result.size()];
        for (int i = 0; i < result.size(); i++) {
            rects[i] = new Rect(result.get(i).getRect());
        }
        //clear result.
        result.clear();
        //return the rects for render.
        return rects;
    }

    @Override//OnCameraListener
    public void onBeforeRender(CameraFrameData data) {

    }

    @Override//OnCameraListener
    public void onAfterRender(CameraFrameData data) {
        mGLSurfaceView.getGLES2Render().draw_rect((Rect[])data.getParams(), Color.GREEN, 2);
    }


    class FRAbsLoop extends AbsLoop {
        AFR_FSDKVersion version = new AFR_FSDKVersion();
        AFR_FSDKEngine engine = new AFR_FSDKEngine();
        AFR_FSDKFace result = new AFR_FSDKFace();

        @Override
        public void setup() {
            AFR_FSDKError error = engine.AFR_FSDK_InitialEngine(FaceUtils.appid, FaceUtils.fr_key);
            Log.e(TAG, "AFR_FSDK_InitialEngine = " + error.getCode());
            error = engine.AFR_FSDK_GetVersion(version);
            Log.e(TAG, "FR=" + version.toString() + "," + error.getCode()); //(210, 178 - 478, 446), degree = 1　780, 2208 - 1942, 3370
        }

        @Override
        public void loop() {
            if (mImageNV21 != null) {
                long time = System.currentTimeMillis();
                AFR_FSDKError error = engine.AFR_FSDK_ExtractFRFeature(mImageNV21, mWidth, mHeight, AFR_FSDKEngine.CP_PAF_NV21, mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree(), result);
                Log.e(TAG, "AFR_FSDK_ExtractFRFeature cost :" + (System.currentTimeMillis() - time) + "ms");
                Log.e(TAG, "Face=" + result.getFeatureData()[0] + "," + result.getFeatureData()[1] + "," + result.getFeatureData()[2] + "," + error.getCode());
                AFR_FSDKMatching score = new AFR_FSDKMatching();
                float max = 0.0f;
                String name = null;
                for (FaceDB.FaceRegist fr : mResgist) {
                    for (AFR_FSDKFace face : fr.mFaceList) {
                        error = engine.AFR_FSDK_FacePairMatching(result, face, score);
                        Log.d(TAG,  "Score:" + score.getScore() + ", AFR_FSDK_FacePairMatching=" + error.getCode());
                        if (max < score.getScore()) {
                            max = score.getScore();
                            name = fr.mName;
                        }
                    }
                }
                //crop
                byte[] data = mImageNV21;
                YuvImage yuv = new YuvImage(data, ImageFormat.NV21, mWidth, mHeight, null);
                ExtByteArrayOutputStream ops = new ExtByteArrayOutputStream();
                yuv.compressToJpeg(mAFT_FSDKFace.getRect(), 80, ops);
                try {
                    ops.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (max > 0.6f) {
                    //fr success.
                    final float max_score = max;
                    Log.d(TAG, "fit Score:" + max + ", NAME:" + name);
                    final String mNameShow = name;
                    mHandler.removeCallbacks(hide);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "置信度" + (float) ((int) (max_score * 1000)) / 1000.0);
                            Log.e(TAG, "名字" + mNameShow);
                            ToastUtils.showToast(DetectActivity.this,"置信度：" + (float) ((int) (max_score * 1000)) / 1000.0 + "  名字 ：" + mNameShow);
                        }
                    });
                } else {
                    final String mNameShow = "未识别";
                    DetectActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //自己的操作
                            Log.e(TAG,mNameShow);
                            ToastUtils.showToast(DetectActivity.this,mNameShow);
                        }
                    });
                }
                mImageNV21 = null;
            }
        }

        @Override
        public void over() {
            AFR_FSDKError error = engine.AFR_FSDK_UninitialEngine();
            Log.d(TAG, "AFR_FSDK_UninitialEngine : " + error.getCode());
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFRAbsLoop.shutdown();
        AFT_FSDKError err = engine.AFT_FSDK_UninitialFaceEngine();
        Log.d(TAG, "AFT_FSDK_UninitialFaceEngine =" + err.getCode());
    }
}
