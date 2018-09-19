package com.theta360.sample.v2;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

import com.theta360.sample.v2.model.ImageSize;
import com.theta360.sample.v2.network.HttpConnector;
import com.theta360.sample.v2.network.HttpDownloadListener;
import com.theta360.sample.v2.network.HttpEventListener;
import com.theta360.sample.v2.network.ImageData;
//import com.theta360.sample.v2.view.LogView;
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


/**
 * Activity that displays the photo list
 */
public class TakePhotoActivity extends Activity {
    //private ListView objectList;
    //private LogView logViewer;
    private String cameraIpAddress;

    private LinearLayout layoutCameraArea;
    private Button btnShoot;
    private TextView textCameraStatus;
    private ImageSize currentImageSize;
    private MJpegView mMv;
    private boolean mConnectionSwitchEnabled = false;

    //private LoadObjectListTask sampleTask = null;
    private ShowLiveViewTask livePreviewTask = null;
    private GetImageSizeTask getImageSizeTask = null;


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
                //objectList = (ListView) findViewById(R.id.object_list);
                //ImageListArrayAdapter empty = new ImageListArrayAdapter(TakePhotoActivity.this, R.layout.listlayout_object, new ArrayList<ImageRow>());
                //objectList.setAdapter(empty);

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
            //	logViewer.append("takePicture");
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
                        textCameraStatus.setText(R.string.text_camera_standby);
                    }
                });
            }
        }

    }

    private class GetThumbnailTask extends AsyncTask<Void, String, Void> {

        private String fileId;

        public GetThumbnailTask(String fileId) {
            this.fileId = fileId;
        }

        @Override
        protected Void doInBackground(Void... params) {
            HttpConnector camera = new HttpConnector(getResources().getString(R.string.theta_ip_address));
            Bitmap thumbnail = camera.getThumb(fileId);
            if (thumbnail != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] thumbnailImage = baos.toByteArray();

                // サムネイル、360画像の保存
                StoreFileTask storeFileTask = new StoreFileTask();
                storeFileTask.execute(fileId);

                GLPhotoActivity.startActivityForResult(TakePhotoActivity.this, cameraIpAddress, fileId, thumbnailImage, true);
            } else {
                publishProgress("failed to get file data.");
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //for (String log : values) {
            //	logViewer.append(log);
            //}
        }
    }

    /********************
     Input : fileid
     Task : 360°画像、サムネイル、(pitch, roll, yaw)をファイル保存
     instakePhotos Task起動
     ********************/
    private class StoreFileTask extends AsyncTask<String, Object, Void> {

        protected Void doInBackground(String ... fileId){

            Log.d("debug","StoreFileTask Start");
            Log.d("debug",fileId[0]);

            String status = Environment.getExternalStorageState();
            String Path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/image";
            File Dir = new File(Path);
            String thumbnailPath = Path + "/thumbnail/";
            File thumbnailDir = new File(thumbnailPath);
            String image360Path = Path + "/image360/";
            File image360Dir = new File(image360Path);
            String infoPath = Path + "/info/";
            File infoDir = new File(infoPath);

            if(status.equals(Environment.MEDIA_MOUNTED)){
                //ディレクトリがなければ作成する
                if(!Dir.exists()){
                    Dir.mkdir();
                }
                if(!thumbnailDir.exists()){
                    thumbnailDir.mkdir();
                }
                if(!image360Dir.exists()){
                    image360Dir.mkdir();
                }
                if(!infoDir.exists()){
                    infoDir.mkdir();
                }
            }else{
                Log.d("debug","外部ストレージなし");
                return null;
            }

            //  ファイル名は、sampleのものを流用。
            //  接頭辞を付けて区別
            String fname = getfilename(fileId[0]);
            String thumbnail_fname = thumbnailDir.getAbsolutePath() + "/thumbnail_" + fname;
            String image360_fname = image360Dir.getAbsolutePath() + "/image360_" + fname;
            String info_fname = infoDir.getAbsolutePath() + "/info_" + fname;
            info_fname = info_fname.substring(0,info_fname.length()-3) + "txt";

            HttpConnector camera = new HttpConnector(getResources().getString(R.string.theta_ip_address));

            // Thumbnail画像の保存
            Bitmap thumbnail = camera.getThumb(fileId[0]);
            try{
                FileOutputStream outstream =new FileOutputStream(thumbnail_fname);
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, outstream);
                outstream.flush();//ファイルとして出力
                outstream.close();//使ったらすぐに閉じる
                Log.d("debug","サムネイル保存完了:" + thumbnail_fname);
            }catch(IOException ie){
                Log.d("debug","サムネイル保存不可");
            }

            //360画像の保存
            ImageData imageData = camera.getImage(fileId[0], new HttpDownloadListener() {
                @Override
                public void onTotalSize(long totalSize) {

                }

                @Override
                public void onDataReceived(int size) {

                }
            });
            Bitmap image360 = BitmapFactory.decodeByteArray(imageData.getRawData(), 0, imageData.getRawData().length);
            try{
                FileOutputStream outstream =new FileOutputStream(image360_fname);
                image360.compress(Bitmap.CompressFormat.JPEG, 100, outstream);
                outstream.flush();//ファイルとして出力
                outstream.close();//使ったらすぐに閉じる
                Log.d("debug","360画像保存完了:" + image360_fname);
            }catch(IOException ie){
                Log.d("debug","360画像保存不可");
            }

            // pitch,roll,yawの保存
            String pitch_s = BigDecimal.valueOf(imageData.getPitch()).toPlainString();
            String roll_s = BigDecimal.valueOf(imageData.getRoll()).toPlainString();
            String yaw_s = BigDecimal.valueOf(imageData.getYaw()).toPlainString();

            try {
                // FileWriterクラスのオブジェクトを生成する
                FileWriter file = new FileWriter(info_fname);
                // PrintWriterクラスのオブジェクトを生成する
                PrintWriter pw = new PrintWriter(new BufferedWriter(file));
                //ファイルに書き込む
                pw.println(pitch_s);
                pw.println(roll_s);
                pw.println(yaw_s);

                //ファイルを閉じる
                pw.close();
                Log.d("debug","pitch, roll, yaw:出力完了" + info_fname);
                Log.d( "debug","(pitch,roll,yaw) = (" + pitch_s + ", " + roll_s + ", " + yaw_s );
            } catch (IOException e) {
                Log.d("debug","pitch, roll, yaw出力不可");
                e.printStackTrace();
            }

            // 写真切り抜きタスク実行
            InstakePhotos instakePhotos = new InstakePhotos();
            instakePhotos.execute(fileId[0]);

            return null;
        }

        public String getfilename(String fileId){
            String[] list = fileId.split("/",0);
            String fname = list[list.length-1];
            return fname;
        }

        public ArrayList<Long> getPitchRollYaw(String infofile) {

            ArrayList<Long> PitchRollYaw = new ArrayList<Long>(3);



            return PitchRollYaw;
        };
    }

    private class InstakePhotos extends AsyncTask<String, Object, Void> {

        @Override
        protected Void doInBackground(String... fileId) {
            Log.d("debug","*** Start instakePhotos Task ***");
            Log.d("debug",fileId[0]);

            // 360度画像　読み出し
            String image360_fname = getfilename(fileId[0]);
            Log.d("debug",image360_fname);
            image360_fname = Environment.getExternalStorageDirectory().getAbsolutePath() + "/image/image360/image360_" + image360_fname;
            Log.d("debug",image360_fname);

            File image360_file = new File(image360_fname);
            byte[] byteArray = convertFile(image360_file);
            Log.d("debug","Load convert file");
            Log.d("debug",Integer.toString(byteArray.length));
            Bitmap image360_bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            Log.d("debug","Complete Load file");

            //画像のpitch,roll,yawを取得する
            String info_image360_fname = Environment.getExternalStorageDirectory().getAbsolutePath() + "/image/info/info_" + getfilename(fileId[0]);
            info_image360_fname = info_image360_fname.substring(0,info_image360_fname.length()-3) + "txt";
            Log.d("debug",info_image360_fname);
            double[] pitch_roll_yaw = readFileTodoubles(info_image360_fname);

            // 2D画像切り取り
            CuttingImage  cutter = new CuttingImage();
            //Bitmap image2d_bitmap = cutter.cut(image360_bitmap,0.8,0.8,0,0);
            Bitmap [] image2d_bitmap_a =new Bitmap[50];
            image2d_bitmap_a = cutter.cut(image360_bitmap,pitch_roll_yaw,1.0,1.0,2 * Math.PI / 10,Math.PI /2/4);
            Log.d("debug","Complete cut image");

            ArrayList<Integer> output_photos = new ArrayList();
            output_photos.add(0);
            output_photos.add(5);
            output_photos.add(10);
            output_photos.add(15);
            output_photos.add(20);
            output_photos.add(24);

            Log.d("debug","Complete cut image");

            // 2D画像保存先フォルダ
            String status = Environment.getExternalStorageState();
            // dir 生成
            String image2dPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/image/image2d/" + getfilename(fileId[0]);
            image2dPath = image2dPath.substring(0,image2dPath.length()-4);
            Log.d("debug","2d写真保存先："+image2dPath);
            File image2dDir = new File(image2dPath);
            if(status.equals(Environment.MEDIA_MOUNTED)){
                //ディレクトリがなければ作成する
                if(!image2dDir.exists()){
                    image2dDir.mkdir();
                }
            }else{
                Log.d("debug","外部ストレージなし");
                return null;
            }

            //2D画像の保存
            for(int index : output_photos) {
                String image2d_fname = image2dDir.getAbsolutePath() + "/image2d_" +Integer.toString(index)+ getfilename(fileId[0]);
                File image2d_file = new File(image2d_fname);
                try {
                    FileOutputStream outstream = new FileOutputStream(image2d_file);
                    image2d_bitmap_a[index].compress(Bitmap.CompressFormat.JPEG, 100, outstream);
                    outstream.flush();//ファイルとして出力
                    outstream.close();//使ったらすぐに閉じる
                    Log.d("debug", "2D画像保存完了:" + image2d_file);
                } catch (IOException ie) {
                    Log.d("debug", "2D画像保存不可");
                }
            }
            Log.d("debug", "*** END instakePhotos Task ***");
            return null;
        }

        //pitch,roll,yawのデータ読み込み用
        public double[] readFileTodoubles(String filePath) {
            FileReader fr = null;
            BufferedReader br = null;
            double[] res = new double[3];
            try {
                fr = new FileReader(filePath);
                br = new BufferedReader(fr);

                String line;
                for (int i=0; i<3; i++) {
                    if((line = br.readLine()) != null) {
                        res[i] = Double.parseDouble(line);
                    }else{
                        Log.d("debug","pitch,roll,yawファイルが壊れています。");
                    }
                }
                br.close();
                fr.close();
                return res;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        public String getfilename(String fileId){

            String[] list = fileId.split("/",0);
            String fname = list[list.length-1];
            return fname;
        }

        public byte[] convertFile(File file) {
            try (FileInputStream inputStream = new FileInputStream(file);) {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                while(true) {
                    int len = inputStream.read(buffer);
                    if(len < 0) {
                        break;
                    }
                    bout.write(buffer, 0, len);
                }
                return bout.toByteArray();
            } catch (Exception e) {
                Log.d("debug","CANNOT OPEN FILE:"+file.getAbsolutePath());
                e.printStackTrace();
            }
            return null;
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

    private class LoadPhotoTask extends AsyncTask<Void, Object, ImageData> {

        //private LogView logViewer;
        private ProgressBar progressBar;
        private String cameraIpAddress;
        private String fileId;
        private long fileSize;
        private long receivedDataSize = 0;

        public LoadPhotoTask(String cameraIpAddress, String fileId) {
            //    this.logViewer = (LogView) findViewById(R.id.photo_info);
            this.progressBar = (ProgressBar) findViewById(R.id.loading_photo_progress_bar);
            this.cameraIpAddress = cameraIpAddress;
            this.fileId = fileId;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected ImageData doInBackground(Void... params) {
            try {
                publishProgress("start to download image" + fileId);
                HttpConnector camera = new HttpConnector(cameraIpAddress);
                ImageData resizedImageData = camera.getImage(fileId, new HttpDownloadListener() {
                    @Override
                    public void onTotalSize(long totalSize) {
                        fileSize = totalSize;
                    }

                    @Override
                    public void onDataReceived(int size) {
                        receivedDataSize += size;

                        if (fileSize != 0) {
                            int progressPercentage = (int) (receivedDataSize * 100 / fileSize);
                            publishProgress(progressPercentage);
                        }
                    }
                });
                publishProgress("finish to download");

                return resizedImageData;

            } catch (Throwable throwable) {
                String errorLog = Log.getStackTraceString(throwable);
                publishProgress(errorLog);
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            for (Object param : values) {
                if (param instanceof Integer) {
                    progressBar.setProgress((Integer) param);
                } else if (param instanceof String) {
                    //      logViewer.append((String) param);
                }
            }
        }

        @Override
        protected void onPostExecute(ImageData imageData) {
            if (imageData != null) {

                byte[] dataObject = imageData.getRawData();

                if (dataObject == null) {
                    //  logViewer.append("failed to download image");
                    return;
                }

                Bitmap __bitmap = BitmapFactory.decodeByteArray(dataObject, 0, dataObject.length);

                progressBar.setVisibility(View.GONE);

                Double yaw = imageData.getYaw();
                Double pitch = imageData.getPitch();
                Double roll = imageData.getRoll();
                File dir = new File(Environment.getExternalStorageDirectory().getPath()+"/Pictures/");
                if(dir.exists()){
                    File file = new File(dir.getAbsolutePath()+"/turbans.jpg");
                    if (file.exists()) {
                        Bitmap bm = BitmapFactory.decodeFile(file.getPath());

                    }else{
                        Log.d("debug"," No File"+dir);
                    }
                }

                //  logViewer.append("<Angle: yaw=" + yaw + ", pitch=" + pitch + ", roll=" + roll + ">");
            } else {
                //    logViewer.append("failed to download image");
            }
        }
    }

}