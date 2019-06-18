package com.km.twhikingapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import com.km.twhikingapp.PhotoGpsUtils.PhotoInfo;
import com.km.twhikingapp.PhotoGpsUtils.PositionInfo;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnMap = (Button)findViewById(R.id.buttonMap);
        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainActivity.this, MapsActivity.class);
                //myIntent.putExtra("find_stone", true);
                MainActivity.this.startActivity(myIntent);
            }
        });

        Button btnPhoto = (Button)findViewById(R.id.buttonPhoto);
        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<PhotoInfo> photoInfoList = getPhotoInfoList(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+"/100ANDRO");

                if(photoInfoList != null && photoInfoList.size() > 0) {
                    /*
                    PhotoInfo prev = photoInfoList.get(0);
                    int tripid = 1;
                    prev.setTripId(tripid);
                    for (int i = 1; i < photoInfoList.size(); i++) {
                        float[] res = new float[1];
                        Location.distanceBetween(prev.mPosition.mLatitude, prev.mPosition.mLongitude, photoInfoList.get(i).mPosition.mLatitude, photoInfoList.get(i).mPosition.mLongitude, res);
                        Log.i("Photo", "distance = " + res[0] + " time diff = " + (photoInfoList.get(i).mTimestamp - prev.mTimestamp));
                        long timeDiff = photoInfoList.get(i).mTimestamp - prev.mTimestamp;
                        // TODO: to collect more data to decide conditions
                        if ( (timeDiff > 30*60*1000 && res[0] > 3000) || timeDiff > 86400*1000) {
                            tripid++;
                        }
                        prev = photoInfoList.get(i);
                        prev.setTripId(tripid);
                    }
                    Log.i("Photo", "group num = " + tripid);
                    */
                    ArrayList<PositionInfo> stonelist = null;
                    try {
                        stonelist = PhotoGpsUtils.loadStonesInfo(getAssets().open("stones.csv"));
                    } catch(Exception e) {
                        e.printStackTrace();
                    }

                    if(stonelist != null) {
                        for (PhotoInfo pi : photoInfoList) {
                            float mindist = 1000000f;
                            PositionInfo nearstone = null;
                            for (PositionInfo stone : stonelist) {
                                float[] res = new float[1];
                                Location.distanceBetween(pi.mPosition.mLatitude, pi.mPosition.mLongitude,
                                        stone.mLatitude, stone.mLongitude, res);
                                if (res[0] < mindist) {
                                    mindist = res[0];
                                    nearstone = stone;
                                }
                            }
                            if (nearstone != null) {
                                Log.i(TAG, "nearest stone = " + nearstone.mName);
                                pi.setStone(nearstone);
                            } else
                                Log.i(TAG, "stone not found");
                        }
                    }

                    Intent myIntent = new Intent(MainActivity.this, MapsActivity.class);
                    myIntent.putExtra("photo_info", PhotoGpsUtils.getPhotoInfoJson(photoInfoList));
                    //Log.i("Photo", "photo_info = " + getPhotoInfoJson(photoInfoList));
                    MainActivity.this.startActivity(myIntent);
                }
            }
        });
    }

    private ArrayList<File> getAllPhotoFilesInDir(File photoRootDir) {
        if(!photoRootDir.isDirectory())
            return null;

        File[] files = photoRootDir.listFiles();

        ArrayList<File> picFiles = new ArrayList<>();
        for(File f:files) {
            if(f.isDirectory()) {
                picFiles.addAll(getAllPhotoFilesInDir(f));
            } else {
                String ext = f.getName().substring(f.getName().lastIndexOf(".") + 1, f.getName().length());
                if (ext.compareToIgnoreCase("jpg") == 0) {
                    picFiles.add(f);
                }
            }
        }
        return picFiles;
    }

    private ArrayList<PhotoInfo> getPhotoInfoList(String path) {
        File photoRootDir = new File(path);
        ArrayList<File> picFiles = getAllPhotoFilesInDir(photoRootDir);
        if(picFiles == null)
            return null;

        ArrayList<PhotoInfo> photoInfoList = new ArrayList<>();
        for(File f:picFiles) {
            Log.i("Photo", "file = " + f.getName());
            try {
                ExifInterface exif = new ExifInterface(f.getPath());
                Log.i("Photo", "datetime = " + exif.getAttribute(ExifInterface.TAG_DATETIME));

                float[] latLong = new float[2];
                if(exif.getLatLong(latLong)) {
                    Log.i("Photo", "location = " + latLong[0] + ", " + latLong[1]);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                    Date date = (Date)sdf.parse(exif.getAttribute(ExifInterface.TAG_DATETIME));
                    Log.i("Photo", "timestamp = " + date.getTime());

                            /*
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inPurgeable = true;
                            options.inInputShareable = true;
                            options.inSampleSize = 32;
                            Bitmap bmp = null;
                            try {
                                bmp = BitmapFactory.decodeFile(f.getPath(), options);
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                            */

                    PhotoInfo pi = new PhotoInfo(latLong[0], latLong[1], (float)exif.getAltitude(0), date.getTime(), f.getAbsolutePath());
                    //pi.saveThumbnail();
                    photoInfoList.add(pi);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        Comparator comparator = new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                PhotoInfo pi1 = (PhotoInfo)o1;
                PhotoInfo pi2 = (PhotoInfo)o2;
                if(pi1.mTimestamp > pi2.mTimestamp)
                    return 1;
                else if(pi1.mTimestamp == pi2.mTimestamp)
                    return 0;
                else
                    return -1;
            }
        };
        if (Build.VERSION.SDK_INT >= 24)
            photoInfoList.sort(comparator);
        else
            Collections.sort(photoInfoList, comparator);

        return photoInfoList;
    }

}
