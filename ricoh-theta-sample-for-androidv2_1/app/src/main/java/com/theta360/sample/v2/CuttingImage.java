package com.theta360.sample.v2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static java.lang.Math.cos;


public class CuttingImage {

    CuttingImage(){

    }


    public ArrayList cut(Bitmap bmp1, double pitch_roll_yaw[] ,double W, double H , int mode, ArrayList<Integer> number,double pixel_width){

        int output_num=0;
        if(mode == 1){
            output_num = number.size();
        }

        double B = pixel_width * H / W;

        ArrayList<Bitmap> Out_bmp = new ArrayList<Bitmap>();
        Bitmap bmp2 = Bitmap.createScaledBitmap(bmp1,5376,2688,false);

        int num=0;
        double alpha,beta;
        double alpha_bu=0;

        double beta_bu=Math.PI / 2 / 4;
        vector1 Xh = new vector1();
        vector1 Xd = new vector1();
        vector1 Sx = new vector1();
        vector1 Sx1 = new vector1();

        double pitch,roll,yaw;
        pitch = -pitch_roll_yaw[0]/360.0*Math.PI*2.0;
        roll = pitch_roll_yaw[1]/360.0*Math.PI*2.0;
        yaw = -pitch_roll_yaw[2]/360.0*Math.PI*2.0;
        Log.i("Debug", "("+pitch_roll_yaw);
        Log.i("Debug", "("+pitch);
        Log.i("Debug", "("+roll);

        double pixel_base_length = H / pixel_width;//1ピクセル当たりの三次元空間上での距離
        double far_length = Math.sqrt(H/2*H/2 + W/2 * W/2);
        //int testtest =0;


        for(beta=-Math.PI/2*3/4; beta < Math.PI/2*4/4 ;beta = beta + beta_bu) {

            if (beta == -Math.PI / 2 * 2 / 4 || beta == Math.PI / 2 * 2 / 4) {
                alpha_bu = 2.0 * Math.PI / 6.0;
                //Log.i("Debug", "条件1です。");
            } else if (beta == -Math.PI / 2 * 2 / 4 + Math.PI / 2 / 4 || beta == Math.PI / 2 * 2 / 4 - Math.PI / 2 / 4) {
                alpha_bu = 2 * Math.PI / 8;
                //Log.i("Debug", "条件2です。");
            }else if(beta ==0){
                alpha_bu = 2 * Math.PI / 12;
                //Log.i("Debug", "条件3です。");
            }else if(beta == -Math.PI / 2 * 3 / 4 || beta == Math.PI / 2 * 3 / 4) {
                alpha_bu = 2 * Math.PI / 3;
                //Log.i("Debug", "条件4です。");
            }

            for (alpha = 0.0; alpha < Math.PI * 2.0*0.99 ; alpha = alpha+alpha_bu ) {

                //モード1のときだけ、ループを飛ばすために実施
                if(mode==1){
                    int Flag=0;
                    for(int i=0;i < output_num;i++ ){
                        if(num == number.get(i)) {
                            Flag=1;
                        }
                    }
                    if(Flag==0) {
                        num++;
                        continue;
                    }
                }
                Bitmap bmp3 = Bitmap.createBitmap((int)pixel_width,(int)B,Bitmap.Config.ARGB_8888);
                Log.i("Debug", "("+num +"枚目)");
                //Log.i("Debug", "alphaは"+alpha+"です");
                //Log.i("Debug", "alpha_bu"+alpha_bu +"です");

                for (double a = 0; a < pixel_width; a++) {
                    // Log.i("Debug", "ピクセル座標("+a +")");
                    for (double b = 0; b < B; b++) {
                        //  Log.i("Debug", "ピクセル座標("+a +"," +b+")");

                        //求めるピクセルの三次元空間での座標
                        double length = Math.sqrt((pixel_width - 2 * a) / pixel_width * W / 2 * (pixel_width - 2 * a) / pixel_width * W / 2+
                                (B - 2 * b) / B * H / 2 *  (B - 2 * b) / B * H / 2);

                        //double Hosei = (length/far_length* (Teisu.Hosei) +1.0) *(length/far_length* (Teisu.Hosei) +1.0) ;
                        double Hosei = length/far_length* (Teisu.Hosei) +1.0  ;
                        //double Hosei = Math.log( (length/far_length* (Teisu.Hosei) +1.0) ) ;
                        //Log.i("Debug", "length"+length);
                        //Log.i("Debug", "farlength"+far_length);

                        //Log.i("Debug", "Hosei"+Hosei);

                        //Hosei = 1.0;

                        //Xh.Enter(Teisu.r, Hosei* (pixel_width - 2 * a) / pixel_width * W / 2,   Hosei * length*(B - 2 * b) / B * H / 2);
                        Xh.Enter(Teisu.r, Hosei* (pixel_width - 2 * a) / pixel_width * W / 2,   Hosei * (B - 2 * b) / B * H / 2);

                        /*
                        double theta_y,theta_z;
                        theta_y = (pixel_width - 2 * a) / pixel_width * W / 2 / Teisu.r;
                        theta_z = (B - 2 * b) / B * H / 2 / Teisu.r;

                        Xh.Enter(Teisu.r *Math.cos(theta_z) * Math.cos(theta_y),
                                Teisu.r *Math.cos(theta_z) * Math.sin(theta_y),
                                Teisu.r * Math.sin(theta_z));
                                */

                        //上記座標を回転させる
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

                        //pitch,rollの回転を考慮
                        X_temp = Sx.x;
                        Y_temp = Sx.y * Math.cos(roll) - Sx.z * Math.sin(roll);
                        Z_temp = Sx.y * Math.sin(roll) + Sx.z * Math.cos(roll);

                        Sx1.Enter(X_temp * Math.cos(pitch) + Z_temp * Math.sin(pitch),
                                Y_temp,
                                -X_temp * Math.sin(pitch) + Z_temp * Math.cos(pitch));

                        //Log.i("Debug", "("+Sx.x+","+Sx.y+","+Sx.z+")");

                        //Log.i("Debug", "A("+A);
                        /*
                        Log.i("Debug", "Xh("+Xh.x+","+Xh.y+","+Xh.z+")");
                        Log.i("Debug", "Xd("+Xd.x+","+Xd.y+","+Xd.z+")");
                        Log.i("Debug", "Sx1("+Sx1.x+","+Sx1.y+","+Sx1.z+")");
                        */

                        double theta, gamma;
                        if (Sx1.y >= 0) {
                            theta = Math.acos(Sx1.x / Math.sqrt(Sx1.x * Sx1.x + Sx1.y * Sx1.y));
                            //cout << "aa" << endl;
                        } else {
                            theta = 2 * Math.PI - Math.acos(Sx1.x / Math.sqrt(Sx1.x * Sx1.x + Sx1.y * Sx1.y));
                        }

                        gamma = Math.atan(Sx1.z / Math.sqrt(Sx1.x * Sx1.x + Sx1.y * Sx1.y));

                        double x, y;
                        x =  5376-(theta / (2 * Math.PI) * 5376-1);
                        y = -gamma / (Math.PI / 2) * 2688.0 / 2.0 + 2688.0 / 2.0;
                        if(x>=5376) {
                            x=x-5376;
                            //Log.i("Debug","5376超えました" );

                        }else
                        if(x<0) {
                            x = 5376+x;
                            //Log.i("Debug","0下回りました" );

                        }
                        if(y>=2688) {
                            y=y-2688;
                            //Log.i("Debug","5376超えました" );

                        }else
                        if(y<0) {
                            y = 2688+y;
                            //Log.i("Debug","0下回りました" );

                        }
                        //Log.i("Debug", "出力画像ピクセル(" + a + "," + b + ")");
                        //Log.i("Debug", "ピクセル(" + x + "," + y + ")");

                        bmp3.setPixel((int)a, (int)b, bmp2.getPixel((int) x,(int)y));

                    }
                }
                Out_bmp.add(bmp3);
                num = num+1;

                if (beta == -Math.PI / 2 * 2 / 4 || beta == Math.PI / 2 * 2 / 4) {
                    alpha_bu = 2.0 * Math.PI / 6.0;
                    //Log.i("Debug", "条件1です。");
                } else if (beta == -Math.PI / 2 * 2 / 4 + Math.PI / 2 / 4 || beta == Math.PI / 2 * 2 / 4 - Math.PI / 2 / 4) {
                    alpha_bu = 2 * Math.PI / 8;
                    //Log.i("Debug", "条件2です。");
                }else if(beta ==0){
                    alpha_bu = 2 * Math.PI / 12;
                    //Log.i("Debug", "条件3です。");
                }else if(beta == -Math.PI / 2 * 3 / 4 || beta == Math.PI / 2 * 3 / 4) {
                    alpha_bu = 2 * Math.PI / 3;
                    //Log.i("Debug", "条件4です。");
                }

            }
        }
        return Out_bmp;
    }

    public Bitmap cut_one(Bitmap bmp1, double pitch_roll_yaw[] ,double W, double H ,double pixel_width, int cut_number) {

        double B = pixel_width * H / W;

        Bitmap bmp2 = Bitmap.createScaledBitmap(bmp1,5376,2688,false);
        Bitmap bmp = Bitmap.createBitmap((int)pixel_width,(int)pixel_width,Bitmap.Config.ARGB_8888);

        int num=0;
        double alpha,beta;
        double alpha_bu=0;

        double beta_bu=Math.PI / 2 / 4;
        vector1 Xh = new vector1();
        vector1 Xd = new vector1();
        vector1 Sx = new vector1();
        vector1 Sx1 = new vector1();

        double pitch,roll,yaw;
        pitch = -pitch_roll_yaw[0]/360.0*Math.PI*2.0;
        roll = pitch_roll_yaw[1]/360.0*Math.PI*2.0;
        yaw = -pitch_roll_yaw[2]/360.0*Math.PI*2.0;
        Log.i("Debug", "("+pitch_roll_yaw);
        Log.i("Debug", "("+pitch);
        Log.i("Debug", "("+roll);

        double pixel_base_length = H / pixel_width;//1ピクセル当たりの三次元空間上での距離
        double far_length = Math.sqrt(H/2*H/2 + W/2 * W/2);
        //int testtest =0;


        for(beta=-Math.PI/2*3/4; beta < Math.PI/2*4/4 ;beta = beta + beta_bu) {

            if (beta == -Math.PI / 2 * 2 / 4 || beta == Math.PI / 2 * 2 / 4) {
                alpha_bu = 2.0 * Math.PI / 6.0;
                //Log.i("Debug", "条件1です。");
            } else if (beta == -Math.PI / 2 * 2 / 4 + Math.PI / 2 / 4 || beta == Math.PI / 2 * 2 / 4 - Math.PI / 2 / 4) {
                alpha_bu = 2 * Math.PI / 8;
                //Log.i("Debug", "条件2です。");
            }else if(beta ==0){
                alpha_bu = 2 * Math.PI / 12;
                //Log.i("Debug", "条件3です。");
            }else if(beta == -Math.PI / 2 * 3 / 4 || beta == Math.PI / 2 * 3 / 4) {
                alpha_bu = 2 * Math.PI / 3;
                //Log.i("Debug", "条件4です。");
            }

            for (alpha = 0.0; alpha < Math.PI * 2.0*0.99 ; alpha = alpha+alpha_bu ) {

                if(num != cut_number){
                    num++;
                    continue;
                }

                Log.i("Debug", "("+num +"枚目)");
                //Log.i("Debug", "alphaは"+alpha+"です");
                //Log.i("Debug", "alpha_bu"+alpha_bu +"です");

                for (double a = 0; a < pixel_width; a++) {
                    // Log.i("Debug", "ピクセル座標("+a +")");
                    for (double b = 0; b < B; b++) {
                        //  Log.i("Debug", "ピクセル座標("+a +"," +b+")");

                        //求めるピクセルの三次元空間での座標
                        double length = Math.sqrt((pixel_width - 2 * a) / pixel_width * W / 2 * (pixel_width - 2 * a) / pixel_width * W / 2+
                                (B - 2 * b) / B * H / 2 *  (B - 2 * b) / B * H / 2);

                        //double Hosei = (length/far_length* (Teisu.Hosei) +1.0) *(length/far_length* (Teisu.Hosei) +1.0) ;
                        double Hosei = length/far_length* (Teisu.Hosei) +1.0  ;
                        //double Hosei = Math.log( (length/far_length* (Teisu.Hosei) +1.0) ) ;
                        //Log.i("Debug", "length"+length);
                        //Log.i("Debug", "farlength"+far_length);

                        //Log.i("Debug", "Hosei"+Hosei);

                        //Hosei = 1.0;

                        //Xh.Enter(Teisu.r, Hosei* (pixel_width - 2 * a) / pixel_width * W / 2,   Hosei * length*(B - 2 * b) / B * H / 2);
                        Xh.Enter(Teisu.r, Hosei* (pixel_width - 2 * a) / pixel_width * W / 2,   Hosei * (B - 2 * b) / B * H / 2);

                        /*
                        double theta_y,theta_z;
                        theta_y = (pixel_width - 2 * a) / pixel_width * W / 2 / Teisu.r;
                        theta_z = (B - 2 * b) / B * H / 2 / Teisu.r;

                        Xh.Enter(Teisu.r *Math.cos(theta_z) * Math.cos(theta_y),
                                Teisu.r *Math.cos(theta_z) * Math.sin(theta_y),
                                Teisu.r * Math.sin(theta_z));
                                */

                        //上記座標を回転させる
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

                        //pitch,rollの回転を考慮
                        X_temp = Sx.x;
                        Y_temp = Sx.y * Math.cos(roll) - Sx.z * Math.sin(roll);
                        Z_temp = Sx.y * Math.sin(roll) + Sx.z * Math.cos(roll);

                        Sx1.Enter(X_temp * Math.cos(pitch) + Z_temp * Math.sin(pitch),
                                Y_temp,
                                -X_temp * Math.sin(pitch) + Z_temp * Math.cos(pitch));

                        //Log.i("Debug", "("+Sx.x+","+Sx.y+","+Sx.z+")");

                        //Log.i("Debug", "A("+A);
                        /*
                        Log.i("Debug", "Xh("+Xh.x+","+Xh.y+","+Xh.z+")");
                        Log.i("Debug", "Xd("+Xd.x+","+Xd.y+","+Xd.z+")");
                        Log.i("Debug", "Sx1("+Sx1.x+","+Sx1.y+","+Sx1.z+")");
                        */

                        double theta, gamma;
                        if (Sx1.y >= 0) {
                            theta = Math.acos(Sx1.x / Math.sqrt(Sx1.x * Sx1.x + Sx1.y * Sx1.y));
                            //cout << "aa" << endl;
                        } else {
                            theta = 2 * Math.PI - Math.acos(Sx1.x / Math.sqrt(Sx1.x * Sx1.x + Sx1.y * Sx1.y));
                        }

                        gamma = Math.atan(Sx1.z / Math.sqrt(Sx1.x * Sx1.x + Sx1.y * Sx1.y));

                        double x, y;
                        x =  5376-(theta / (2 * Math.PI) * 5376-1);
                        y = -gamma / (Math.PI / 2) * 2688.0 / 2.0 + 2688.0 / 2.0;
                        if(x>=5376) {
                            x=x-5376;
                            //Log.i("Debug","5376超えました" );

                        }else
                        if(x<0) {
                            x = 5376+x;
                            //Log.i("Debug","0下回りました" );

                        }
                        if(y>=2688) {
                            y=y-2688;
                            //Log.i("Debug","5376超えました" );

                        }else
                        if(y<0) {
                            y = 2688+y;
                            //Log.i("Debug","0下回りました" );

                        }
                        //Log.i("Debug", "出力画像ピクセル(" + a + "," + b + ")");
                        //Log.i("Debug", "ピクセル(" + x + "," + y + ")");

                        bmp.setPixel((int)a, (int)b, bmp2.getPixel((int) x,(int)y));

                    }
                }
                num = num+1;

                if (beta == -Math.PI / 2 * 2 / 4 || beta == Math.PI / 2 * 2 / 4) {
                    alpha_bu = 2.0 * Math.PI / 6.0;
                    //Log.i("Debug", "条件1です。");
                } else if (beta == -Math.PI / 2 * 2 / 4 + Math.PI / 2 / 4 || beta == Math.PI / 2 * 2 / 4 - Math.PI / 2 / 4) {
                    alpha_bu = 2 * Math.PI / 8;
                    //Log.i("Debug", "条件2です。");
                }else if(beta ==0){
                    alpha_bu = 2 * Math.PI / 12;
                    //Log.i("Debug", "条件3です。");
                }else if(beta == -Math.PI / 2 * 3 / 4 || beta == Math.PI / 2 * 3 / 4) {
                    alpha_bu = 2 * Math.PI / 3;
                    //Log.i("Debug", "条件4です。");
                }

            }
        }
        return bmp;

    }

    public Bitmap ball_cut(Bitmap bmp1,double pixel_width,double pitch_roll_yaw[]) {

        double Height = 2688;
        double Width = 5376;
        Bitmap bmp = Bitmap.createBitmap((int)pixel_width,(int)pixel_width,Bitmap.Config.ARGB_8888);
        Bitmap bmp2 = Bitmap.createScaledBitmap(bmp1,5376,2688,false);

         /*
        int color2 = bmp2.getPixel(5370,100);
        Log.i("Debug", "カラー("+color2 +")");
        黒 -16777216
        白 -1
        */

        double pitch,roll,yaw;
        pitch = -pitch_roll_yaw[0]/360.0*Math.PI*2.0;
        roll = pitch_roll_yaw[1]/360.0*Math.PI*2.0;
        yaw = -pitch_roll_yaw[2]/360.0*Math.PI*2.0;

        vector1 Sx1 = new vector1();
        vector1 Sx_temp = new vector1();
        vector1 Sx = new vector1();

        double image_R;
        image_R = pixel_width/2;
        for (double a = 0; a < pixel_width; a++) {
            // Log.i("Debug", "ピクセル座標("+a +")");
            for (double b = 0; b < pixel_width; b++) {

                double length =Math.sqrt( (image_R-a)*(image_R-a) + (image_R-b)*(image_R-b));
                double x,y,theta_d,theta;
                if(length > image_R) {
                    bmp.setPixel((int)a, (int)b, -16777216);
                    continue;
                }
                y = (1.0-length / image_R)* 2688.0;
                theta_d = Math.acos((a-image_R)/length);
                if(b > image_R){
                    theta = Math.PI * 2 - theta_d;
                }else{
                    theta = theta_d;
                }
                x = 5375 - 5376 * theta / (2 * Math.PI);

                //x = 5376 - 5376 * theta / (2 * Math.PI);
                //Log.i("Debug", "ピクセル座標("+x +")");

                double alpha,beta,X_temp,Y_temp,Z_temp;
                alpha = x / Width * 2 * Math.PI;
                beta = Math.PI * (Height / 2 - y) / 2688;
                Sx.Enter(Math.cos(alpha) * Math.cos(beta) , Math.sin(alpha) * Math.cos(beta) , Math.sin(beta) );

                X_temp = Sx.x;
                Y_temp = Sx.y * Math.cos(roll) - Sx.z * Math.sin(roll);
                Z_temp = Sx.y * Math.sin(roll) + Sx.z * Math.cos(roll);

                Sx1.Enter(X_temp * Math.cos(pitch) + Z_temp * Math.sin(pitch),
                        Y_temp,
                        -X_temp * Math.sin(pitch) + Z_temp * Math.cos(pitch));

                double theta_s, gamma;
                if (Sx1.y >= 0) {
                    theta_s = Math.acos(Sx1.x / Math.sqrt(Sx1.x * Sx1.x + Sx1.y * Sx1.y));
                    //cout << "aa" << endl;
                } else {
                    theta_s = 2 * Math.PI - Math.acos(Sx1.x / Math.sqrt(Sx1.x * Sx1.x + Sx1.y * Sx1.y));
                }
                gamma = Math.atan(Sx1.z / Math.sqrt(Sx1.x * Sx1.x + Sx1.y * Sx1.y));
                        /*
                        if (Sx.y >= 0) {
                            theta = Math.acos(Sx.x / Math.sqrt(Sx.x * Sx.x + Sx.y * Sx.y));
                            //cout << "aa" << endl;
                        } else {
                            theta = 2 * Teisu.PI - Math.acos(Sx.x / Math.sqrt(Sx.x * Sx.x + Sx.y * Sx.y));
                        }
                        //if(Sx.y<0)	theta = 2*PI- acos(Sx.x/R);
                        //cout << "bb" << endl;
                        gamma = Math.atan(Sx.z / Math.sqrt(Sx.x * Sx.x + Sx.y * Sx.y));*/
                //cout << theta <<","<< gamma <<endl;
                //cout << gamma <<endl;

                double x_, y_;
                x_ =  (theta_s / (2 * Math.PI) * 5376-1)-1;
                //x_ = 5376 - (theta_s / (2 * Math.PI) * 5376-1);

                y_ = -gamma / (Math.PI / 2) * 2688.0 / 2.0 + 2688.0 / 2.0;
                if(x_>=5376) {
                    x_=x_-5376;
                    //Log.i("Debug","5376超えました" );

                }else
                if(x_<0) {
                    x_ = 5376+x_;
                    //Log.i("Debug","0下回りました" );

                }
                if(y_>=2688) {
                    y_ = y_ -2688;
                    //Log.i("Debug","5376超えました" );

                }else
                if(y_ < 0) {
                    y_ = 2688 + y_;
                    //Log.i("Debug","0下回りました" );
                }

                if(x_>=5376 || y_>=2688 ) {
                    bmp.setPixel((int)a, (int)b, bmp1.getPixel((int) a-1,(int)b));
                    //Log.i("Debug", "x,yピクセル座標("+x +","+ y+")");
                    //Log.i("Debug", "a,bピクセル座標("+a +","+ b+")");
                }else{

                    bmp.setPixel((int)a, (int)b, bmp2.getPixel((int) x_,(int)y_));
                }
            }
        }

        return bmp;
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
    public static final double r = 0.25;//切り取る面の中心からの距離
    public static final double R = 1.0;//球の半径

    public static final double Hosei = 1.0;//外側にいくと大きくなるひずみをなくすための補正

    public static final double A = 120;//画像のピクセル数決定
    //public static final double B = 720;
    //public static final double W = 0.8;

}