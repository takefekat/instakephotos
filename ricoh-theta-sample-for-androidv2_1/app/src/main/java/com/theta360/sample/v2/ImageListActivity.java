package com.theta360.sample.v2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.Toolbar;

import com.theta360.sample.v2.network.HttpConnector;
import com.theta360.sample.v2.view.ImageRow;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageListActivity extends Activity {

    // 表示する画像の名前（拡張子無し）
    private String[] members = new String[1000];
    private String cameraIpAddress;
    private ImageListActivity.LoadObjectListTask sampleTask = null;
    private String fileId;
    private byte[] thumbnail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_list);

        Log.d("debug","*** Start ImageList2D_Activity ***");

        // ツールバーをアクションバーとしてセット
        MenuInflater inflater = getMenuInflater();

        // フォルダ生成
        MyFolderAccess myFolderAccess = new MyFolderAccess();
        myFolderAccess.initDirctory();

        // 全360°画像に以下が存在するか確認ー＞なければ instakePhoto Task起動
        checkFolder();

        //サムネ画像取得（球体版優先）

        final ArrayList<File> thumbnailList = myFolderAccess.getThumbnailFileList();

        // GridViewのインスタンスを生成
        GridView gridview = (GridView) findViewById(R.id.gridview2);
        gridview.setNumColumns(2);

        //表示する画像を取得
        ArrayList<Bitmap> lstBitmap = new ArrayList<Bitmap>();
        for(File file: thumbnailList) {
            byte[] byteArray = convertFile(file);
            lstBitmap.add(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length));
            Log.d("debug",file + " size : " + byteArray.length );
        }
        Log.d("debug","lstBitmap size = " + lstBitmap.size());

        //アダプター作成
        BitmapAdapter adapter = new BitmapAdapter(getApplicationContext(), lstBitmap);
        gridview.setAdapter(adapter);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                cameraIpAddress = getResources().getString(R.string.theta_ip_address);
                MyFileAccess myFileAccess = new MyFileAccess(thumbnailList.get(position).getName());

                Log.d("debug","ImageListActivity-->GLPhotoActivity" + Integer.toString(position) +" "+myFileAccess.fileid);

                // Activity 遷移
                Intent intent = new Intent(getApplication(), GLPhotoActivity.class);
                intent.putExtra("CAMERA_IP_ADDRESS", cameraIpAddress);
                intent.putExtra("OBJECT_ID",myFileAccess.fileid);
                intent.putExtra("THUMBNAIL",myFileAccess.getThumbnailByteArray());
                startActivity(intent);
            }
        });


        gridview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(ImageListActivity.this )
                        .setTitle("CAUTION !")
                        .setMessage("画像を削除しますか？")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // OK button pressed
                                //ここでListからタップされた情報を取得したいparent.getItemAtPositionなど。
                                MyFileAccess myFileAccess = new MyFileAccess(thumbnailList.get(position).getName());
                                if(myFileAccess.deleteAll()){
                                    Log.d("debug","Cannot delete file: " + myFileAccess.fileid);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            }
        });

    }

    private void checkFolder(){
        Log.d("debug","*** Start checkFolder() *** ");

        MyFolderAccess myFolderAccess = new MyFolderAccess();
        ArrayList<File> image360filelist= myFolderAccess.getImage360FileList();

        for (File image360file : image360filelist){
            MyFileAccess myfile = new MyFileAccess(image360file.getName());

            if(!myfile.ExistAllFiles()) {
                Log.d("debug", myfile.image360 + " に関するファイルが足りません。");
                ImageListActivity.InstakePhotos instakePhotos = new ImageListActivity.InstakePhotos();
                instakePhotos.execute(myfile.image360.getAbsolutePath());
            }
        }
        Log.d("debug","*** End checkFolder() *** ");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.to_take_photo, menu);
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
            case R.id.takePhoto:
                // 写真撮影画面（Activity）に遷移
                Intent intent = new Intent(getApplication(), TakePhotoActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
        return true;
    }

    private void changeCameraStatus(final int resid) {
        runOnUiThread(new Runnable() {
            public void run() {
                //textCameraStatus.setText(resid);
            }
        });
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GLPhotoActivity.REQUEST_REFRESH_LIST) {
            if (sampleTask != null) {
                sampleTask.cancel(true);
            }
            sampleTask = new ImageListActivity.LoadObjectListTask();
            sampleTask.execute();
        }
    }

    private class LoadObjectListTask extends AsyncTask<Void, String, List<ImageRow>> {

        private ProgressBar progressBar;

        //	コンストラクタ
        // 		progressBarの設定
        public LoadObjectListTask() {
            //progressBar = (ProgressBar) findViewById(R.id.loading_object_list_progress_bar);
        }

        //	実行前処理
        //		progressBarの可視化
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        //	メイン処理
        //		返り値：List<ImageRow>
        @Override
        protected List<ImageRow> doInBackground(Void... params) {
            try {

                String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/image/thumbnail";
                File[] _files = new File(storagePath).listFiles();
                ArrayList<File> files = new ArrayList<File>();
                for (File file : _files){
                    Log.d("debug",file.getName());
                    if(file.isFile()) {
                        files.add(file);
                        Log.d("debug",file.getName());
                    }
                }
                //HttpConnector camera = new HttpConnector(cameraIpAddress);
                //changeCameraStatus(R.string.text_camera_standby);

                List<ImageRow> imageRows = new ArrayList<>();

                //StorageInfo storage = camera.getStorageInfo();
                ImageRow storageCapacity = new ImageRow();
                //int freeSpaceInImages = storage.getFreeSpaceInImages();
                int megaByte = 1024 * 1024;
                //long freeSpace = storage.getFreeSpaceInBytes() / megaByte;
                //long maxSpace = storage.getMaxCapacity() / megaByte;
                //storageCapacity.setFileName("Free space: " + freeSpaceInImages + "[shots] (" + freeSpace + "/" + maxSpace + "[MB])");
                imageRows.add(storageCapacity);

                //ArrayList<ImageInfo> objects = camera.getList();
                //int objectSize = objects.size();

                for (int i = 0; i < files.size(); i++) {
                    ImageRow imageRow = new ImageRow();
                    //ImageInfo object = objects.get(i);

                    //imageRow.setFileSize(object.getFileSize());
                    //  TODO
                    //  imageRowの情報が十分か確認する
                    String fname = getfilename(files.get(i).getAbsoluteFile().toString());
                    fname = fname.substring(fname.length() - 12);
                    //imageRow.setFileName(object.getFileName());
                    imageRow.setFileName(fname);
                    imageRow.setFileId(fname);
                    //imageRow.setCaptureDate(object.getCaptureDate());
                    //publishProgress("<ImageInfo: File ID=" + object.getFileId() + ", filename=" + object.getFileName() + ", capture_date=" + object.getCaptureDate()
                    //		+ ", image_pix_width=" + object.getWidth() + ", image_pix_height=" + object.getHeight() + ", object_format=" + object.getFileFormat()
                    //		+ ">");

                    //if (object.getFileFormat().equals(ImageInfo.FILE_FORMAT_CODE_EXIF_JPEG)) {
                    imageRow.setIsPhoto(true);
                    //	Bitmap thumbnail = camera.getThumb(object.getFileId());
                    //	ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    //	thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    //	final byte[] thumbnailImage = baos.toByteArray();
                    byte[] thumbnailImage = convertFile(files.get(i));
                    imageRow.setThumbnail(thumbnailImage);
                    //} else {
                    //	imageRow.setIsPhoto(false);
                    //}

                    imageRows.add(imageRow);

                    //publishProgress("getList: " + (i + 1) + "/" + objectSize);
                }
                return imageRows;

            } catch (Throwable throwable) {
                String errorLog = Log.getStackTraceString(throwable);
                publishProgress(errorLog);
                return null;
            }
        }

        String getfilename(String fileId)   {

            String[] list = fileId.split("/",0);
            String fname = list[list.length-1];
            return fname;
        }

        @Override
        protected void onPostExecute(List<ImageRow> imageRows) {
            GLPhotoActivity.startActivityForResult(ImageListActivity.this, cameraIpAddress, fileId, thumbnail, false);
        }
        @Override
        protected void onProgressUpdate(String... values) {

        }

        @Override
        protected void onCancelled() {
            progressBar.setVisibility(View.GONE);
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
                e.printStackTrace();
            }
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
            return bout.toByteArray();
        } catch (Exception e) {
            Log.d("debug","CANNOT OPEN FILE:"+file.getAbsolutePath());
            e.printStackTrace();
        }
        return null;
    }

    private class InstakePhotos extends AsyncTask<String, Object, Void> {

        @Override
        protected void onPreExecute() {
            Toast toast = Toast.makeText(getApplicationContext(), "Start instakePhotos !", Toast.LENGTH_LONG);
            toast.show();
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

            image2d_bitmap_a = cutter.cut(image360_bitmap, pitch_roll_yaw,0.5, 0.5, 0,null,360);
            Log.d("debug","Complete cut image");

            SelectMode selectMode = new SelectMode();

            //TODO 上位何個を出力するか定数として一か所で定義するか検討
            int output_num = 3; //<変更>
            if(selectMode.DEFINE_C == 1) {
                output_num = 6; //<変更>
            }

            //TODO モデル読み込みは事前の実施が可能(位置変更可)
            //モデル読み込み
            MyTensorFlow myTensorFlow = new MyTensorFlow(selectMode.DEFINE_C, selectMode.DEFINE_M, ImageListActivity.this);
            myTensorFlow.modelCreate();

            //TODO ビットマップの数と合わせる
            //切り取った画像を認識してinstabaeを抽出(resultsに代入)
            for(int i = 0; i < image2d_bitmap_a.size(); i++){
                myTensorFlow.startRecognition(image2d_bitmap_a.get(i), Integer.toString(i));
            }

            //resultsを認識率の高い順にソート
            if(selectMode.DEFINE_C == 0) {
                Collections.sort(myTensorFlow.results_food, new Classifier.ScoreCmp());
                Collections.sort(myTensorFlow.results_human, new Classifier.ScoreCmp());

                //識別率の高い画像の番号を保存
                ArrayList<Integer> output_photos_food = new ArrayList<Integer>();
                ArrayList<Integer> output_photos_human = new ArrayList<Integer>();
                for (int i=0;i<output_num; i++){
                    output_photos_food.add(-1);
                    output_photos_human.add(i);
                }

                for (int i = 0; i < output_num; i++) {
                    if (myTensorFlow.results_food.get(i).getConfidence().intValue() == -1) {
                        output_photos_food.add(-1);
                    } else {
                        output_photos_food.add(Integer.parseInt(myTensorFlow.results_food.get(i).getImageId()));
                    }
                    if (myTensorFlow.results_human.get(i).getConfidence().intValue() == -1) {
                        output_photos_human.add(-1);
                    } else {
                        output_photos_human.add(Integer.parseInt(myTensorFlow.results_human.get(i).getImageId()));
                    }
                }

                //
                // debug ここから（評価値ファイル作成）
                //
                for (int i = 0; i < output_num; i++) {
                    String index = myTensorFlow.results_food_d.get(i).getImageId();
                    Log.d("debug", "Food評価値 " + i + " 番目: No." + index);
                    float dasafood = 0;
                    float dasahuman = 0;
                    float osyafood = myTensorFlow.results_food_d.get(i).getConfidence();
                    float osyahuman = 0;
                    float other = 0;

                    for (int j = 0; j < myTensorFlow.results_dasahuman.size(); j++) {
                        if (index == myTensorFlow.results_dasafood.get(j).getImageId()) {
                            dasafood = myTensorFlow.results_dasafood.get(j).getConfidence();
                        }
                        if (index == myTensorFlow.results_dasahuman.get(j).getImageId()) {
                            dasahuman = myTensorFlow.results_dasahuman.get(j).getConfidence();
                        }
                        if (index == myTensorFlow.results_human_d.get(j).getImageId()) {
                            osyahuman = myTensorFlow.results_human_d.get(j).getConfidence();
                        }
                        if (index == myTensorFlow.results_other.get(j).getImageId()) {
                            other = myTensorFlow.results_other.get(j).getConfidence();
                        }
                    }
                    myFileAccess.storeConfidence(dasafood, dasahuman, osyafood, osyahuman, other, index);
                }
                for (int i = 0; i < output_num; i++) {

                    String index = myTensorFlow.results_human_d.get(i).getImageId();
                    Log.d("debug", "Human評価値 " + i + " 番目: No." + index);
                    float dasafood = 0;
                    float dasahuman = 0;
                    float osyafood = 0;
                    float osyahuman = myTensorFlow.results_human_d.get(i).getConfidence();
                    float other = 0;

                    for (int j = 0; j < myTensorFlow.results_dasahuman.size(); j++) {
                        if (index == myTensorFlow.results_dasafood.get(j).getImageId()) {
                            dasafood = myTensorFlow.results_dasafood.get(j).getConfidence();
                        }
                        if (index == myTensorFlow.results_dasahuman.get(j).getImageId()) {
                            dasahuman = myTensorFlow.results_dasahuman.get(j).getConfidence();
                        }
                        if (index == myTensorFlow.results_food_d.get(j).getImageId()) {
                            osyafood = myTensorFlow.results_food_d.get(j).getConfidence();
                        }
                        if (index == myTensorFlow.results_other.get(j).getImageId()) {
                            other = myTensorFlow.results_other.get(j).getConfidence();
                        }
                    }
                    myFileAccess.storeConfidence(dasafood, dasahuman, osyafood, osyahuman, other, index);
                }
                //
                //  debug　ここまで
                //

                //上で分かった識別率の高い画像だけ画質を上げる　<変更>
                for (int i = 0; i < output_num; i++) {
                    if (output_photos_food.get(i).equals(-1)) {
                    } else {
                        image2d_bitmap_a.set(output_photos_food.get(i),
                                cutter.cut_one(image360_bitmap, pitch_roll_yaw, 0.5, 0.5, 360, output_photos_food.get(i)));
                    }
                    if (output_photos_human.get(i).equals(-1)) {
                    } else {
                        image2d_bitmap_a.set(output_photos_human.get(i),
                                cutter.cut_one(image360_bitmap, pitch_roll_yaw, 0.5, 0.5, 360, output_photos_human.get(i)));
                    }
                }
                Log.d("debug","Complete cut image");

                // Human 2D画像保存
                Bitmap no_image = BitmapFactory.decodeResource(getResources(), R.drawable.noimage);
                for(int i=0; i<output_num; i++) {
                    File image2d_human = new File(myFileAccess.image2D + "/image2d_" + i + "_human_"  + myFileAccess.fileid + ".JPG");
                    if(output_photos_human.get(i).equals(-1)){
                        myFileAccess.storeImage2D(no_image, image2d_human);
                    }else {
                        myFileAccess.storeImage2D(image2d_bitmap_a.get(output_photos_human.get(i)), image2d_human);
                    }
                    File image2d_food = new File(myFileAccess.image2D + "/image2d_" + i + "_food_" +  myFileAccess.fileid + ".JPG");
                    if(output_photos_food.get(i).equals(-1)){
                        myFileAccess.storeImage2D(no_image, image2d_food);
                    }else {
                        myFileAccess.storeImage2D(image2d_bitmap_a.get(output_photos_food.get(i)),image2d_food);
                    }
                }
            }else{
                Collections.sort(myTensorFlow.results_instabae, new Classifier.ScoreCmp());

                //識別率の高い画像の番号を保存
                ArrayList<Integer> output_photos = new ArrayList();
                for(int i = 0; i < output_num; i++){
                    output_photos.add(Integer.parseInt(myTensorFlow.results_instabae.get(i).getImageId()));
                }

                //
                // debug ここから（評価値ファイル作成）
                //
                for(int i=0; i<output_num; i++){
                    String index = myTensorFlow.results_instabae.get(i).getImageId();
                    Log.d("debug","Instabae評価値 "+ i + " 番目: No." + index  );
                    float notinstabae = 0;
                    float instabae = myTensorFlow.results_instabae.get(i).getConfidence();

                    for(int j=0; j<myTensorFlow.results_instabae.size(); j++){
                        if(index ==  myTensorFlow.results_instabae.get(j).getImageId()){
                            notinstabae = myTensorFlow.results_notinstabae.get(j).getConfidence();
                        }
                    }
                    myFileAccess.storeConfidence_i(instabae,notinstabae,index);
                }
                //
                //  debug　ここまで
                //

                //上で分かった識別率の高い画像だけ画質を上げる
                for (int i = 0; i < output_num; i++) {
                    image2d_bitmap_a.set(output_photos.get(i),
                            cutter.cut_one(image360_bitmap, pitch_roll_yaw, 0.5, 0.5, 360, output_photos.get(i)));
                }
                Log.d("debug","Complete cut image");

                // 2D画像保存
                for(int i=0; i<output_num; i=i+2) {
                    File image2d_human = new File(myFileAccess.image2D + "/image2d_" + i + "_human_" + myTensorFlow.results_instabae.get(i).getConfidence() + myFileAccess.fileid + ".JPG");
                    myFileAccess.storeImage2D(image2d_bitmap_a.get(output_photos.get(i)), image2d_human);
                    File image2d_food = new File(myFileAccess.image2D + "/image2d_" + i + "_food_" + myTensorFlow.results_instabae.get(i).getConfidence() + myFileAccess.fileid + ".JPG");
                    myFileAccess.storeImage2D(image2d_bitmap_a.get(output_photos.get(i+1)),image2d_food);
                }
            }
            //モデルクローズ
            myTensorFlow.endRecognition();


            Log.d("debug", "*** END instakePhotos Task ***");
            return null;
        }
    }
}