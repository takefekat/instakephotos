package com.theta360.sample.v2;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.VideoView;

import java.io.File;

public class StartMenuActivity extends Activity
        implements View.OnClickListener{  //クリックリスナーを実装

    @Override
    protected void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_menu);
        findViewById(R.id.imageButton).setOnClickListener(this);  //リスナーをボタンに登録

        final VideoView videoView = (VideoView) findViewById(R.id.video);

        videoView.setVideoPath("android.resource://" + this.getPackageName() + "/" + R.raw.menu);
        videoView.start();

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer mp) {
                // 先頭に戻す
                videoView.seekTo(0);
                // 再生開始
                videoView.start();
            }
        });
    }

    //ボタンが押された時の処理
    public void onClick (View view){
        //ここに遷移するための処理を追加する
        Intent intent = new Intent(this, ImageListActivity.class);
        //finish();
        startActivity(intent);
    }

    @Override
    public void onRestart(){
        super.onRestart();
        //Log.v("LifeCycle", "onRestart");
        final VideoView videoView = (VideoView) findViewById(R.id.video);
        videoView.setVideoPath("android.resource://" + this.getPackageName() + "/" + R.raw.menu);
        videoView.start();

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer mp) {
                // 先頭に戻す
                videoView.seekTo(0);
                // 再生開始
                videoView.start();
            }
        });
    }

}
