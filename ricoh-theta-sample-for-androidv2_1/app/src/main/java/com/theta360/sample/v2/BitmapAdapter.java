package com.theta360.sample.v2;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.util.List;

public class BitmapAdapter extends ArrayAdapter<Bitmap> {

    //GridView内で画像を表示するために作成したレイアウト
    private static final int RESOURCE_ID = R.layout.grid_items;

    private LayoutInflater mInflater;

    public BitmapAdapter(Context context, List<Bitmap> objects) {
        super(context, RESOURCE_ID, objects);
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(RESOURCE_ID, null);
        }
        ImageView imageView = (ImageView) convertView.findViewById(R.id.image_view);
        imageView.setImageBitmap(getItem(position));
        return convertView;
    }
}
