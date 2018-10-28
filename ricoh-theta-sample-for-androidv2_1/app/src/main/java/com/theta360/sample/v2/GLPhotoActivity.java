package com.theta360.sample.v2;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.theta360.sample.v2.glview.GLPhotoView;
import com.theta360.sample.v2.model.Photo;
import com.theta360.sample.v2.model.RotateInertia;
import com.theta360.sample.v2.network.ImageData;
import com.theta360.sample.v2.view.ConfigurationDialog;
//import com.theta360.sample.v2.view.LogView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * 写真を球体として表示するアクティビティ
 */
public class GLPhotoActivity extends Activity implements ConfigurationDialog.DialogBtnListener {

    private static final String CAMERA_IP_ADDRESS = "CAMERA_IP_ADDRESS";
    private static final String OBJECT_ID = "OBJECT_ID";
    private static final String THUMBNAIL = "THUMBNAIL";

    private GLPhotoView mGLPhotoView;

    private Photo mTexture = null;
    private LoadPhotoTask mLoadPhotoTask = null;

    private RotateInertia mRotateInertia = RotateInertia.INERTIA_0;

    public static final int REQUEST_REFRESH_LIST = 100;
    public static final int REQUEST_NOT_REFRESH_LIST = 101;


    /**
     * onCreate Method
     * @param savedInstanceState onCreate Status value
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glphoto);

    }

    @Override
    protected void onDestroy() {
        if (mTexture != null) {
            mTexture.getPhoto().recycle();
        }
        if (mLoadPhotoTask != null) {
            mLoadPhotoTask.cancel(true);
        }
        super.onDestroy();
    }

    /**
     * onCreateOptionsMenu method
     * @param menu Menu initialization object
     * @return Menu display feasibility status value
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.configuration, menu);

        return true;
    }


    /**
     * onOptionsItemSelected Method
     * @param item Process menu
     * @return Menu process continuation feasibility value
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.configuration:
                FragmentManager mgr = getFragmentManager();
                ConfigurationDialog.show(mgr, mRotateInertia);
                break;
            case R.id.ImageList2D:

                Intent _intent = getIntent();
                String fileId = _intent.getStringExtra(OBJECT_ID);

                Intent intent = new Intent(getApplication(), ImageList2D_Activity.class);
                intent.putExtra(OBJECT_ID,fileId);
                startActivity(intent);
                break;
            default:
                break;
        }
        return true;
    }


    /**
     * onResume Method
     */
    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        String cameraIpAddress = intent.getStringExtra(CAMERA_IP_ADDRESS);
        String fileId = intent.getStringExtra(OBJECT_ID);

        MyFileAccess myFileAccess = new MyFileAccess(fileId);
        byte[] byteThumbnail = myFileAccess.getThumbnailByteArray();

        ByteArrayInputStream inputStreamThumbnail = new ByteArrayInputStream(byteThumbnail);
        Drawable thumbnail = BitmapDrawable.createFromStream(inputStreamThumbnail, null);

        Photo _thumbnail = new Photo(((BitmapDrawable)thumbnail).getBitmap());

        mGLPhotoView = (GLPhotoView) findViewById(R.id.photo_image);
        mGLPhotoView.setTexture(_thumbnail);
        mGLPhotoView.setmRotateInertia(mRotateInertia);

        mLoadPhotoTask = new LoadPhotoTask(cameraIpAddress, fileId);
        mLoadPhotoTask.execute();

        mGLPhotoView.onResume();



        if (null != mTexture) {
            if (null != mGLPhotoView) {
                mGLPhotoView.setTexture(mTexture);
            }
        }
    }

    /**
     * onPause Method
     */
    @Override
    protected void onPause() {
        this.mGLPhotoView.onPause();
        super.onPause();
    }


    /**
     * onDialogCommitClick Method
     * @param inertia selected inertia
     */
    @Override
    public void onDialogCommitClick(RotateInertia inertia) {
        mRotateInertia = inertia;
        if (null != mGLPhotoView) {
            mGLPhotoView.setmRotateInertia(mRotateInertia);
        }
    }


    private class LoadPhotoTask extends AsyncTask<Void, Object, ImageData> {

        //private LogView logViewer;
        private ProgressBar progressBar;
        //private String cameraIpAddress;
        public String fileId;
        //private long fileSize;
        //private long receivedDataSize = 0;

        public LoadPhotoTask(String cameraIpAddress, String fileId) {
        //    this.logViewer = (LogView) findViewById(R.id.photo_info);
            this.progressBar = (ProgressBar) findViewById(R.id.loading_photo_progress_bar);
          //  this.cameraIpAddress = cameraIpAddress;
            this.fileId = fileId;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected ImageData doInBackground(Void... params) {

            Log.d("debug","*** Start LoadPhotoTask ***");
            MyFileAccess myFileAccess = new MyFileAccess(fileId);

            // byte[] mRowDataをファイル入力
            ImageData resizedImageData = new ImageData();
            resizedImageData.setRawData(myFileAccess.getImage360ByteArray());
            Log.d("debug","End setRawData");

            // double pitch, roll, yaw をファイル入力
            double[] pry = myFileAccess.getImageInfo();
            resizedImageData.setPitch(pry[0]);
            resizedImageData.setRoll(pry[1]);
            resizedImageData.setYaw(pry[2]);
            Log.d("debug","End setPitch,Roll,Yaw");

            return resizedImageData;
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
                  Log.d("debug","Image360 cannot Load");
                    return;
                }

                MyFileAccess myFileAccess = new MyFileAccess(fileId);

                Bitmap __bitmap = myFileAccess.getBitmapImage360();//BitmapFactory.decodeByteArray(dataObject, 0, dataObject.length);

                progressBar.setVisibility(View.GONE);

                Double yaw = imageData.getYaw();
                Double pitch = imageData.getPitch();
                Double roll = imageData.getRoll();

                mTexture = new Photo(__bitmap, yaw, pitch, roll);
                if (null != mGLPhotoView) {
                    mGLPhotoView.setTexture(mTexture);
                    Log.d("debug","Complete Load Task");


                    if(myFileAccess.image2D.listFiles().length == 0){
                        Toast toast = Toast.makeText(GLPhotoActivity.this, "Start InstakePhoto!!", Toast.LENGTH_LONG);
                        toast.show();
                        InstakePhotos instakePhotos = new InstakePhotos();
                        instakePhotos.execute(myFileAccess.fileid);
                    }

                }
            } else {
            //    logViewer.append("failed to download image");
            }
        }
    }

    /**
     * Activity call method
     * 
     * @param activity Call source activity
     * @param cameraIpAddress IP address for camera device
     * @param fileId Photo object identifier
     * @param thumbnail Thumbnail
     * @param refreshAfterClose true is to refresh list after closing this activity, otherwise is not to refresh
     */
    public static void startActivityForResult(Activity activity,
                                              String cameraIpAddress,
                                              String fileId,
                                              byte[] thumbnail,
                                              boolean refreshAfterClose) {
        int requestCode;
        if (refreshAfterClose) {
            requestCode = REQUEST_REFRESH_LIST;     //  画像撮影による遷移
        } else {
            requestCode = REQUEST_NOT_REFRESH_LIST; //  List選択による遷移
        }

        Intent intent = new Intent(activity, GLPhotoActivity.class);
        intent.putExtra(CAMERA_IP_ADDRESS, cameraIpAddress);
        intent.putExtra(OBJECT_ID, fileId);
        intent.putExtra(THUMBNAIL, thumbnail);
        activity.startActivityForResult(intent, requestCode);
    }


    private class InstakePhotos extends AsyncTask<String, Integer, Void> {

        private ProgressBar progressBar;

        InstakePhotos(){
            progressBar = (ProgressBar)findViewById(R.id.progressBar);
        }

        @Override
        protected void onPreExecute() {

            progressBar.setVisibility(View.VISIBLE);

            progressBar.setMax(42); // 水平プログレスバーの最大値を設定
            progressBar.setProgress(0); // 水平プログレスバーの値を設定
            progressBar.setSecondaryProgress(60); // 水平プログレスバーのセカンダリ値を設定
        }

        @Override
        protected void onProgressUpdate(Integer... value){
            progressBar.setProgress(value[0]); // 水平プログレスバーの値を設定
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

            onProgressUpdate(1);

            //TODO 上位何個を出力するか定数として一か所で定義するか検討
            int output_num = 3; //<変更>

            //TODO モデル読み込みは事前の実施が可能(位置変更可)
            //モデル読み込み
            MyTensorFlow_GL myTensorFlow = new MyTensorFlow_GL();
            myTensorFlow.modelCreate();

            //TODO ビットマップの数と合わせる
            //切り取った画像を認識してinstabaeを抽出(resultsに代入)
            for(int i = 0; i < image2d_bitmap_a.size(); i++){
                myTensorFlow.startRecognition(image2d_bitmap_a.get(i), Integer.toString(i));
                onProgressUpdate(1+i);
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

            onProgressUpdate(41);

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
            onProgressUpdate(42 );
            Log.d("debug", "*** END instakePhotos Task ***");
            return null;
        }



        @Override
        protected void onPostExecute(Void  v)  {

            progressBar.setVisibility(View.GONE);


            Toast toast = Toast.makeText(getApplicationContext(), "Complete instakePhotos", Toast.LENGTH_LONG);
            toast.show();

        }
    }
    public class MyTensorFlow_GL {
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

        MyTensorFlow_GL() {
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

}