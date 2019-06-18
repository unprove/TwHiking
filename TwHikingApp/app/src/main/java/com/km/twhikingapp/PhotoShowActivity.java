package com.km.twhikingapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.ExifInterface;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;

public class PhotoShowActivity extends AppCompatActivity {
    private static final String TAG = "PhotoShowActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_show);

        ImageView ivPhoto = findViewById(R.id.ivPhotoShow);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String path = bundle.getString("photo_path");
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            Log.i(TAG, "screen size = " + size.x + " " + size.y);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPurgeable = true;
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            int ws = options.outWidth / size.x;
            int hs = options.outHeight / size.y;
            if(ws < 1) ws = 1;
            if(hs < 1) hs = 1;
            Log.i(TAG, "picture scale = " + ws + " " + hs);

            options = new BitmapFactory.Options();
            options.inPurgeable = true;
            options.inInputShareable = true;
            if(ws <= hs)
                options.inSampleSize = ws;
            else
                options.inSampleSize = hs;

            try {
                Bitmap bmp = BitmapFactory.decodeFile(path, options);
                Log.i(TAG, "bmp w = " + bmp.getWidth() + " h = " + bmp.getHeight());

                ExifInterface exif = new ExifInterface(path);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                Log.i(TAG, "orientation = " + orientation);
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        bmp = PhotoGpsUtils.bmpRotate(bmp, 90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        bmp = PhotoGpsUtils.bmpRotate(bmp, 180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        bmp = PhotoGpsUtils.bmpRotate(bmp, 270);
                        break;
                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                        bmp = PhotoGpsUtils.bmpFlip(bmp, true, false);
                        break;
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                        bmp = PhotoGpsUtils.bmpFlip(bmp, false, true);
                        break;
                    default:
                        break;
                }

                ivPhoto.setImageBitmap(bmp);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }


        ivPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

//        new Handler().postDelayed(new Runnable() {
//
//            /*
//             * Showing splash screen with a timer. This will be useful when you
//             * want to show case your app logo / company
//             */
//
//            @Override
//            public void run() {
//                // This method will be executed once the timer is over
//                // Start your app main activity
//                //Intent i = new Intent(SplashScreen.this, MainActivity.class); //MainActivity為主要檔案名稱
//                //startActivity(i);
//
//                // close this activity
//                finish();
//            }
//        }, 5000);
    }
}
