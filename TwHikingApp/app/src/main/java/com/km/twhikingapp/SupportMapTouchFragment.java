package com.km.twhikingapp;

/**
 * Created by kaomin on 2/8/18.
 */
import com.google.android.gms.maps.SupportMapFragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class SupportMapTouchFragment extends SupportMapFragment {
    public View mOriginalContentView;
    public TouchableWrapper mTouchView;
    OnCurrentPositonListener mOnCurrentPositionListener = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        mOriginalContentView = super.onCreateView(inflater, parent, savedInstanceState);

        mTouchView = new TouchableWrapper(getActivity());
        mTouchView.addView(mOriginalContentView);
        LayoutParams lp = new LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT );
        lp.gravity = Gravity.CENTER_VERTICAL|Gravity.RIGHT;
        ImageView iv = new ImageView(getContext());
        iv.setLayoutParams(lp);
        Bitmap bImage = BitmapFactory.decodeResource(getResources(), R.raw.current);
        iv.setImageBitmap(bImage);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mOnCurrentPositionListener != null) {
                    mOnCurrentPositionListener.onCurrentPosition();
                }
            }
        });
        //TextView tv = new TextView(getContext());
        //tv.setLayoutParams(lp);
        //tv.setText("Hey, World");
        mTouchView.addView(iv);
        return mTouchView;
    }

    @Override
    public View getView() {
        return mOriginalContentView;
    }

    public void setTouchListener(TouchableWrapper.OnTouchListener listener) {
        mTouchView.setOnTouchListener(listener);
    }

    public void setCurrentPositionListener(OnCurrentPositonListener listener) {
        mOnCurrentPositionListener = listener;
    }

    public interface OnCurrentPositonListener {
        void onCurrentPosition();
    }
}
