package com.km.twhikingapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by kaomin on 2/6/18.
 */

public class PhotoGpsUtils {
    private static final String TAG = "PhotoGpsUtils";

    public static class PhotoInfo {
        PositionInfo mPosition;
        PositionInfo mStone;
        long mTimestamp;
        String mPhotoFilePath;
        int mTripId;
        String mPhotoId;

        PhotoInfo(float lat, float lng, float altitude, long timestamp, String filepath) {
            mPosition = new PositionInfo(altitude, lat, lng, null);
            mTimestamp = timestamp;
            mPhotoFilePath = filepath;
        }

        Bitmap getThumbnail() {
            if (mPhotoFilePath == null) return null;

            Bitmap thumb = null;

            try {
                ExifInterface exif = new ExifInterface(mPhotoFilePath);
                if (Build.VERSION.SDK_INT >= 26) {
                    thumb = exif.getThumbnailBitmap();
                } else {
                    byte[] jpg = exif.getThumbnail();
                    if(jpg != null) {
                        thumb = BitmapFactory.decodeByteArray(jpg, 0, jpg.length);
                    }
                }

            } catch(Exception e){
                e.printStackTrace();
            }

            if(thumb == null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPurgeable = true;
                options.inInputShareable = true;
                options.inSampleSize = 32;
                try {
                    thumb = BitmapFactory.decodeFile(mPhotoFilePath, options);
                    Log.i(TAG, "thumb from jpg w = " + thumb.getWidth() + " h = " + thumb.getHeight());
                } catch(Exception e) {
                    e.printStackTrace();
                }
            } else {
                Log.i(TAG, "thumb from exif w = " + thumb.getWidth() + " h = " + thumb.getHeight());
            }

            return thumb;
        }


        /*
        void saveThumbnail() {
            Log.i("Photo", "bmp width = " + mThumbnail.getWidth() + " height = " + mThumbnail.getHeight());
            File file = new File(getCacheDir(), "thumb-"+mTimestamp+".jpg");
            Log.i("Photo", "file: " + file.getPath());
            try {
                FileOutputStream out = new FileOutputStream(file);
                mThumbnail.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        */

        void setTripId(int id) {
            mTripId = id;
        }

        void setStone(PositionInfo stone) {
            mStone = stone;
        }

        void setPhotoId(String id) {
            mPhotoId = id;
        }
    }

    public static class PositionInfo {
        float mLatitude;
        float mLongitude;
        float mAltitude;
        int mLevel;
        String mName;
        boolean mIsShown;
        Marker mMarker;

        PositionInfo(float altitude, float lat, float lng, String name) {
            mLatitude = lat;
            mLongitude = lng;
            mAltitude = altitude;
            mName = name;
            mMarker = null;
            //Log.i("Photo", "info = " + mName + " " + mLatitude + " " + mLongitude + " " + mAltitude);
        }

        void setLevel(int level) {
            mLevel = level;
        }
        boolean isSamePosition(PositionInfo other) {
            float[] res = new float[1];
            Location.distanceBetween(mLatitude, mLongitude, other.mLatitude, other.mLongitude, res);
            if(res[0] < 10) return true;
            else return false;
        }

        LatLng getLatlng() {
            return new LatLng(mLatitude, mLongitude);
        }

        void setMarker(Marker mark) {
            mMarker = mark;
        }
    }

    static String getPhotoInfoJson(ArrayList<PhotoInfo> piList) {
        JSONObject json = new JSONObject();

        try {
            json.put("photo_num", piList.size());
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(int i=0;i<piList.size();i++) {
            try {
                json.put("trip_id-"+i, piList.get(i).mTripId);
                json.put("stone_name-"+i, piList.get(i).mStone.mName);
                json.put("stone_lat-"+i, piList.get(i).mStone.mLatitude);
                json.put("stone_lng-"+i, piList.get(i).mStone.mLongitude);
                json.put("latitude-"+i, piList.get(i).mPosition.mLatitude);
                json.put("longitude-"+i, piList.get(i).mPosition.mLongitude);
                json.put("altitude-"+i, piList.get(i).mPosition.mAltitude);
                json.put("timestamp-"+i, piList.get(i).mTimestamp);
                json.put("file_path-"+i, piList.get(i).mPhotoFilePath);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        return json.toString();
    }

    static ArrayList<PhotoInfo> parseJsonToPhotoInfo(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            int photoNum = json.getInt("photo_num");
            ArrayList<PhotoInfo> pilist = new ArrayList<>();
            for(int i=0;i<photoNum;i++) {
                int tripId = json.getInt("trip_id-"+i);
                String stoneName = json.getString("stone_name-"+i);
                float stoneLat = (float)json.getDouble("stone_lat-"+i);
                float stoneLng = (float)json.getDouble("stone_lng-"+i);
                float lat = (float)json.getDouble("latitude-"+i);
                float lng = (float)json.getDouble("longitude-"+i);
                float alt = (float)json.getDouble("altitude-"+i);
                long ts = json.getLong("timestamp-"+i);
                String fpath = json.getString("file_path-"+i);
                PhotoInfo pi = new PhotoInfo(lat, lng, alt, ts, fpath);
                pi.setStone(new PositionInfo(0, stoneLat, stoneLng, stoneName));
                pi.setTripId(tripId);
                pilist.add(pi);
            }
            return pilist;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static ArrayList<PositionInfo> loadStonesInfo(InputStream isStoneAsset) {
        try {
            InputStreamReader is = new InputStreamReader(isStoneAsset);

            BufferedReader reader = new BufferedReader(is);
            //reader.readLine();
            String line;
            ArrayList<PositionInfo> stonesList = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                //Log.i("Photo", "line = " + line);
                String[] infos = line.split(",");
                //if(infos.length >= 5)
                //    Log.i("Photo", "name = " + infos[0] + " altitude = " + infos[1] + " latiude = " + infos[2] + " longitude = " + infos[3] + " level = " + infos[4]);
                //else if(infos.length >= 4)
                //    Log.i("Photo", "name = " + infos[0] + " altitude = " + infos[1] + " latiude = " + infos[2] + " longitude = " + infos[3]);

                if(infos.length >= 4) {
                    PositionInfo stone = new PositionInfo(Float.valueOf(infos[1]), Float.valueOf(infos[2]), Float.valueOf(infos[3]), infos[0]);
                    int level = 0;
                    if(infos.length >= 5) {
                        if(infos[4].equals("Ⅰ"))
                            level = 1;
                        else if(infos[4].equals("Ⅱ"))
                            level = 2;
                        else if(infos[4].equals("Ⅲ"))
                            level = 3;
                        else if(infos[4].equals("Ⅳ"))
                            level = 4;
                    }
                    stone.setLevel(level);
                    stonesList.add(stone);
                }
            }
            is.close();
            return stonesList;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Bitmap bmpRotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap bmpFlip(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
