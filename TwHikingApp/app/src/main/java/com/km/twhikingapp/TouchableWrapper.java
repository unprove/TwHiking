package com.km.twhikingapp;

/**
 * Created by kaomin on 2/8/18.
 */
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class TouchableWrapper extends FrameLayout {
    private static final String TAG = "TouchableWrapper";
    private OnTouchListener mOnTouchListener;

    public TouchableWrapper(Context context) {
        super(context);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //MainActivity.mMapIsTouched = true;
                //Log.i(TAG, "ACTION_DOWN");
                mOnTouchListener.onTouchDownListener();
                break;

            case MotionEvent.ACTION_UP:
                //MainActivity.mMapIsTouched = false;
                //Log.i(TAG, "ACTION_UP");
                mOnTouchListener.onTouchUpListener();
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    public interface OnTouchListener{
        void onTouchUpListener();
        void onTouchDownListener();
    }

    public void setOnTouchListener(OnTouchListener listener) {
        mOnTouchListener = listener;
    }
}