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
import java.util.List;
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
    private TextView textCameraStatus;
    private ImageSize currentImageSize;
    private MJpegView mMv;
    private boolean mConnectionSwitchEnabled = false;
    final Handler handler = new Handler();
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

                //360画像の保存
                ImageData imageData = camera.getImage(fileId, new HttpDownloadListener() {
                    @Override
                    public void onTotalSize(long totalSize) {
                    }
                    @Override
                    public void onDataReceived(int size) {
                    }
                });

                // 360°画像の保存
                MyFileAccess myFileAccess = new MyFileAccess(fileId);
                Bitmap image360 = BitmapFactory.decodeByteArray(imageData.getRawData(), 0, imageData.getRawData().length);
                myFileAccess.storeImage360(image360);

                // サムネイル画像の保存
                myFileAccess.storeThumbnail(thumbnail);

                // pitch,roll,yawの保存
                myFileAccess.storeInfo(imageData.getPitch(),imageData.getRoll(),imageData.getYaw());

                // 写真切り抜きタスク実行
                InstakePhotos instakePhotos = new InstakePhotos();
                instakePhotos.execute(fileId);

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

            Log.d("debug","*** StoreFileTask Start *** ");

            HttpConnector camera = new HttpConnector(getResources().getString(R.string.theta_ip_address));

            MyFileAccess myFileAccess = new MyFileAccess(fileId[0]);

            // Thumbnail画像の保存
            Bitmap thumbnail = camera.getThumb(fileId[0]);
            myFileAccess.storeThumbnail(thumbnail);

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
            myFileAccess.storeImage360(image360);

            // pitch,roll,yawの保存
            myFileAccess.storeInfo(imageData.getPitch(),imageData.getRoll(),imageData.getYaw());




            return null;
        }

        @Override
        protected void onPostExecute(Void  v)  {
            Toast toast = Toast.makeText(TakePhotoActivity.this, "Complete Store Files", Toast.LENGTH_LONG);
            toast.show();
        }

    }

    private class InstakePhotos extends AsyncTask<String, Object, Void> {

        @Override
        protected void onPreExecute() {

        }


        @Override
        protected Void doInBackground(String... fileId) { // fileId: /***/image/image360/image360_R0001275.JPG
            Log.d("debug","*** Start instakePhotos Task ***");
            Log.d("debug",fileId[0]);

            MyFileAccess myFileAccess = new MyFileAccess(fileId[0]);

            // 360画像読み出し
            Bitmap image360_bitmap = myFileAccess.getBitmapImage360();
            Log.d("debug","Complete image360 Bitmap :" + myFileAccess.image360);

            // pitch,roll,yaw情報取得
            double[] pitch_roll_yaw = myFileAccess.getImageInfo();

            // 2D画像切り取り
            CuttingImage cutter = new CuttingImage();
            ArrayList<Bitmap> image2d_bitmap_a;

            image2d_bitmap_a = cutter.cut(image360_bitmap, pitch_roll_yaw,0.5, 0.5, 0,null,150);
            Log.d("debug","Complete cut image");


            //TODO 上位何個を出力するか定数として一か所で定義するか検討
            int output_num = 3; //<変更>

            //TODO モデル読み込みは事前の実施が可能(位置変更可)
            //モデル読み込み
            MyTensorFlow myTensorFlow = new MyTensorFlow();
            myTensorFlow.modelCreate();

            //TODO ビットマップの数と合わせる
            //切り取った画像を認識してinstabaeを抽出(resultsに代入)
            for(int i = 0; i < image2d_bitmap_a.size(); i++){
                myTensorFlow.startRecognition(image2d_bitmap_a.get(i), Integer.toString(i));
            }

            //resultsを認識率の高い順にソート
            Collections.sort(myTensorFlow.results_food, new Classifier.ScoreCmp());
            Collections.sort(myTensorFlow.results_human, new Classifier.ScoreCmp());

            //識別率の高い画像の番号を保存
            ArrayList<Integer> output_photos_food = new ArrayList();
            ArrayList<Integer> output_photos_human = new ArrayList();
            for(int i = 0; i < output_num; i++){
                output_photos_food.add(Integer.parseInt(myTensorFlow.results_food.get(i).getImageId()));
                output_photos_human.add(Integer.parseInt(myTensorFlow.results_human.get(i).getImageId()));
            }
            //モデルクローズ
            myTensorFlow.endRecognition();

            //上で分かった識別率の高い画像だけ画質を上げる　
            for(int i = 0; i < output_num; i++){
                image2d_bitmap_a.set( output_photos_food.get(i),
                        cutter.cut_one(image360_bitmap, pitch_roll_yaw,0.5, 0.5,360,output_photos_food.get(i)));
                image2d_bitmap_a.set( output_photos_human.get(i),
                        cutter.cut_one(image360_bitmap, pitch_roll_yaw,0.5, 0.5,360,output_photos_human.get(i)));

            }
            Log.d("debug","Complete cut image");

            myFileAccess.mkdirImage2D();
            // Human 2D画像保存
            for(int index : output_photos_human) {
                File image2d_human = new File(myFileAccess.image2D + "/image2d_human_" +Integer.toString(index) + myFileAccess.fileid + ".JPG");
                myFileAccess.storeImage2D(image2d_bitmap_a.get(index),image2d_human);
            }

            // Food 2D画像保存
            for(int index : output_photos_food) {
                File image2d_food = new File(myFileAccess.image2D + "/image2d_food_" +Integer.toString(index) + myFileAccess.fileid + ".JPG");
                myFileAccess.storeImage2D(image2d_bitmap_a.get(index),image2d_food);
            }

            Log.d("debug", "*** END instakePhotos Task ***");
            return null;
        }


        @Override
        protected void onPostExecute(Void  v)  {
            Toast toast = Toast.makeText(getApplicationContext(), "Complete instakePhotos", Toast.LENGTH_LONG);
            toast.show();
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

    public class MyTensorFlow {
        // These are the settings for the original v1 Inception model. If you want to
        // use a model that's been produced from the TensorFlow for Poets codelab,
        // you'll need to set IMAGE_SIZE = 299, IMAGE_MEAN = 128, IMAGE_STD = 128,
        // INPUT_NAME = "Mul", and OUTPUT_NAME = "final_result".
        // You'll also need to update the MODEL_FILE and LABEL_FILE paths to point to
        // the ones you produced.
        //
        // To use v3 Inception model, strip the DecodeJpeg Op from your retrained
        // model first:
        //
        // python strip_unused.py \
        // --input_graph=<retrained-pb-file> \
        // --output_graph=<your-stripped-pb-file> \
        // --input_node_names="Mul" \
        // --output_node_names="final_result" \
        // --input_binary=true
        private final int INPUT_SIZE;
        private final int IMAGE_MEAN;
        private final float IMAGE_STD;
        private final String INPUT_NAME;
        private final String OUTPUT_NAME;
        //private static final int INPUT_SIZE = 224;
        //private static final int IMAGE_MEAN = 117;
        //private static final float IMAGE_STD = 1;
        //private static final String INPUT_NAME = "input";
        //private static final String OUTPUT_NAME = "output";

        private final String MODEL_FILE;
        private final String LABEL_FILE;

        private Bitmap croppedBitmap;
        private Matrix imageToCropTransform;

        private int inputHeight;
        private int inputWidth;

        private Classifier classifier;

        ArrayList<Classifier.Recognition> results_food = new ArrayList<Classifier.Recognition>();
        ArrayList<Classifier.Recognition> results_human = new ArrayList<Classifier.Recognition>();

        MyTensorFlow() {
            INPUT_SIZE = 299;
            IMAGE_MEAN = 128;
            IMAGE_STD = 128;
            //TODO strip_unused.py実行時には--input_node_names, INPUT_NAME="Mul"にするべきかも
            INPUT_NAME = "Placeholder";
//            INPUT_NAME = "Mul";
            OUTPUT_NAME = "final_result";
            //TODO MODEL_FILEはどちらかを選択
            MODEL_FILE = "file:///android_asset/instabae_graph_android_unused.pb";
//            MODEL_FILE = "file:///android_asset/instabae_graph_android.pb";
            LABEL_FILE = "file:///android_asset/instabae_labels.txt";

            croppedBitmap = null;
        }

        public void modelCreate() {
            classifier = TensorFlowImageClassifier.create(
                    getAssets(),
                    MODEL_FILE,
                    LABEL_FILE,
                    INPUT_SIZE,
                    IMAGE_MEAN,
                    IMAGE_STD,
                    INPUT_NAME,
                    OUTPUT_NAME
            );
        }

        public void endRecognition() {
                classifier.close();
        }

        public void startRecognition(Bitmap _inputImage, String _image_num) {

            inputWidth = _inputImage.getWidth();
            inputHeight = _inputImage.getHeight();
            croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);

            imageToCropTransform = getTransformationMatrix(
                    inputWidth, inputHeight,
                    INPUT_SIZE, INPUT_SIZE,
                    0, true);//sensorOrientation, MAINTAIN_ASPECT

            croppedBitmap = Bitmap.createBitmap(_inputImage, 0, 0, _inputImage.getWidth(), _inputImage.getHeight(), imageToCropTransform, true);
            List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap, _image_num);
            for(int i = 0; i < results.size(); i++){
                if(results.get(i).getTitle().equals("osyafood")){
                    results_food.add(results.get(i));
                }else if(results.get(i).getTitle().equals("osyahuman")){
                    results_human.add(results.get(i));
                }else{
                    Log.d("debug","Unmatching labels");
                }
            }
        }

        /**
         * Returns a transformation matrix from one reference frame into another.
         * Handles cropping (if maintaining aspect ratio is desired) and rotation.
         *
         * @param srcWidth Width of source frame.
         * @param srcHeight Height of source frame.
         * @param dstWidth Width of destination frame.
         * @param dstHeight Height of destination frame.
         * @param applyRotation Amount of rotation to apply from one frame to another.
         *  Must be a multiple of 90.
         * @param maintainAspectRatio If true, will ensure that scaling in x and y remains constant,
         * cropping the image if necessary.
         * @return The transformation fulfilling the desired requirements.
         */
        public Matrix getTransformationMatrix(
                final int srcWidth,
                final int srcHeight,
                final int dstWidth,
                final int dstHeight,
                final int applyRotation,
                final boolean maintainAspectRatio) {
            final Matrix matrix = new Matrix();

            if (applyRotation != 0) {
                if (applyRotation % 90 != 0) {
                    //LOGGER.w("Rotation of %d % 90 != 0", applyRotation);
                }

                // Translate so center of image is at origin.
                matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

                // Rotate around origin.
                matrix.postRotate(applyRotation);
            }

            // Account for the already applied rotation, if any, and then determine how
            // much scaling is needed for each axis.
            final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

            final int inWidth = transpose ? srcHeight : srcWidth;
            final int inHeight = transpose ? srcWidth : srcHeight;

            // Apply scaling if necessary.
            if (inWidth != dstWidth || inHeight != dstHeight) {
                final float scaleFactorX = dstWidth / (float) inWidth;
                final float scaleFactorY = dstHeight / (float) inHeight;

                if (maintainAspectRatio) {
                    // Scale by minimum factor so that dst is filled completely while
                    // maintaining the aspect ratio. Some image may fall off the edge.
                    final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                    //final float scaleFactor = Math.min(scaleFactorX, scaleFactorY);
                    matrix.postScale(scaleFactor, scaleFactor);
                } else {
                    // Scale exactly to fill dst from src.
                    matrix.postScale(scaleFactorX, scaleFactorY);
                }
            }

            if (applyRotation != 0) {
                // Translate back from origin centered reference to destination frame.
                matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
            }

            return matrix;
        }
    }

    public class CheckPhotoTimer extends TimerTask {

        public long numStoredFiles;
        public HttpConnector httpConnector;

        CheckPhotoTimer(){
            this.numStoredFiles = httpConnector.getList().size();
        }

        @Override
        public void run() {
            if(numStoredFiles == httpConnector.getList().size()){

            }
            else{
                Toast toast = Toast.makeText(getApplication(), "写真が撮影されました！", Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }
}