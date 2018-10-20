package com.theta360.sample.v2;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MyFileAccess {

    public String fileid;           // 例：R0010252
    public File image360;           // /***/image/image360/image360_R0010252.JPG
    public File thumbnail;          // /***/image/thumbnail/thumbnail_R0010252.JPG
    public File thumbnailcircle;    // /***/image/thumbnailcircle/thumbnailcircle_R0010252.JPG
    public File image2D;            // /***/image/image2d/
    public File imageinfo;          // /***/image/info/info_R0010252.txt

    public String imageDir;

    // コンストラクタ
    MyFileAccess(){
        fileid = "";
        imageDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/image/";

    }
    MyFileAccess(String str){
        fileid = String2fileId(str);
        Log.d("debug",str + "-->"+fileid);

        imageDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/image/";
        image360 = new File(imageDir + "image360/image360_"+fileid+".JPG");
        thumbnail = new File(imageDir + "thumbnail/thumbnail_"+fileid+".JPG");
        thumbnailcircle = new File(imageDir + "thumbnailcircle/thumbnailcircle_"+fileid+".JPG");
        image2D = new File(imageDir + "image2D/"+ fileid);
        imageinfo = new File(imageDir + "info/info_"+ fileid+".txt");
    }

    // 全フォルダ作成
    // （image2Dフォルダの中には、360°画像のfileId名をフォルダ名とするフォルダを作成する）
    public void InitDirctory(){
        String status = Environment.getExternalStorageState();

        String Path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/image";
        File Dir = new File(Path);
        String thumbnailPath = Path + "/thumbnail/";
        File thumbnailDir = new File(thumbnailPath);
        String image360Path = Path + "/image360/";
        File image360Dir = new File(image360Path);
        String infoPath = Path + "/info/";
        File infoDir = new File(infoPath);
        String image2dPath = Path + "/image2d/";
        File image2dDir = new File(image2dPath);
        String thumbnailcirclePath = Path + "/thumbnailcircle/";
        File thumbnailcircleDir = new File(thumbnailcirclePath);

        if(status.equals(Environment.MEDIA_MOUNTED)){
            //ディレクトリがなければ作成する
            if(!Dir.exists()){
                Dir.mkdir();
                Log.d("debug","mkdir " + Dir);
            }
            if(!thumbnailDir.exists()){
                thumbnailDir.mkdir();
                Log.d("debug","mkdir " + thumbnailDir);
            }
            if(!image360Dir.exists()){
                image360Dir.mkdir();
                Log.d("debug","mkdir " + image360Dir);
            }
            if(!infoDir.exists()){
                infoDir.mkdir();
                Log.d("debug","mkdir " + infoDir);
            }
            if(!image2dDir.exists()){
                image2dDir.mkdir();
                Log.d("debug","mkdir " + image2dDir);
            }
            if(!thumbnailcircleDir.exists()){
                thumbnailcircleDir.mkdir();
                Log.d("debug","mkdir " + thumbnailcircleDir);
            }
        }else{
            Log.d("debug","外部ストレージなし");
        }
    }



    public String String2fileId(String str) {
        int R_index = str.lastIndexOf("R");
        return str.substring(R_index,R_index+8);
    }


    // image360フォルダの全ファイルをリストで返す
    public ArrayList<File> GetImage360FileList(){
        String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/image/image360/";
        File[] _files = new File(storagePath).listFiles();
        ArrayList<File> files = new ArrayList<File>();

        for (int i=0; i<_files.length; i++) {
            if(_files[i].isFile()) {
                Log.d("debug","image/image360に存在するfile "+ Integer.toString(i) +" : " + _files[i]);
                files.add(_files[i]);
            }
        }
        return files;
    }


    // 360°画像に対して以下があるか確認
    // thumbnail
    // thumbnailcircle
    // 2D画像フォルダ
    // info
    //
    public boolean ExistAllFiles(){
        boolean AllFileExists = true;

        if(fileid == ""){
            Log.d("debug","fileIdが設定されていません。:ExistAllFiles()");
            AllFileExists = false;
        }

        if(!thumbnail.exists()) {
            Log.d("debug","thumbnail file Not Exists.:ExistAllFiles()");
            AllFileExists = false;
        }

        if(!thumbnailcircle.exists()) {
            Log.d("debug","thumbnailcircle file Not Exists.:ExistAllFiles()");
            AllFileExists = false;
        }

        if(!image2D.exists()) {
            Log.d("debug","image2D Dir Not Exists.:ExistAllFiles()");
            AllFileExists = false;
        }
        // TODO image2d/内に切り取られた画像が存在するか確認する

        if(!imageinfo.exists()){
            Log.d("debug","info file Not Exists.:ExistAllFiles()");
            AllFileExists = false;

            try {
                Log.d("debug","info file作成:" + imageinfo);
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(imageinfo)));
                pw.println(0.0);
                pw.println(0.0);
                pw.println(0.0);
                pw.close();
            } catch (IOException e) {
                Log.d("debug","infoファイルを作成できません。");
                e.printStackTrace();
            }
        }

        return AllFileExists;
    }
}
