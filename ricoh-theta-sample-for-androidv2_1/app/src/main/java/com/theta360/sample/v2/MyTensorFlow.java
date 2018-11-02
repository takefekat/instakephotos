package com.theta360.sample.v2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MyTensorFlow {

    private final int SELECT_MODE;
    private final int SELECT_METHOD;
    private final Context CONTEXT_LOCAL;

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

    ArrayList<Classifier.Recognition> results_instabae = new ArrayList<Classifier.Recognition>();
    ArrayList<Classifier.Recognition> results_notinstabae = new ArrayList<Classifier.Recognition>();

    ArrayList<Classifier.Recognition> results_food = new ArrayList<Classifier.Recognition>();
    ArrayList<Classifier.Recognition> results_human = new ArrayList<Classifier.Recognition>();

    ArrayList<Classifier.Recognition> results_food_d = new ArrayList<Classifier.Recognition>();
    ArrayList<Classifier.Recognition> results_human_d = new ArrayList<Classifier.Recognition>();
    ArrayList<Classifier.Recognition> results_dasafood = new ArrayList<Classifier.Recognition>();
    ArrayList<Classifier.Recognition> results_dasahuman = new ArrayList<Classifier.Recognition>();
    ArrayList<Classifier.Recognition> results_other = new ArrayList<Classifier.Recognition>();

    MyTensorFlow(int _selectMode, int _selectMethod, Context _context) {

        SELECT_MODE = _selectMode;
        SELECT_METHOD = _selectMethod;
        CONTEXT_LOCAL = _context;

        INPUT_SIZE = 299;
        IMAGE_MEAN = 128;
        IMAGE_STD = 128;
        //TODO strip_unused.py実行時には--input_node_names, INPUT_NAME="Mul"にするべきかも
        INPUT_NAME = "Placeholder";
//            INPUT_NAME = "Mul";
        OUTPUT_NAME = "final_result";
        //TODO MODEL_FILEはどちらかを選択
        if(SELECT_MODE == 0) {
            MODEL_FILE = "file:///android_asset/retrained_graph.pb";
//  	          MODEL_FILE = "file:///android_asset/instabae_graph_android_unused.pb";
//      	      MODEL_FILE = "file:///android_asset/instabae_graph_android.pb";
            LABEL_FILE = "file:///android_asset/retrained_labels.txt";
        }else{
            MODEL_FILE = "file:///android_asset/retrained_graph_2.pb";
            LABEL_FILE = "file:///android_asset/retrained_labels_2.txt";
        }

        croppedBitmap = null;

    }

    public void modelCreate() {
        classifier = TensorFlowImageClassifier.create(
                CONTEXT_LOCAL.getAssets(),
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

        if(SELECT_MODE == 0) {

            if(SELECT_METHOD == 0) {
                int osyafood_n = -1;
                int dasafood_n = -1;
                int osyahuman_n = -1;
                int dasahuman_n = -1;
                for (int i = 0; i < results.size(); i++) {
                    if (results.get(i).getTitle().equals("osyafood")) {
                        osyafood_n = i;
                    } else if (results.get(i).getTitle().equals("osyahuman")) {
                        osyahuman_n = i;
                    } else if (results.get(i).getTitle().equals("dasafood")) {
                        dasafood_n = i;
                    } else if (results.get(i).getTitle().equals("dasahuman")) {
                        dasahuman_n = i;
                    }
                }

                for (int i = 0; i < results.size(); i++) {
                    if (results.get(i).getTitle().equals("osyafood")) {
                        results_food_d.add(results.get(i));
                    } else if (results.get(i).getTitle().equals("osyahuman")) {
                        results_human_d.add(results.get(i));
                    } else {
                        Log.d("debug", "Unmatching labels");
                        // debug用
                        if (results.get(i).getTitle().equals("dasafood")) {
                            results_dasafood.add(results.get(i));
                        }
                        if (results.get(i).getTitle().equals("dasahuman")) {
                            results_dasahuman.add(results.get(i));
                        }
                        if (results.get(i).getTitle().equals("other")) {
                            results_other.add(results.get(i));
                        }
                    }
                }
/*
                //if (results.get(osyafood_n).getConfidence() > results.get(osyahuman_n).getConfidence()) {
                    if (results.get(osyafood_n).getConfidence() > results.get(dasafood_n).getConfidence()) {
                        results_food.add(new Classifier.Recognition(
                                "" + results.get(osyafood_n).getId(), results.get(osyafood_n).getTitle(), results.get(osyafood_n).getConfidence()-results.get(dasafood_n).getConfidence(), null, results.get(osyafood_n).getImageId()));
                    } else {
                        results_food.add(new Classifier.Recognition(
                                "" + results.get(osyafood_n).getId(), results.get(osyafood_n).getTitle(), (float) -1.0, null, results.get(osyafood_n).getImageId()));
                    }
                //    results_human.add(new Classifier.Recognition(
                //            "" + results.get(osyahuman_n).getId(), results.get(osyahuman_n).getTitle(), (float) -1.0, null, results.get(osyahuman_n).getImageId()));
                //} else {
                    if (results.get(osyahuman_n).getConfidence() > results.get(dasahuman_n).getConfidence()) {
                        results_human.add(new Classifier.Recognition(
                                "" + results.get(osyahuman_n).getId(), results.get(osyahuman_n).getTitle(), results.get(osyahuman_n).getConfidence()-results.get(dasahuman_n).getConfidence(), null, results.get(osyahuman_n).getImageId()));
                    } else {
                        results_human.add(new Classifier.Recognition(
                                "" + results.get(osyahuman_n).getId(), results.get(osyahuman_n).getTitle(), (float) -1.0, null, results.get(osyahuman_n).getImageId()));
                    }
                //    results_food.add(new Classifier.Recognition(
                //            "" + results.get(osyafood_n).getId(), results.get(osyafood_n).getTitle(), (float) -1.0, null, results.get(osyafood_n).getImageId()));
                //}
                */
                /*
                if (results.get(osyafood_n).getConfidence() > results.get(osyahuman_n).getConfidence()) {
                    if (results.get(osyafood_n).getConfidence() > results.get(dasafood_n).getConfidence()) {
                        results_food.add(results.get(osyafood_n));
                    } else {
                        results_food.add(new Classifier.Recognition(
                                "" + results.get(osyafood_n).getId(), results.get(osyafood_n).getTitle(), (float) -1.0, null, results.get(osyafood_n).getImageId()));
                    }
                    results_human.add(new Classifier.Recognition(
                            "" + results.get(osyahuman_n).getId(), results.get(osyahuman_n).getTitle(), (float) -1.0, null, results.get(osyahuman_n).getImageId()));
                } else {
                    if (results.get(osyahuman_n).getConfidence() > results.get(dasahuman_n).getConfidence()) {
                        results_human.add(results.get(osyahuman_n));
                    } else {
                        results_human.add(new Classifier.Recognition(
                                "" + results.get(osyahuman_n).getId(), results.get(osyahuman_n).getTitle(), (float) -1.0, null, results.get(osyahuman_n).getImageId()));
                    }
                    results_food.add(new Classifier.Recognition(
                            "" + results.get(osyafood_n).getId(), results.get(osyafood_n).getTitle(), (float) -1.0, null, results.get(osyafood_n).getImageId()));
                }
                */

                if (results.get(osyafood_n).getConfidence() > results.get(dasafood_n).getConfidence()) {
                    results_food.add(results.get(osyafood_n));
                } else {
                    results_food.add(new Classifier.Recognition(
                            "" + results.get(osyafood_n).getId(), results.get(osyafood_n).getTitle(), (float) -1.0, null, results.get(osyafood_n).getImageId()));
                }
                if (results.get(osyahuman_n).getConfidence() > results.get(dasahuman_n).getConfidence()) {
                    results_human.add(results.get(osyahuman_n));
                } else {
                    results_human.add(new Classifier.Recognition(
                            "" + results.get(osyahuman_n).getId(), results.get(osyahuman_n).getTitle(), (float) -1.0, null, results.get(osyahuman_n).getImageId()));
                }

            }else {

                for (int i = 0; i < results.size(); i++) {
                    if (results.get(i).getTitle().equals("osyafood")) {
                        results_food.add(results.get(i));
                        results_food_d.add(results.get(i));
                    } else if (results.get(i).getTitle().equals("osyahuman")) {
                        results_human.add(results.get(i));
                        results_human_d.add(results.get(i));
                    } else {
                        Log.d("debug", "Unmatching labels");
                        // debug用
                        if (results.get(i).getTitle().equals("dasafood")) {
                            results_dasafood.add(results.get(i));
                        }
                        if (results.get(i).getTitle().equals("dasahuman")) {
                            results_dasahuman.add(results.get(i));
                        }
                        if (results.get(i).getTitle().equals("other")) {
                            results_other.add(results.get(i));
                        }
                    }
                }
            }

        }else {

            for (int i = 0; i < results.size(); i++) {
                if (results.get(i).getTitle().equals("instabae")) {
                    results_instabae.add(results.get(i));
                }else if(results.get(i).getTitle().equals("notinstabae")){
                    results_notinstabae.add(results.get(i));
                }
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