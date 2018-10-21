package com.theta360.sample.v2;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.theta360.sample.v2.glview.GLPhotoView;
import com.theta360.sample.v2.model.Photo;
import com.theta360.sample.v2.model.RotateInertia;
import com.theta360.sample.v2.network.HttpDownloadListener;
import com.theta360.sample.v2.network.HttpConnector;
import com.theta360.sample.v2.network.ImageData;
import com.theta360.sample.v2.view.ConfigurationDialog;
//import com.theta360.sample.v2.view.LogView;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
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

        Intent intent = getIntent();
        String cameraIpAddress = intent.getStringExtra(CAMERA_IP_ADDRESS);
        String fileId = intent.getStringExtra(OBJECT_ID);

        MyFileAccess myFileAccess = new MyFileAccess(fileId);
        //byte[] byteThumbnail = intent.getByteArrayExtra(THUMBNAIL);
        byte[] byteThumbnail = myFileAccess.getThumbnailByteArray();
        Log.d("debug",Integer.toString(byteThumbnail.length ));

        ByteArrayInputStream inputStreamThumbnail = new ByteArrayInputStream(byteThumbnail);
        Drawable thumbnail = BitmapDrawable.createFromStream(inputStreamThumbnail, null);

        Photo _thumbnail = new Photo(((BitmapDrawable)thumbnail).getBitmap());

        mGLPhotoView = (GLPhotoView) findViewById(R.id.photo_image);
        mGLPhotoView.setTexture(_thumbnail);
        mGLPhotoView.setmRotateInertia(mRotateInertia);

        mLoadPhotoTask = new LoadPhotoTask(cameraIpAddress, fileId);
        mLoadPhotoTask.execute();
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

        public String getfilename(){
            Log.d("debug","start getfilename");
            Log.d("debug",fileId);
            //String[] list = fileId.split("/",0);
            //String fname = list[list.length-1];
            String fname = fileId.substring(fileId.length()-12);
            Log.d("debug",fname);
            return fname;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected ImageData doInBackground(Void... params) {
            try {
                Log.d("debug","*** Start LoadPhotoTask ***");
                String rawPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/image/image360/";
                Log.d("debug",rawPath);
                String infoPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/image/info/";
                Log.d("debug",infoPath);
                String raw_fname = rawPath + "image360_" + getfilename();
                Log.d("debug",raw_fname);
                String info_fname = infoPath + "info_"+ getfilename();
                info_fname = info_fname.substring(0,info_fname.length()-3) + "txt";
                Log.d("debug",info_fname);

                // byte[] mRowDataをファイル入力
                ImageData resizedImageData = new ImageData();
                resizedImageData.setRawData(convertFile(new File(raw_fname)));
                Log.d("debug","End setRawData");

                // double pitch, roll, yaw をファイル入力
                double[] pitch_roll_yaw = readFileTodoubles(info_fname);
                resizedImageData.setPitch(pitch_roll_yaw[0]);
                resizedImageData.setRoll(pitch_roll_yaw[1]);
                resizedImageData.setYaw(pitch_roll_yaw[2]);
                Log.d("debug",resizedImageData.getPitch().toString());
                Log.d("debug",resizedImageData.getRoll().toString());
                Log.d("debug",resizedImageData.getYaw().toString());
                Log.d("debug","End setPitch,Roll,Yaw");

                return resizedImageData;

            } catch (Throwable throwable) {
                String errorLog = Log.getStackTraceString(throwable);
                publishProgress(errorLog);
                return null;
            }
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
                Log.d("debug","END OPEN FILE:"+file.getAbsolutePath());
                return bout.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("debug","CANNOT OPEN FILE:"+file.getAbsolutePath());
            }
            return null;
        }

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
              //  logViewer.append("<Angle: yaw=" + yaw + ", pitch=" + pitch + ", roll=" + roll + ">");

                mTexture = new Photo(__bitmap, yaw, pitch, roll);
                if (null != mGLPhotoView) {
                    mGLPhotoView.setTexture(mTexture);
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
}