package com.km.twhikingapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Locale;

import com.km.twhikingapp.PhotoGpsUtils.PhotoInfo;
import com.km.twhikingapp.PhotoGpsUtils.PositionInfo;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = "MapsActivity";
    private static final int MAP_STONE_MARKER_RANGE = 10000;
    private GoogleMap mMap;
    private ArrayList<PhotoInfo> mPhotoInfoList = null;
    ArrayList<PositionInfo> mStoneMarkList = null;
    private LocationManager mLocationManager;
    private LatLng mCurrCameraPosition = null;
    private MarkerUpdateThread mUpdateThread = null;
    private boolean mIsTouchDown = false;
    private Marker mCurrentMarker = null;
    private TouchableWrapper.OnTouchListener mMapTouchListener = new TouchableWrapper.OnTouchListener() {
        @Override
        public void onTouchUpListener() {
            mIsTouchDown = false;
        }

        @Override
        public void onTouchDownListener() {
            mIsTouchDown = true;
        }
    };

    private class MarkerUpdateThread {
        private Thread mThread;
        private LatLng mLatestPosition;
        Runnable mThreadRun = new Runnable() {
            @Override
            public void run() {
                if(mIsTouchDown)
                    return;

                if(mStoneMarkList== null || mStoneMarkList.isEmpty())
                    return;

                for (PositionInfo stone:mStoneMarkList) {
                    //if (stone.mMarker != null) continue;
                    LatLng ref = stone.getLatlng();
                    float[] dis = new float[1];
                    Location.distanceBetween(mLatestPosition.latitude, mLatestPosition.longitude, ref.latitude, ref.longitude, dis);

                    if (dis[0] < MAP_STONE_MARKER_RANGE) {
                        if(stone.mMarker == null) {
                            final PositionInfo newmarker = stone;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showStoneMarker(newmarker);
                                }
                            });
                        }
                    } else {
                        if(stone.mMarker != null) {
                            final PositionInfo newmarker = stone;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(newmarker.mMarker != null) {
                                        newmarker.mMarker.remove();
                                        newmarker.mMarker = null;
                                    }
                                }
                            });
                        }
                    }
                }
            }
        };

        MarkerUpdateThread(LatLng pos) {
            Log.i(TAG, "MarkerUpdateThread");
            mLatestPosition = pos;
            mThread = new Thread(mThreadRun);
            mThread.start();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapTouchFragment mapFragment = (SupportMapTouchFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mapFragment.setTouchListener(mMapTouchListener);
        mapFragment.setCurrentPositionListener(new SupportMapTouchFragment.OnCurrentPositonListener() {
            @Override
            public void onCurrentPosition() {
                Location loc = null;
                try {
                    loc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if(loc == null)
                        loc = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                } catch(SecurityException e) {
                    e.printStackTrace();
                }
                if(loc != null) {
                    LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(ll, 13);
                    mMap.animateCamera(cameraUpdate);
                }
            }
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String jsonStr = bundle.getString("photo_info");
            mPhotoInfoList = PhotoGpsUtils.parseJsonToPhotoInfo(jsonStr);
        } else {
            Log.i(TAG, "current position and find stone");
            try {
                mStoneMarkList = PhotoGpsUtils.loadStonesInfo(getAssets().open("stones.csv"));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        if(mPhotoInfoList != null) {
            mStoneMarkList = new ArrayList<>();
            for (PhotoInfo photo : mPhotoInfoList) {
                if (mStoneMarkList.isEmpty()) {
                    mStoneMarkList.add(photo.mStone);
                } else {
                    boolean isNew = true;
                    for (PositionInfo m : mStoneMarkList) {
                        if (m.isSamePosition(photo.mStone)) {
                            isNew = false;
                            break;
                        }
                    }
                    if (isNew) {
                        mStoneMarkList.add(photo.mStone);
                    }
                }
            }
        }
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(mLocationManager != null) {
            try {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
                        3, mLocationListener);
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000,
                        3, mLocationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }



    Bitmap readBmpIcon(long ts) {
        File file = new File(getCacheDir(), "thumb-"+ts+".jpg");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable = true;
        options.inInputShareable = true;
        try {
            return BitmapFactory.decodeFile(file.getPath(), options);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMarkerClickListener(mMarkClickListener);

        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                mCurrCameraPosition = mMap.getCameraPosition().target;
                if(!mIsTouchDown) {
                    if(mUpdateThread == null || mUpdateThread.mThread.getState() == Thread.State.TERMINATED)
                        mUpdateThread = new MarkerUpdateThread(mCurrCameraPosition);
                }
            }
        });

        LatLng start = new LatLng(23.46999192, 120.95726550); // init in Mt.Yi
        Location loc = null;
        if(mPhotoInfoList != null && !mPhotoInfoList.isEmpty()) {
            for(int i=0;i<mPhotoInfoList.size();i++) {
                LatLng ll = new LatLng(mPhotoInfoList.get(i).mPosition.mLatitude, mPhotoInfoList.get(i).mPosition.mLongitude);
                Marker marker = mMap.addMarker(new MarkerOptions().position(ll).title(mPhotoInfoList.get(i).mStone.mName).snippet("xxx")
                        .icon(BitmapDescriptorFactory.fromBitmap(mPhotoInfoList.get(i).getThumbnail())));
                //marker.setAlpha(0.5f);
                mPhotoInfoList.get(i).setPhotoId(marker.getId());
            }
            start = new LatLng(mPhotoInfoList.get(0).mStone.mLatitude, mPhotoInfoList.get(0).mStone.mLongitude);

            if(mStoneMarkList != null && !mStoneMarkList.isEmpty()) {
                for(PositionInfo stone:mStoneMarkList) {
                    showStoneMarker(stone);
                }
            }
        } else {
            try {
                loc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(loc == null)
                    loc = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            } catch(SecurityException e) {
                e.printStackTrace();
            }
        }

        //loc = null;

        if(loc != null) {
            Log.i(TAG, "final = " + loc.getLatitude());
            start = new LatLng(loc.getLatitude(), loc.getLongitude());
            //mMap.addMarker(new MarkerOptions().position(start).title("current"));
        }

        mCurrCameraPosition = start;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(start, 13), new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                Log.i(TAG, "onCameraMove onFinish");
            }

            @Override
            public void onCancel() {
                Log.i(TAG, "onCameraMove onCancel");
            }
        });

        //mMap.addMarker(new MarkerOptions().position(mt7Star).title("Mt. 7-star"));
        //LatLng mt7StarEast = new LatLng(25.1790, 121.5557);
        //mMap.addMarker(new MarkerOptions().position(mt7StarEast).title("Mt. 7-star East"));
        //LatLng mtDaiTu = new LatLng(25.1767, 121.5220);
        //mMap.addMarker(new MarkerOptions().position(mtDaiTu).title("Mt. Dai Tu"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(mt7Star));
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.i(TAG, "onLocationChanged");
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 13));
            Log.i(TAG, "onLocationChanged location = " + ll.latitude + ", " + ll.longitude);
            if(mCurrentMarker == null) {
                mCurrentMarker = mMap.addMarker(new MarkerOptions().position(ll).title("current")
                        .icon(BitmapDescriptorFactory.fromResource(R.raw.current)));
                //Log.i(TAG, "current z index = " + mCurrentMarker.getZIndex());
                mCurrentMarker.setZIndex(1000);
                //CircleOptions circleOptions = new CircleOptions()
                //        .center(ll)
                //        .radius(200)
                //        .fillColor(Color.RED).strokeColor(Color.RED); // In meters
                //Circle circle = mMap.addCircle(circleOptions);
                //circle.setZIndex(1000);

            } else {
                mCurrentMarker.setPosition(ll);
            }

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(ll, 13);
            mMap.animateCamera(cameraUpdate);

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            Log.i(TAG, "onStatusChanged s = " + s);
        }

        @Override
        public void onProviderEnabled(String s) {
            Log.i(TAG, "onProviderEnabled s = " + s);
        }

        @Override
        public void onProviderDisabled(String s) {
            Log.i(TAG, "onProviderDisabled s = " + s);
        }
    };

    private void showStoneMarker(PositionInfo stone) {
        Log.i(TAG, "showStoneMarker " + stone.mName);
        int logoid = R.raw.triangle;
        if(stone.mLevel == 1)
            logoid = R.raw.triangle_red;
        else if(stone.mLevel == 2)
            logoid = R.raw.triangle_blue;
        else if(stone.mLevel == 3)
            logoid = R.raw.triangle_green;

        stone.mMarker = mMap.addMarker(new MarkerOptions().position(stone.getLatlng()).title(stone.mName + "/" + (int)stone.mAltitude + "m")
                .icon(BitmapDescriptorFactory.fromResource(logoid)));
    }

    private GoogleMap.OnMarkerClickListener mMarkClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            Log.i(TAG, "marker pos = " + marker.getPosition().latitude + ", " + marker.getPosition().longitude);
            String id = marker.getId();
            if(mPhotoInfoList != null) {
                for (PhotoInfo photo : mPhotoInfoList) {
                    if (photo.mPhotoId.equals(id)) {
                        Log.i(TAG, "open photo " + photo.mPhotoFilePath);

                        Intent myIntent = new Intent(MapsActivity.this, PhotoShowActivity.class);
                        myIntent.putExtra("photo_path", photo.mPhotoFilePath);
                        MapsActivity.this.startActivity(myIntent);
                        break;
                    }
                }
            }
            return false;
        }
    };
}
