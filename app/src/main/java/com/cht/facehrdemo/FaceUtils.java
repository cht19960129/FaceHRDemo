package com.cht.facehrdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.util.Log;

import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.guo.android_extend.image.ImageConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/4/19.
 */

public class FaceUtils {
    private static final String TAG = "FaceUtils";

    public static String appid = "FNdkFBH2S4dCg7xe3T2h5fXTSJaj5hWFUKAG8Zs4yjiu";
    public static String ft_key = "7Y4chbUwJESsee9k1wYvqeeNVkESNSWZr1rYESR5yPj8";
    public static String fd_key = "7Y4chbUwJESsee9k1wYvqeeVf9VbczQdz475hRaMzdQe";
    public static String fr_key = "7Y4chbUwJESsee9k1wYvqeecpYkmwRhcNyccnBgZZ9Rs";

    List<AFD_FSDKFace> AFD_list;
    private AFR_FSDKFace mAFR_FSDKFace;

    public AFR_FSDKFace initFace(Bitmap bitmap) {
        byte[] data = new byte[bitmap.getWidth() * bitmap.getHeight() * 3 / 2];
        Log.e(TAG, "data数据" + data);
        ImageConverter convert = new ImageConverter();
        convert.initial(bitmap.getWidth(), bitmap.getHeight(), ImageConverter.CP_PAF_NV21);
        if (convert.convert(bitmap, data)) {
            Log.d(TAG, "convert ok!");
        }
        convert.destroy();
        //人脸检测
        AFD_FSDKEngine AFD_engine = new AFD_FSDKEngine();
        AFD_list = new ArrayList<AFD_FSDKFace>();
        AFD_FSDKError AFD_error = AFD_engine.AFD_FSDK_InitialFaceEngine(appid, fd_key, AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5);
        Log.e(TAG, "AFD_FSDK_InitialFaceEngine = " + AFD_error.getCode());
        if (AFD_error.getCode() == AFD_FSDKError.MOK) {
            AFD_error = AFD_engine.AFD_FSDK_StillImageFaceDetection(data, bitmap.getWidth(), bitmap.getHeight(),
                    AFD_FSDKEngine.CP_PAF_NV21, AFD_list);
            Log.e(TAG, "AFD_FSDK_StillImageFaceDetection =" + AFD_error.getCode() + "<" + AFD_list.size() + "   bitmap.getHeight() 数据 " + bitmap.getHeight());
            Log.e(TAG, "AFD_list 人脸检测 " + AFD_list);
            if (!AFD_list.isEmpty()) {
                //实现了人脸识别的功能
                AFR_FSDKEngine engine1 = new AFR_FSDKEngine();
                //保存人脸特征信息
                AFR_FSDKFace result = new AFR_FSDKFace();
                //初始化人脸识别引擎，使用时请替换申请的 APPID 和 SDKKEY
                AFR_FSDKError AFR_error = engine1.AFR_FSDK_InitialEngine(appid, fr_key);
                Log.e("com.arcsoft", "AFR_FSDK_InitialEngine = " + AFR_error.getCode());
                AFR_error = engine1.AFR_FSDK_ExtractFRFeature(data, bitmap.getWidth(), bitmap.getHeight(),
                        AFR_FSDKEngine.CP_PAF_NV21, new Rect(AFD_list.get(0).getRect()), AFD_list.get(0).getDegree(), result);
                if (AFR_error.getCode() == AFR_FSDKError.MOK) {
                    //人脸特征数据
                    mAFR_FSDKFace = result.clone();
                    Log.e(TAG, "mAFR_FSDKFace 数据" + mAFR_FSDKFace);
                } else {
                    Log.e(TAG, "人脸特征无法检测");
                }
            } else {
                Log.e(TAG, "AFD_list数据为空");
                return null;
            }
        } else {
            Log.e(TAG, "AFD 人脸检测 初始化失败");
        }
        return mAFR_FSDKFace;
    }


    public static Bitmap decodeImage(String path) {
        Bitmap res;
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            BitmapFactory.Options op = new BitmapFactory.Options();
            op.inSampleSize = 1;
            op.inJustDecodeBounds = false;
            //op.inMutable = true;
            res = BitmapFactory.decodeFile(path, op);
            //rotate and scale.
            Matrix matrix = new Matrix();

            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                matrix.postRotate(90);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                matrix.postRotate(180);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                matrix.postRotate(270);
            }

            Bitmap temp = Bitmap.createBitmap(res, 0, 0, res.getWidth(), res.getHeight(), matrix, true);
            Log.d("com.arcsoft", "check target Image:" + temp.getWidth() + "X" + temp.getHeight());

            if (!temp.equals(res)) {
                res.recycle();
            }
            return temp;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
