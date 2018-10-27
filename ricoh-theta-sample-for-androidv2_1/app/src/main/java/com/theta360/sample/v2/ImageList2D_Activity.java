package com.theta360.sample.v2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

public class ImageList2D_Activity extends Activity {

    // 表示する画像の名前（拡張子無し）
    private String[] members = new String[10];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_list2d);

        Log.d("debug","*** Start ImageList2D_Activity ***");

        Intent intent = getIntent();
        final String fileId = intent.getStringExtra("OBJECT_ID");

        MyFileAccess myFileAccess = new MyFileAccess(fileId);
        Log.d("debug","2D画像保存先 = "+ myFileAccess.image2D);

        final ArrayList<File> image2DFiles = new ArrayList<File>();

        ArrayList<File> foodImage2DList = myFileAccess.getFoodImage2D();
        ArrayList<File> humanImage2DList = myFileAccess.getHumanImage2D();
        for(int i = 0; i<foodImage2DList.size(); i++){
            image2DFiles.add(humanImage2DList.get(i));
            image2DFiles.add(foodImage2DList.get(i));
        }

        // GridViewのインスタンスを生成
        GridView gridview = (GridView) findViewById(R.id.gridview);

        //グリッド2列表示
        gridview.setNumColumns(2);
        //表示する画像を取得
        ArrayList<Bitmap> lstBitmap = new ArrayList<Bitmap>();

        for(File file: image2DFiles) {
            byte[] byteArray = convertFile(file);
            lstBitmap.add(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length));
        }

        Log.d("debug","lstBitmap size = " + lstBitmap.size());
        //アダプター作成
        BitmapAdapter adapter = new BitmapAdapter(getApplicationContext(), lstBitmap);

        //グリッドにアダプタを設定
        gridview.setAdapter(adapter);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplication(), Image2D_Activity.class);
                intent.putExtra("filepath",image2DFiles.get(position).getAbsolutePath());
                Log.d("debug",image2DFiles.get(position).getAbsolutePath());
                startActivity(intent);
            }
        });


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