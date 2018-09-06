package com.theta360.sample.v2;


import android.graphics.Bitmap;
import android.util.Log;

import static java.lang.Math.cos;


public class CuttingImage {

    CuttingImage(){

    }


    public Bitmap[] cut(Bitmap bmp1,double W, double H, double alpha_bu, double beta_bu) {

        double B = Teisu.A * H / W;
        //double kakudo = Teisu.PI;
        //double bunkai = 360;
        Bitmap [] Out_bmp =new Bitmap[50];
        Bitmap bmp2 = Bitmap.createScaledBitmap(bmp1,5376,2688,false);


        int num=0;
        double alpha,beta;
        vector1 Xh = new vector1();
        vector1 Xd = new vector1();
        vector1 Sx = new vector1();

        for(beta=-Math.PI/2*2/4; beta < Math.PI/2*3/4 ;beta = beta + beta_bu) {
            for (alpha = 0; alpha < Math.PI * 2; alpha = alpha + alpha_bu) {
                Out_bmp[num] = Bitmap.createBitmap((int)Teisu.A,(int)B,Bitmap.Config.ARGB_8888);
                //Out_bmp[num] = Bitmap.createScaledBitmap(bmp1,720,720,false);
                Log.i("Debug", "("+num +"枚目)");

                for (double a = 0; a < Teisu.A; a++) {
                    // Log.i("Debug", "ピクセル座標("+a +")");
                    for (double b = 0; b < B; b++) {
                        //  Log.i("Debug", "ピクセル座標("+a +"," +b+")");

                        Xh.Enter(Teisu.r, (Teisu.A - 2 * a) / Teisu.A * W / 2, (B - 2 * b) / B * H / 2);
                        //Log.i("Debug", "Xhに代入");

                        double X_temp, Y_temp, Z_temp;

                        X_temp = Xh.x * Math.cos(beta) + Xh.z * Math.sin(beta);
                        Z_temp = -Xh.x * Math.sin(beta) + Xh.z * Math.cos(beta);
                        Y_temp = Xh.y;

                        Xd.Enter(X_temp * Math.cos(alpha) - Y_temp * Math.sin(alpha),
                                X_temp * Math.sin(alpha) + Y_temp * Math.cos(alpha),
                                Z_temp);

                        /*
                        X_temp = Xh.x * Math.cos(alpha) - Xh.y * Math.sin(alpha);
                        Y_temp = Xh.x * Math.sin(alpha) + Xh.y * Math.cos(alpha);
                        Z_temp = Xh.z;

                        Xd.Enter(X_temp * Math.cos(beta) + Z_temp * Math.sin(beta),
                                Y_temp,
                                -X_temp * Math.sin(beta) + Z_temp * Math.cos(beta));
                        */
                        //Xd.print();
                        //Log.i("Debug", "行列回転");
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
                        x =  5376-(theta / (2 * Teisu.PI) * 5376-1);
                        y = -gamma / (Teisu.PI / 2) * 2688.0 / 2.0 + 2688.0 / 2.0;
                        if(x>=5376) {
                            x=5375;
                        }
                        if(x<0) {
                            x=0;
                        }
                        //Log.i("Debug", "出力画像ピクセル(" + a + "," + b + ")");
                        //Log.i("Debug", "ピクセル(" + x + "," + y + ")");
                        Out_bmp[num].setPixel((int)a, (int)b, bmp2.getPixel((int) x,(int)y));

                    }
                }
                num =num+1;
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
    void Enter(double a,double b,double c){
        x=a;
        y=b;
        z=c;
    }
}

class Teisu {
    public static final double r = 0.6;
    public static final double R = 1.0;

    public static final double A = 240;
    //public static final double B = 720;
    //public static final double W = 0.8;

    public static final double PI = 3.141592653589;

}