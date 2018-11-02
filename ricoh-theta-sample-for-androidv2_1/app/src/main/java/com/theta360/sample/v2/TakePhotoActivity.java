package com.theta360.sample.v2;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.theta360.sample.v2.model.ImageSize;
import com.theta360.sample.v2.network.HttpConnector;
import com.theta360.sample.v2.network.HttpDownloadListener;
import com.theta360.sample.v2.network.HttpEventListener;
import com.theta360.sample.v2.network.ImageData;
import com.theta360.sample.v2.network.ImageInfo;
import com.theta360.sample.v2.view.MJpegInputStream;
import com.theta360.sample.v2.view.MJpegView;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Activity that displays the photo list
 */
public class TakePhotoActivity extends Activity {
    //private ListView objectList;
    //private LogView logViewer;
    private String cameraIpAddress;

    private LinearLayout layoutCameraArea;
    private Button btnShoot;
    private Button btnLoad;
    private TextView textCameraStatus;
    private ImageSize currentImageSize;
    private MJpegView mMv;
    private boolean mConnectionSwitchEnabled = false;
    final Handler handler = new Handler();

    private ShowLiveViewTask livePreviewTask = null;
    private GetImageSizeTask getImageSizeTask = null;

    private Set<String> oldFileListInCamera = null;

    private boolean IsTakePhotoByAndroid = true;


    /**
     * onCreate Method
     * @param savedInstanceState onCreate Status value
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);

        //	logViewer = (LogView) findViewById(R.id.log_view);
        cameraIpAddress = getResources().getString(R.string.theta_ip_address);
        getActionBar().setTitle("Take a Photo");

        layoutCameraArea = (LinearLayout) findViewById(R.id.shoot_area);
        textCameraStatus = (TextView) findViewById(R.id.camera_status);
        btnShoot = (Button) findViewById(R.id.btn_shoot);
        btnShoot.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                btnShoot.setEnabled(false);
                textCameraStatus.setText(R.string.text_camera_synthesizing);

                new ShootTask().execute();
            }
        });

        btnLoad = (Button) findViewById(R.id.btn_load);
        btnLoad.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                btnLoad.setEnabled(false);
                textCameraStatus.setText(R.string.text_camera_synthesizing);


                IsTakePhotoByAndroid = false;
                Log.d("debug","aaaaaaaaaaa");
                livePreviewTask = null;
                Log.d("debug","aaaaaaaaaaa");
                getImageSizeTask = null;
                Log.d("debug","aaaaaaaaaaa");
                //mMv.stopPlay();
                Log.d("debug","aaaaaaaaaaa");
                new LoadTask().execute();
            }
        });

        mMv = (MJpegView) findViewById(R.id.live_view);

        forceConnectToWifi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMv.stopPlay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMv.play();

        if (livePreviewTask != null) {
            livePreviewTask.cancel(true);
            livePreviewTask = new ShowLiveViewTask();
            livePreviewTask.execute(cameraIpAddress);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GLPhotoActivity.REQUEST_REFRESH_LIST) {
            /*
            if (sampleTask != null) {
                sampleTask.cancel(true);
            }
            sampleTask = new LoadObjectListTask();
            sampleTask.execute();
            */
        }
    }

    @Override
    protected void onDestroy() {
        /*
        if (sampleTask != null) {
            sampleTask.cancel(true);
        }
*/
        if (livePreviewTask != null) {
            livePreviewTask.cancel(true);
        }

        if (getImageSizeTask != null) {
            getImageSizeTask.cancel(true);
        }

        super.onDestroy();
    }

    /**
     * onCreateOptionsMenu Method
     * @param menu Menu initialization object
     * @return Menu display feasibility status value
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.connection, menu);

        Switch connectionSwitch = (Switch) menu.findItem(R.id.connection).getActionView().findViewById(R.id.connection_switch);
        connectionSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    layoutCameraArea.setVisibility(View.VISIBLE);
/*
                    if (sampleTask == null) {
                        sampleTask = new LoadObjectListTask();
                        sampleTask.execute();
                    }
*/
                    if (livePreviewTask == null) {
                        livePreviewTask = new ShowLiveViewTask();
                        livePreviewTask.execute(cameraIpAddress);
                    }

                    if (getImageSizeTask == null) {
                        getImageSizeTask = new GetImageSizeTask();
                        getImageSizeTask.execute();
                    }

                    new checkCameraImageList().execute();

                } else {
                    layoutCameraArea.setVisibility(View.INVISIBLE);
/*
                    if (sampleTask != null) {
                        sampleTask.cancel(true);
                        sampleTask = null;
                    }
*/
                    if (livePreviewTask != null) {
                        livePreviewTask.cancel(true);
                        livePreviewTask = null;
                    }

                    if (getImageSizeTask != null) {
                        getImageSizeTask.cancel(true);
                        getImageSizeTask = null;
                    }

                    new DisConnectTask().execute();

                    mMv.stopPlay();
                }
            }
        });
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Switch connectionSwitch = (Switch) menu.findItem(R.id.connection).getActionView().findViewById(R.id.connection_switch);
        if (!mConnectionSwitchEnabled) {
            connectionSwitch.setChecked(false);
        }
        connectionSwitch.setEnabled(mConnectionSwitchEnabled);
        return true;
    }

    private class checkCameraImageList extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... v){
            Log.d("debug","*** Start checkCameraImageList ***");
            HttpConnector camera = new HttpConnector(cameraIpAddress);
            ArrayList<ImageInfo> imageInfoList = camera.getList();
            Set<String> latestFileListInCamera = new HashSet<String>(0);

            for (ImageInfo imageInfo : imageInfoList){
                latestFileListInCamera.add(imageInfo.getFileId());
            }
            oldFileListInCamera = latestFileListInCamera;
            Log.d("debug","*** End checkCameraImageList ***");
            return null;
        }
    }


    private class LoadTask extends AsyncTask<Void, Void, Void> {

        boolean result;

        @Override
        protected Void doInBackground(Void... v){

            HttpConnector camera = new HttpConnector(cameraIpAddress);
            ArrayList<ImageInfo> imageInfoList = camera.getList();
            Set<String> latestFileListInCamera = new HashSet<String>(0);

            for (ImageInfo imageInfo : imageInfoList){
                latestFileListInCamera.add(imageInfo.getFileId());
            }
            if( oldFileListInCamera.size() == latestFileListInCamera.size() ){
                result = false;
                Log.d("debug","camera内データに変化なし");
                return null;
            }else if(oldFileListInCamera.size() < latestFileListInCamera.size() ){
                result = true;
                Log.d("debug","camera内データに変化あり");
            }

            for (String fileid : latestFileListInCamera){
                if ( !oldFileListInCamera.contains(fileid) ){
                    new GetThumbnailTask(fileid).execute();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v){

        }

    }


    private class GetImageSizeTask extends AsyncTask<Void, String, ImageSize> {

        @Override
        protected ImageSize doInBackground(Void... params) {
            publishProgress("get current image size");
            HttpConnector camera = new HttpConnector(cameraIpAddress);
            ImageSize imageSize = camera.getImageSize();
            Log.d("debug",imageSize.toString());
            return imageSize;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //for (String log : values) {
            //	logViewer.append(log);
            //}
        }

        @Override
        protected void onPostExecute(ImageSize imageSize) {
            if (imageSize != null) {
                //	logViewer.append("new image size: " + imageSize.name());
                currentImageSize = imageSize;
                //} else {
                //	logViewer.append("failed to get image size");
            }
        }
    }

    private class ShowLiveViewTask extends AsyncTask<String, String, MJpegInputStream> {
        @Override
        protected MJpegInputStream doInBackground(String... ipAddress) {
            MJpegInputStream mjis = null;
            final int MAX_RETRY_COUNT = 20;

            for (int retryCount = 0; retryCount < MAX_RETRY_COUNT; retryCount++) {
                try {
                    publishProgress("start Live view");
                    HttpConnector camera = new HttpConnector(ipAddress[0]);
                    InputStream is = camera.getLivePreview();
                    mjis = new MJpegInputStream(is);
                    retryCount = MAX_RETRY_COUNT;
                } catch (IOException e) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } catch (JSONException e) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }

            return mjis;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //for (String log : values) {
            //logViewer.append(log);
            //}
        }

        @Override
        protected void onPostExecute(MJpegInputStream mJpegInputStream) {
            if (mJpegInputStream != null) {
                mMv.setSource(mJpegInputStream);
                //} else {
                //logViewer.append("failed to start live view");
            }
        }
    }

    private class DisConnectTask extends AsyncTask<Void, String, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                publishProgress("disconnected.");
                return true;

            } catch (Throwable throwable) {
                String errorLog = Log.getStackTraceString(throwable);
                publishProgress(errorLog);
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //for (String log : values) {
            //	logViewer.append(log);
            //}
        }
    }

    //  Shoot Buttonが押された時に実行される非同期処理
    private class ShootTask extends AsyncTask<Void, Void, HttpConnector.ShootResult> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected HttpConnector.ShootResult doInBackground(Void... params) {
            mMv.setSource(null);
            CaptureListener postviewListener = new CaptureListener();
            HttpConnector camera = new HttpConnector(getResources().getString(R.string.theta_ip_address));
            HttpConnector.ShootResult result = camera.takePicture(postviewListener);
            return result;
        }

        @Override
        protected void onPostExecute(HttpConnector.ShootResult result) {
            if (result == HttpConnector.ShootResult.FAIL_CAMERA_DISCONNECTED) {
                //	logViewer.append("takePicture:FAIL_CAMERA_DISCONNECTED");
            } else if (result == HttpConnector.ShootResult.FAIL_STORE_FULL) {
                //	logViewer.append("takePicture:FAIL_STORE_FULL");
            } else if (result == HttpConnector.ShootResult.FAIL_DEVICE_BUSY) {
                //	logViewer.append("takePicture:FAIL_DEVICE_BUSY");
            } else if (result == HttpConnector.ShootResult.SUCCESS) {
                //	logViewer.append("takePicture:SUCCESS");
            }
        }

        private class CaptureListener implements HttpEventListener {
            private String latestCapturedFileId;
            private boolean ImageAdd = false;

            @Override
            public void onCheckStatus(boolean newStatus) {
                //if(newStatus) {
                //	appendLogView("takePicture:FINISHED");
                //} else {
                //	appendLogView("takePicture:IN PROGRESS");
                //}
            }

            @Override
            public void onObjectChanged(String latestCapturedFileId) {
                this.ImageAdd = true;
                this.latestCapturedFileId = latestCapturedFileId;
                //	appendLogView("ImageAdd:FileId " + this.latestCapturedFileId);
            }

            @Override
            public void onCompleted() {
                //	appendLogView("CaptureComplete");
                if (ImageAdd) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnShoot.setEnabled(true);
                            btnLoad.setEnabled(true);
                            textCameraStatus.setText(R.string.text_camera_standby);
                            new GetThumbnailTask(latestCapturedFileId).execute();
                        }
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                //	appendLogView("CaptureError " + errorMessage);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnShoot.setEnabled(true);
                        btnLoad.setEnabled(true);
                        textCameraStatus.setText(R.string.text_camera_standby);
                    }
                });
            }
        }

    }

    private class GetThumbnailTask extends AsyncTask<Void, String, Void> {

        private String fileId;
        byte[] thumbnailImage;

        public GetThumbnailTask(String fileId) {
            this.fileId = fileId;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d("debug","*** Start GetThumbnailTask *** ");
            HttpConnector camera = new HttpConnector(getResources().getString(R.string.theta_ip_address));
            Bitmap thumbnail = camera.getThumb(fileId);
            if (thumbnail != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                thumbnailImage = baos.toByteArray();

                //360画像の保存
                ImageData imageData = camera.getImage(fileId, new HttpDownloadListener() {
                    @Override
                    public void onTotalSize(long totalSize) {
                    }
                    @Override
                    public void onDataReceived(int size) {
                    }
                });
                camera = null;

                // 360°画像の保存
                MyFileAccess myFileAccess = new MyFileAccess(fileId);
                myFileAccess.mkdirImage2D();

                Bitmap image360 = BitmapFactory.decodeByteArray(imageData.getRawData(), 0, imageData.getRawData().length);
                myFileAccess.storeImage360(image360);

                // サムネイル画像の保存
                myFileAccess.storeThumbnail(thumbnail);

                // pitch,roll,yawの保存
                myFileAccess.storeInfo(imageData.getPitch(),imageData.getRoll(),imageData.getYaw());


            } else {
                publishProgress("failed to get file data.");
            }
            Log.d("debug","*** End GetThumbnailTask *** ");
            return null;
        }

        @Override
        protected void onPostExecute(Void values) {
            if( IsTakePhotoByAndroid ) {
                GLPhotoActivity.startActivityForResult(TakePhotoActivity.this, cameraIpAddress, fileId, thumbnailImage, true);
            }
            else{
                Log.d("debug","restart");
                //onDestroy();
                reload();
                Log.d("debug","restart");
                GLPhotoActivity.startActivityForResult(TakePhotoActivity.this, cameraIpAddress, fileId, thumbnailImage, true);
            }
        }

        private void reload() {
            Intent intent = getIntent();
            overridePendingTransition(0, 0);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();

            overridePendingTransition(0, 0);
            startActivity(intent);
        }

        @Override
        protected void onProgressUpdate(String... values) {

        }
    }

    /**
     * Force this applicatioin to connect to Wi-Fi
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void forceConnectToWifi() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if ((info != null) && info.isAvailable()) {
                NetworkRequest.Builder builder = new NetworkRequest.Builder();
                builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
                NetworkRequest requestedNetwork = builder.build();

                ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);

                        ConnectivityManager.setProcessDefaultNetwork(network);
                        mConnectionSwitchEnabled = true;
                        invalidateOptionsMenu();
                        //				appendLogView("connect to Wi-Fi AP");
                    }

                    @Override
                    public void onLost(Network network) {
                        super.onLost(network);

                        mConnectionSwitchEnabled = false;
                        invalidateOptionsMenu();
                        //	appendLogView("lost connection to Wi-Fi AP");
                    }
                };

                cm.registerNetworkCallback(requestedNetwork, callback);
            }
        } else {
            mConnectionSwitchEnabled = true;
            invalidateOptionsMenu();
        }
    }
}