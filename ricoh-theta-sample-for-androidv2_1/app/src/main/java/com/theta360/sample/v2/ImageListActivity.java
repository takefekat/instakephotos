package com.theta360.sample.v2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.theta360.sample.v2.network.HttpConnector;
import com.theta360.sample.v2.network.HttpEventListener;
import com.theta360.sample.v2.view.ImageListArrayAdapter;
import com.theta360.sample.v2.view.ImageRow;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageListActivity extends Activity {

	// 表示する画像の名前（拡張子無し）
	private String[] members = new String[1000];
	private String cameraIpAddress;
	private ImageListActivity.LoadObjectListTask sampleTask = null;
	private String fileId;
	private byte[] thumbnail;
//    private ImageList360Activity.GetImageSizeTask getImageSizeTask = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_object_list);

		Log.d("debug","*** Start ImageList2D_Activity ***");

		//Log.d("debug","2D画像保存先　：　" + fileId);

		String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/image/thumbnail/";
		Log.d("debug","360画像保存先 = "+storagePath);

		final File[] _files = new File(storagePath).listFiles();
		final ArrayList<File> files = new ArrayList<File>();

		Log.d("debug","360 image数 = "+Integer.toString(_files.length));
		for (int i=0; i<_files.length; i++) {
			if(_files[i].isFile()) {
				files.add(_files[i]);
				members[i] = _files[i].getName();
				Log.d("debug",_files[i].getName());
			}
		}
		Log.d("debug","_files.size() = " + Integer.toString(_files.length));
		// GridViewのインスタンスを生成
		GridView gridview = (GridView) findViewById(R.id.gridview2);
		Log.d("debug","_files.size() = " + Integer.toString(_files.length));
		//グリッド2列表示
		gridview.setNumColumns(3);
		Log.d("debug","_files.size() = " + Integer.toString(_files.length));
		//表示する画像を取得
		ArrayList<Bitmap> lstBitmap = new ArrayList<Bitmap>();

		for(File file: files) {
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
				cameraIpAddress = getResources().getString(R.string.theta_ip_address);
				fileId = thumnail2image360( _files[position].getAbsolutePath());
				thumbnail = convertFile(files.get(position));
				GLPhotoActivity.startActivityForResult(ImageListActivity.this, cameraIpAddress, fileId, thumbnail, false);
			}
		});
	}

	private String thumnail2image360(String string) {
		return  Environment.getExternalStorageDirectory().getAbsolutePath() +
				"/image/image360/"+
				string.substring(string.length()-12);
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

}