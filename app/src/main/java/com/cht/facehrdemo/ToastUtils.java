package com.cht.facehrdemo;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by Administrator on 2018/4/23.
 */

public class ToastUtils {

    private static Toast toast;

    public static void showToast(Context context,String s){
        if(toast == null){
            toast = Toast.makeText(context,s,Toast.LENGTH_LONG);
        }else{
            toast.setText(s);
        }
        toast.show();
    }


}
