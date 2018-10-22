package com.theta360.sample.v2;

import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;



public class MyFolderAccess {

    public File imageDir;
    public File image360Dir;
    public File thumbnailDir;
    public File thumbnailcircleDir;
    public File imageinfoDir;
    public File image2DDir;

    // コンストラクタ
    MyFolderAccess(){

        String ExternalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        imageDir = new File(ExternalStoragePath + "/image/");
        image360Dir = new File(ExternalStoragePath + "/image/image360/");
        thumbnailDir = new File(ExternalStoragePath + "/image/thumbnail/");
        thumbnailcircleDir = new File(ExternalStoragePath + "/image/thumbnailcircle/");
        imageinfoDir = new File(ExternalStoragePath + "/image/info/");
        image2DDir = new File(ExternalStoragePath + "/image/image2d/");

        Log.d("debug",image360Dir.getAbsolutePath());
        Log.d("debug",thumbnailDir.getAbsolutePath());
        Log.d("debug",thumbnailcircleDir.getAbsolutePath());
        Log.d("debug",image2DDir.getAbsolutePath());
        Log.d("debug",imageinfoDir.getAbsolutePath());

    }

    /**********************************/
    /*****    public method      *****/
    /**********************************/

    // 全フォルダ作成
    // （image2Dフォルダの中には、360°画像のfileId名をフォルダ名とするフォルダを作成する）
    public void initDirctory(){
        String status = Environment.getExternalStorageState();

        if(status.equals(Environment.MEDIA_MOUNTED)){
            //ディレクトリがなければ作成する
            if(!imageDir.exists()){
                imageDir.mkdir();
                Log.d("debug","mkdir " + imageDir);
            }
            if(!thumbnailDir.exists()){
                thumbnailDir.mkdir();
                Log.d("debug","mkdir " + thumbnailDir);
            }
            if(!image360Dir.exists()){
                image360Dir.mkdir();
                Log.d("debug","mkdir " + image360Dir);
            }
            if(!imageinfoDir.exists()){
                imageinfoDir.mkdir();
                Log.d("debug","mkdir " + imageinfoDir);
            }
            if(!image2DDir.exists()){
                image2DDir.mkdir();
                Log.d("debug","mkdir " + image2DDir);
            }
            if(!thumbnailcircleDir.exists()){
                thumbnailcircleDir.mkdir();
                Log.d("debug","mkdir " + thumbnailcircleDir);
            }
        }else{
            Log.d("debug","外部ストレージなし");
        }
    }

    // image360フォルダの全ファイルをリストで返す
    public ArrayList<File> getImage360FileList(){
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

    public ArrayList<File> getThumbnailFileList(){

        ArrayList<File> thumbnailList = new ArrayList<File>();
        ArrayList<File> image360List = getImage360FileList();
        Log.d("debug","360 image数 = "+Integer.toString(image360List.size()));
        for (int i=0; i<image360List.size(); i++) {
            if(image360List.get(i).isFile()) {
                MyFileAccess myFileAccess = new MyFileAccess(image360List.get(i).getName());
                if(myFileAccess.thumbnailcircle.exists()) {
                    thumbnailList.add(myFileAccess.thumbnailcircle);
                    //members[i] = myFileAccess.thumbnailcircle.getAbsolutePath();
                    Log.d("debug","サムネ有：" +  myFileAccess.thumbnailcircle);
                }else if(myFileAccess.thumbnail.exists()){
                    thumbnailList.add(myFileAccess.thumbnail);
                    //members[i] = myFileAccess.thumbnail.getAbsolutePath();
                    Log.d("debug","サムネ有：" +  myFileAccess.thumbnail);
                }
            }
        }
        return thumbnailList;
    }





    /**********************************/
    /*****    private method      *****/
    /**********************************/



}
