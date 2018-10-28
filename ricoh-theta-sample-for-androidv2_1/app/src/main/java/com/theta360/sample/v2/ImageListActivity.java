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
			ArrayList<Bitmap> image2d_bitmap_a = new ArrayList<Bitmap>();

			image2d_bitmap_a = cutter.cut(image360_bitmap, pitch_roll_yaw,0.5, 0.5, 0,null,150);
			Log.d("debug","Complete cut image");


			//TODO 上位何個を出力するか定数として一か所で定義するか検討
			int output_num = 3; //<変更>

			//TODO モデル読み込みは事前の実施が可能(位置変更可)
			//モデル読み込み
			MyTensorFlow2 myTensorFlow = new MyTensorFlow2();
			myTensorFlow.modelCreate();

			//TODO ビットマップの数と合わせる
			//切り取った画像を認識してinstabaeを抽出(resultsに代入)
			for(int i = 0; i < image2d_bitmap_a.size(); i++){
				myTensorFlow.startRecognition(image2d_bitmap_a.get(i), Integer.toString(i));
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


			//上で分かった識別率の高い画像だけ画質を上げる　<変更>
			for(int i = 0; i < output_num; i++){
				image2d_bitmap_a.set( output_photos_food.get(i),
						cutter.cut_one(image360_bitmap, pitch_roll_yaw,0.5, 0.5,360,output_photos_food.get(i)));
				image2d_bitmap_a.set( output_photos_human.get(i),
						cutter.cut_one(image360_bitmap, pitch_roll_yaw,0.5, 0.5,360,output_photos_human.get(i)));

			}

			//モデルクローズ
			myTensorFlow.endRecognition();

			Log.d("debug","Complete cut image");


            for(int i=0; i<output_num; i++) {
                File image2d_human = new File(myFileAccess.image2D + "/image2d_human_No_" + i  + "_" + myTensorFlow.results_human.get(i).getConfidence() + myFileAccess.fileid + ".JPG");
                myFileAccess.storeImage2D(image2d_bitmap_a.get(i),image2d_human);

                File image2d_food = new File(myFileAccess.image2D + "/image2d_food_No_" + i + "_" + myTensorFlow.results_food.get(i).getConfidence() + myFileAccess.fileid + ".JPG");
                myFileAccess.storeImage2D(image2d_bitmap_a.get(i),image2d_food);
            }

			Log.d("debug", "*** END instakePhotos Task ***");
			return null;
		}



	}

	public class MyTensorFlow2 {
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

		MyTensorFlow2() {
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