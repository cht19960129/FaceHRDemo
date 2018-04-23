package com.cht.facehrdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import java.io.File;

/**
 * Created by Administrator on 2018/4/21.
 */

public class MainActivity extends Activity implements View.OnClickListener{
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_IMAGE_CAMERA = 1;
    private static final int REQUEST_CODE_IMAGE_OP = 2;
    private static final int REQUEST_CODE_OP = 3;
    private String mFilePath;

    private EditText edtName;
    private Button btnSave,btnIdentify;

    FaceUtils utils = new FaceUtils();
    private ImageView faceSrc;
    AFR_FSDKFace faceData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFilePath = Environment.getExternalStorageDirectory().getPath();// 获取SD卡路径
        mFilePath = mFilePath + "/" + "temp.png";// 指定路径


        new Thread(new Runnable() {
            @Override
            public void run() {
                Application app = (Application) MainActivity.this.getApplicationContext();
                app.mFaceDB.loadFaces();
            }
        }).start();

        initViewById();
        faceSrc = findViewById(R.id.faceSrc);
        findViewById(R.id.face).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                entryFace();
            }
        });
    }

    private void initViewById() {
        edtName = findViewById(R.id.edtName);
        btnSave = findViewById(R.id.btnSave);
        btnIdentify = findViewById(R.id.btnIdentify);

        btnSave.setOnClickListener(this);
        btnIdentify.setOnClickListener(this);
    }

    private void entryFace() {
        new AlertDialog.Builder(this)
                .setTitle("请选择注册方式")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setItems(new String[]{"打开图片", "拍摄照片"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 1:   //拍摄照片
                                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);// 启动系统相机
                                Uri photoUri = Uri.fromFile(new File(mFilePath)); // 传递路径
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);// 更改系统默认存储路径
                                startActivityForResult(intent, REQUEST_CODE_IMAGE_CAMERA);
                                break;
                            case 0:  //打开图片
                                Intent intent2 = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(intent2, REQUEST_CODE_IMAGE_OP);
                                break;
                            default:
                                ;
                        }
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //打开图片
        if (requestCode == REQUEST_CODE_IMAGE_OP && resultCode == RESULT_OK) {
            Uri mPath = data.getData();
            Cursor cursor = getContentResolver().query(mPath, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                Bitmap bitmap = FaceUtils.decodeImage(path);
                faceSrc.setImageBitmap(bitmap);
                startRegister(bitmap, path);//注册  bmp   file  图库
            }

        } else if (requestCode == REQUEST_CODE_IMAGE_CAMERA) {
            Bitmap bitmap2 = FaceUtils.decodeImage(mFilePath);
            faceSrc.setImageBitmap(bitmap2);
            startRegister(bitmap2, mFilePath);//注册  bmp   file  相机
        }
    }

    private void startRegister(Bitmap b, String mFilePath) {
        faceData = utils.initFace(b);
        if(faceData == null){
            Log.e(TAG,"faceData为空");
            ToastUtils.showToast(MainActivity.this,"未获取到数据特征");
        }else{
            ToastUtils.showToast(MainActivity.this,"获取到数据特征");
            btnSave.setEnabled(true);
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btnSave:
                String name = edtName.getText().toString().trim();
                if(!name.isEmpty()){
                    ((Application)MainActivity.this.getApplicationContext()).mFaceDB.addFace(name,faceData);
                    ToastUtils.showToast(MainActivity.this,"注册完成");
                }else{
                    ToastUtils.showToast(MainActivity.this,"填写完整信息");
                }
                break;

            case R.id.btnIdentify:
                //先判断face是否为空  如果为空就不用开始
                if(((Application)getApplicationContext()).mFaceDB.mRegister.isEmpty()){
                    Log.e(TAG, "人脸的数量22: "+((Application)getApplicationContext()).mFaceDB.mRegister.size() );
                    ToastUtils.showToast(MainActivity.this,"数据为空，请添加数据");
                }else{
                    Log.e(TAG, "人脸的数量: "+((Application)getApplicationContext()).mFaceDB.mRegister.size() );
                    new AlertDialog.Builder(this)
                            .setTitle("请选择相机")
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setItems(new String[]{"后置相机", "前置相机"}, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startDetector(which);
                                }
                            })
                            .show();
                }


                break;
        }
    }

    private void startDetector(int camera) {
        Intent it = new Intent(MainActivity.this, DetectActivity.class);
        it.putExtra("Camera", camera);
        startActivityForResult(it, REQUEST_CODE_OP);
    }
}
