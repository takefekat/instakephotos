package com.theta360.sample.v2;

import android.graphics.Bitmap;
import static java.lang.Math.cos;

public class CuttingImage {

    CuttingImage(){

    }

    public Bitmap cut(Bitmap bmp1,double W, double H, double alpha, double beta) {

        double B = Teisu.A * H / W;
        double kakudo = Teisu.PI;
        double bunkai = 360;
        //BitmapFactory.Options options = new BitmapFactory.Options();
        //options.inJustDecodeBounds = true;

        Bitmap Out_bmp;
        Bitmap bmp2 = Bitmap.createScaledBitmap(bmp1,5376,2688,false);
        Out_bmp = Bitmap.createScaledBitmap(bmp1,(int)Teisu.A,(int) B,false);
        //Bitmap bmp2 =new Bitmap();
        //ImageView iv1 = (ImageView)findViewById(R.id.image_view_2);
        //iv1.setImageBitmap(bmp1);


        //ImageView iv1 = new ImageView();

        //ImageView iv1 = (ImageView)findViewById(R.id.image_view_2);
        //iv1.setImageBitmap(bmp1);

        //Mat Img1 =new Mat();
        //Mat Img2 =new Mat();
        //Mat Img1 = Imgcodecs.imread("C:\\Users\\shingo\\Documents\\AndroidStudio\\OpenCVtest0811\\app\\src\\main\\res\\drawable");
        /*
        Img1 = Imgcodecs.imread(p);
        File dir = new File(p);
        String path_out = "C:\\Users\\shingo\\Documents\\AndroidStudio\\OpenCVtest0811\\app\\src\\main\\res\\drawable\\ajikna_gray.jpg";

        if (dir.exists()) {
            //Bitmap bmp = BitmapFactory.decodeFile(dir.getPath());
            Log.i("OpenCV", "ファイルあり"+dir.getPath());
        }else {
            Log.i("OpenCV", "ファイルなし");
        }
        */
        //Imgproc.cvtColor(Img1,Img2 , Imgproc.COLOR_BGR2GRAY); // カラー画像をグレー画像に変換
        //Highgui.imwrite(path_out, Img2);


        //double H = W * A;
        /*
        int data,data1;

        data = bmp1.getHeight();
        System.out.println(data+"データ");

        data = bmp1.getPixel(100,100);
        System.out.println(data+"データピクセル");*/

        double angle = 0;

        for (double a = 0; a < Teisu.A; a++) {
            for (double b = 0; b < B; b++) {

                //vector1 Xh=null, Xd=null, Sx=null;
                vector1 Xh = new vector1();
                vector1 Xd = new vector1();
                vector1 Sx = new vector1();

                Xh.Enter(Teisu.r, (Teisu.A - 2 * a) / Teisu.A * W / 2, (B - 2 * b) / B * H / 2);

                double X_temp, Y_temp, Z_temp;
                X_temp = Xh.x * Math.cos(beta) + Xh.z * Math.sin(beta);
                Z_temp = -Xh.x * Math.sin(beta) + Xh.z * Math.cos(beta);
                Y_temp = Xh.y;

                Xd.Enter(X_temp * cos(alpha) - Y_temp * Math.sin(alpha),
                        X_temp * Math.sin(alpha) + Y_temp * Math.cos(alpha),
                        Xh.z);
                //Xd.print();
                double t;
                t = Teisu.R / Math.sqrt(Xd.x * Xd.x + Xd.y * Xd.y + Xd.z * Xd.z);

                Sx.Enter(t * Xd.x, t * Xd.y, t * Xd.z);
                //Sx.print();
                double theta, gamma;
                if (Sx.y >= 0) {
                    theta = Math.acos(Sx.x / Math.sqrt(Sx.x * Sx.x + Sx.y * Sx.y));
                    //cout << "aa" << endl;
                } else {
                    theta = 2 * Teisu.PI - Math.acos(Sx.x / Math.sqrt(Sx.x * Sx.x + Sx.y * Sx.y));
                }
                //if(Sx.y<0)	theta = 2*PI- acos(Sx.x/R);
                //cout << "bb" << endl;
                gamma = Math.atan(Sx.z / Math.sqrt(Sx.x * Sx.x + Sx.y * Sx.y));
                //cout << theta <<","<< gamma <<endl;
                //cout << gamma <<endl;
                double x, y;
                x =  theta / (2 * Teisu.PI) * 5376;
                y = -gamma / (Teisu.PI / 2) * 2688.0 / 2.0 + 2688.0 / 2.0;

                Out_bmp.setPixel((int)a,(int)b,bmp2.getPixel((int) x,(int)y));
            }
        }

        return Out_bmp;
    }


}


class vector1{
    public
    double x;
    double y;
    double z;
    int Enter(double a,double b,double c){
        x=a;
        y=b;
        z=c;
        return 0;
    }
}

class Teisu {
    public static final double r = 0.3;
    public static final double R = 1.0;

    public static final double A = 720;
    //public static final double B = 720;
    public static final double W = 0.8;

    public static final double PI = 3.141592653589;
}