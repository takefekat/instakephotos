package com.theta360.sample.v2;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

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
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;

public class MyFileAccess {

    public String fileid;           // 例：R0010252
    public File image360;           // /***/image/image360/image360_R0010252.JPG
    public File thumbnail;          // /***/image/thumbnail/thumbnail_R0010252.JPG
    public File thumbnailcircle;    // /***/image/thumbnailcircle/thumbnailcircle_R0010252.JPG
    public File image2D;
    public File imageinfo;          // /***/image/info/info_R0010252.txt

    // コンストラクタ
    MyFileAccess(){
        fileid = "";
    }

    MyFileAccess(String str){

        fileid = String2fileId(str);
        Log.d("debug",str + "-->"+fileid);

        String ExternalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/image/";
        image360 = new File(ExternalStoragePath + "/image360/image360_"+fileid+".JPG");
        thumbnail = new File(ExternalStoragePath + "/thumbnail/thumbnail_"+fileid+".JPG");
        thumbnailcircle = new File(ExternalStoragePath + "/thumbnailcircle/thumbnailcircle_"+fileid+".JPG");
        image2D = new File(ExternalStoragePath + "/image2D/"+ fileid);
        imageinfo = new File(ExternalStoragePath + "/info/info_"+ fileid+".txt");


        Log.d("debug",image360.getAbsolutePath());
        Log.d("debug",thumbnail.getAbsolutePath());
        Log.d("debug",thumbnailcircle.getAbsolutePath());
        Log.d("debug",image2D.getAbsolutePath());
        Log.d("debug",imageinfo.getAbsolutePath());
    }

    // 360°画像に対して以下があるか確認
    // thumbnail
    // thumbnailcircle
    // 2D画像フォルダ
    // info
    public boolean ExistAllFiles(){
        boolean AllFileExists = true;

        if(fileid == ""){
            Log.d("debug","fileIdが設定されていません。:ExistAllFiles()");
            AllFileExists = false;
        }else {
            if (!imageinfo.exists()) {
                Log.d("debug", "info file Not Exists.:ExistAllFiles()");
                //AllFileExists = false;

                try {
                    Log.d("debug", "info file作成:" + imageinfo);
                    PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(imageinfo)));
                    pw.println(0.0);
                    pw.println(0.0);
                    pw.println(0.0);
                    pw.close();
                } catch (IOException e) {
                    Log.d("debug", "infoファイルを作成できません。");
                    e.printStackTrace();
                }
            }

            if (!thumbnail.exists()) {
                Log.d("debug", "thumbnail file Not Exists.:" + thumbnail);
                //AllFileExists = false;

                //サムネイル画像の保存
                Bitmap thumbnail_btmp = null;
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                Log.d("debug","image360 size " + getBitmapImage360().getByteCount());
                /** 横の縮尺を求める */
                int scaleW = options.outWidth / 96 + 1;
                /** 縦の縮尺を求める */
                int scaleH = options.outHeight / 96 + 1;
                int scale = Math.max(scaleW, scaleH);
                options.inJustDecodeBounds = false;
                options.inSampleSize = 30;
                thumbnail_btmp = BitmapFactory.decodeFile(image360.getAbsolutePath(), options);

                //Log.d("debug", Integer.toString(thumbnail_btmp.getByteCount()));

                try {
                    FileOutputStream outstream = new FileOutputStream(thumbnail);
                    thumbnail_btmp.compress(Bitmap.CompressFormat.JPEG, 100, outstream);
                    outstream.flush();//ファイルとして出力
                    outstream.close();//使ったらすぐに閉じる
                    Log.d("debug", "サムネイル画像を保存完了:" + thumbnail);
                } catch (IOException ie) {
                    Log.d("debug", "サムネイル画像保存不可");
                }

            }

            if (!thumbnailcircle.exists()) {
                Log.d("debug", "thumbnailcircle file Not Exists.:" + thumbnailcircle);
                //AllFileExists = false;

                //球サムネイル画像の保存
                try {
                    FileOutputStream outstream = new FileOutputStream(thumbnailcircle);
                    CuttingImage cutter = new CuttingImage();
                    cutter.ball_cut(this.getBitmapImage360(),500,getImageInfo()).compress(Bitmap.CompressFormat.JPEG, 100, outstream);
                    outstream.flush();//ファイルとして出力
                    outstream.close();//使ったらすぐに閉じる
                    Log.d("debug", "球状のサムネイル画像を保存完了:" + thumbnailcircle);
                } catch (IOException ie) {
                    Log.d("debug", "球状のサムネイル画像保存不可");
                }

            }

            if (!image2D.exists()) {
                Log.d("debug", "image2D Dir Not Exists.:ExistAllFiles()");
                image2D.mkdir();
                AllFileExists = false;
            }else {
                File[] image2Ds = image2D.listFiles();
                if(image2Ds.length == 0){
                    AllFileExists = false;
                }
            }
            // TODO image2d/内に切り取られた画像が存在するか確認する


        }

        return AllFileExists;
    }


    // fileIdのimage360画像のBitmapを取得
    public Bitmap getBitmapImage360(){
        if(fileid == ""){
            Log.d("debug","fileIdが設定されていません。:getBitmapimage360()");
            return null;
        }
        byte[] byteArray = convertFile(image360);
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }

    // fileIdのpitch,roll,yawのデータ読み込み用
    public double[] getImageInfo() {
        FileReader fr = null;
        BufferedReader br = null;
        double[] res = new double[3];
        try {
            fr = new FileReader(imageinfo);
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

    public void storeImage2D(Bitmap image2D_btmp, File filename){
        Log.d("debug",  "2D画像file名：" + filename );
        try {
            FileOutputStream outstream = new FileOutputStream(filename);
            image2D_btmp.compress(Bitmap.CompressFormat.JPEG, 100, outstream);
            outstream.flush();//ファイルとして出力
            outstream.close();//使ったらすぐに閉じる
        } catch (IOException ie) {
            Log.d("debug", "2D画像保存不可");
        }
    }

    public void storeThumbnail(Bitmap btmp){

        try {
            FileOutputStream outstream = new FileOutputStream(thumbnail);
            btmp.compress(Bitmap.CompressFormat.JPEG, 100, outstream);
            outstream.flush();//ファイルとして出力
            outstream.close();//使ったらすぐに閉じる
            Log.d("debug",  "thumbnail 保存完了：" + thumbnail );
        } catch (IOException ie) {
            Log.d("debug", "Thumbnail 保存不可 : " + thumbnail);
        }
    }

    public void storeThumbnail(){
        Log.d("debug",  "thumbnail file名：" + thumbnail );
        try {
            Bitmap btmp = getBitmapImage360();

            FileOutputStream outstream = new FileOutputStream(thumbnail);
            btmp.compress(Bitmap.CompressFormat.JPEG, 100, outstream);
            outstream.flush();//ファイルとして出力
            outstream.close();//使ったらすぐに閉じる
        } catch (IOException ie) {
            Log.d("debug", "thumbnail 保存不可");
        }
    }

    public void storeImage360(Bitmap btmp){

        try {
            FileOutputStream outstream = new FileOutputStream(image360);
            btmp.compress(Bitmap.CompressFormat.JPEG, 100, outstream);
            outstream.flush();//ファイルとして出力
            outstream.close();//使ったらすぐに閉じる
            Log.d("debug",  "image360 保存：" + image360 );
        } catch (IOException ie) {
            Log.d("debug", "image360 保存不可");
        }
    }

    public void storeInfo(Double p, Double r, Double y){
        String pitch_s = BigDecimal.valueOf(p).toPlainString();
        String roll_s = BigDecimal.valueOf(r).toPlainString();
        String yaw_s = BigDecimal.valueOf(y).toPlainString();

        try {
            // FileWriterクラスのオブジェクトを生成する
            FileWriter file = new FileWriter(imageinfo);
            // PrintWriterクラスのオブジェクトを生成する
            PrintWriter pw = new PrintWriter(new BufferedWriter(file));
            //ファイルに書き込む
            pw.println(pitch_s);
            pw.println(roll_s);
            pw.println(yaw_s);

            //ファイルを閉じる
            pw.close();
            Log.d("debug","pitch, roll, yaw　保存完了" + imageinfo);
            Log.d( "debug","(pitch,roll,yaw) = (" + pitch_s + ", " + roll_s + ", " + yaw_s );
        } catch (IOException e) {
            Log.d("debug","pitch, roll, yaw出力不可");
            e.printStackTrace();
        }
    }

    public void mkdirImage2D(){
        if (!image2D.exists()) {
            image2D.mkdir();
        }
    }

    public boolean deleteAll(){
        boolean isDelete = true;
        if(!thumbnail.delete()){
            isDelete = false;
        }
        if(thumbnailcircle.delete()){
            isDelete = false;
        }
        if(imageinfo.delete()){
            isDelete = false;
        }
        if(image360.delete()){
            isDelete = false;
        }
        if(image2D.delete()){
            isDelete = false;
        }
        return isDelete;
    }

    // image360フォルダの全ファイルをリストで返す
    public ArrayList<File> getImage2DFileList(){

        File[] _files = new File(image2D.getAbsolutePath()).listFiles();
        ArrayList<File> files = new ArrayList<File>();

        for (int i=0; i<_files.length; i++) {
            if(_files[i].isFile()) {
                Log.d("debug","image/image2D に存在するfile "+ Integer.toString(i) +" : " + _files[i]);
                files.add(_files[i]);
            }
        }
        return files;
    }

    public byte[] getThumbnailByteArray(){
        if(!thumbnail.exists()) {
            Log.d("debug","No Exsit thumbnail: " + thumbnail);
            storeThumbnail();
        }
        return convertFile(thumbnail);
    }

    public byte[] getImage360ByteArray(){
        return convertFile(thumbnail);
    }

    public ArrayList<File> getHumanImage2D(){
        ArrayList<File> humanImage2DList = new ArrayList<File>(0);

        File[] fileArray = image2D.listFiles();
        for (File image2dfile : fileArray) {
            if(image2dfile.getName().indexOf("human") != -1){
                humanImage2DList.add(image2dfile);
            }
        }
        return  humanImage2DList;
    }

    public ArrayList<File> getFoodImage2D(){
        ArrayList<File> foodImage2DList = new ArrayList<File>(0);

        File[] fileArray = image2D.listFiles();
        for (File image2dfile : fileArray) {
            if(image2dfile.getName().indexOf("food") != -1){
                foodImage2DList.add(image2dfile);
            }
        }
        return  foodImage2DList;
    }

    public void storeConfidence(float dasafood,
                                float dasahuman,
                                float osyafood,
                                float osyahuman,
                                float other,
                                String index){
        File filename = new File(image2D + "/image2D_" + index + "_" + fileid + ".txt");
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
            pw.println("dasafood " + dasafood);
            pw.println("dasahuman" + dasahuman);
            pw.println("osyafood" + osyafood);
            pw.println("osyahuman" + osyahuman);
            pw.println("other" + other);
            pw.close();
            Log.d("debug", "評価値ファイル保存: " + filename);
            Log.d("debug","dasafood " + dasafood);
            Log.d("debug","dasahuman " + dasahuman);
            Log.d("debug","osyafood " + osyafood);
            Log.d("debug","osyahuman " + osyahuman);
            Log.d("debug","other " + other);
        } catch (IOException e) {
            Log.d("debug", "評価値ファイルを保存できません。");
            e.printStackTrace();
        }
    }

    public void storeConfidence_i(float instabae,
                                  float notinstabae,
                                  String index){
        File filename = new File(image2D + "/image2D_" + index + "_" + fileid + ".txt");
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
            pw.println("instabae " + instabae);
            pw.println("notinstabae " + notinstabae);
            pw.close();
            Log.d("debug", "評価値ファイル保存: " + filename);
            Log.d("debug","instabae " + instabae);
            Log.d("debug","notinstabae " + notinstabae);
        } catch (IOException e) {
            Log.d("debug", "評価値ファイルを保存できません。");
            e.printStackTrace();
        }
    }



    /**********************************/
    /*****    private method      *****/
    /**********************************/


    private String String2fileId(String str) {
        int R_index = str.lastIndexOf("R");
        return str.substring(R_index,R_index+8);
    }

    private byte[] convertFile(File file) {
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
