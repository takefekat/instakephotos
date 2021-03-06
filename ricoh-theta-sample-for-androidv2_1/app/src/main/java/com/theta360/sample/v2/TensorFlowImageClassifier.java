/* Copyright 2016 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.theta360.sample.v2;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

/** A classifier specialized to label images using TensorFlow. */
public class TensorFlowImageClassifier implements Classifier {
  private static final String TAG = "TensorFlowImageClassifier";

  // Only return this many results with at least this confidence.
  //private static final int MAX_RESULTS = 3;
  //private static final float THRESHOLD = 0.1f;

  // Config values.
  private String inputName;
  private String outputName;
  private int inputSize;
  private int imageMean;
  private float imageStd;

  // Pre-allocated buffers.
  private Vector<String> labels = new Vector<String>();
  private int[] intValues;
  private float[] floatValues;
  private float[] outputs;
  private String[] outputNames;

  private boolean logStats = false;

  private TensorFlowInferenceInterface inferenceInterface;

  private TensorFlowImageClassifier() {}

  /**
   * Initializes a native TensorFlow session for classifying images.
   *
   * @param assetManager The asset manager to be used to load assets.
   * @param modelFilename The filepath of the model GraphDef protocol buffer.
   * @param labelFilename The filepath of label file for classes.
   * @param inputSize The input size. A square image of inputSize x inputSize is assumed.
   * @param imageMean The assumed mean of the image values.
   * @param imageStd The assumed std of the image values.
   * @param inputName The label of the image input node.
   * @param outputName The label of the output node.
   * @throws IOException
   */
  public static Classifier create(
      AssetManager assetManager,
      String modelFilename,
      String labelFilename,
      int inputSize,
      int imageMean,
      float imageStd,
      String inputName,
      String outputName) {
    TensorFlowImageClassifier c = new TensorFlowImageClassifier();
    c.inputName = inputName;
    c.outputName = outputName;

    // Read the label names into memory.
    String actualFilename = labelFilename.split("file:///android_asset/")[1];
    //Log.i(TAG, "Reading labels from: " + actualFilename);
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(assetManager.open(actualFilename)));
      String line;
      while ((line = br.readLine()) != null) {
        c.labels.add(line);
      }
      br.close();
    } catch (IOException e) {
      throw new RuntimeException("Problem reading label file!" , e);
    }

    c.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);

    // The shape of the output is [N, NUM_CLASSES], where N is the batch size.
    final Operation operation = c.inferenceInterface.graphOperation(outputName);
    final int numClasses = (int) operation.output(0).shape().size(1);
    //Log.i(TAG, "Read " + c.labels.size() + " labels, output layer size is " + numClasses);

    // Ideally, inputSize could have been retrieved from the shape of the input operation.  Alas,
    // the placeholder node for input in the graphdef typically used does not specify a shape, so it
    // must be passed in as a parameter.
    c.inputSize = inputSize;
    c.imageMean = imageMean;
    c.imageStd = imageStd;

    // Pre-allocate buffers.
    c.outputNames = new String[] {outputName};
    c.intValues = new int[inputSize * inputSize];
    c.floatValues = new float[inputSize * inputSize * 3];
    c.outputs = new float[numClasses];

    return c;
  }

  @Override
  public List<Recognition> recognizeImage(final Bitmap bitmap, final String imageId) {
    // Log this method so that it can be analyzed with systrace.
    //Trace.beginSection("recognizeImage");

    //Trace.beginSection("preprocessBitmap");
    // Preprocess the image data from 0-255 int to normalized float based
    // on the provided parameters.
    bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    for (int i = 0; i < intValues.length; ++i) {
      final int val = intValues[i];
      floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) - imageMean) / imageStd;
      floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - imageMean) / imageStd;
      floatValues[i * 3 + 2] = ((val & 0xFF) - imageMean) / imageStd;
    }
    //Trace.endSection();

    // Copy the input data into TensorFlow.
    //Trace.beginSection("feed");
    inferenceInterface.feed(inputName, floatValues, 1, inputSize, inputSize, 3);
    //Trace.endSection();

    // Run the inference call.
    //Trace.beginSection("run");
    inferenceInterface.run(outputNames, logStats);
    //Trace.endSection();

    // Copy the output Tensor back into the output array.
    //Trace.beginSection("fetch");
    inferenceInterface.fetch(outputName, outputs);
    //Trace.endSection();

    //outputsにinstabae,notinstabaeの値が入っている
      ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
      /*
      int osyafood_n = -1;
      int dasafood_n = -1;
      int osyahuman_n = -1;
      int dasahuman_n = -1;
      for (int i = 0; i < outputs.length; ++i) {
        if (labels.get(i).equals("osyafood")) {
          osyafood_n = i;
        } else if (labels.get(i).equals("osyahuman")) {
          osyahuman_n = i;
        } else if (labels.get(i).equals("dasafood")) {
          dasafood_n = i;
        } else if (labels.get(i).equals("dasahuman")) {
          dasahuman_n = i;
        }
      }

      if(outputs[osyafood_n] > outputs[osyahuman_n]){
          if(outputs[osyafood_n] > outputs[dasafood_n]){
              recognitions.add(new Recognition(
                      "" + osyafood_n, labels.get(osyafood_n), outputs[osyafood_n], null, imageId));
          }else{
              recognitions.add(new Recognition(
                      "" + osyafood_n, labels.get(osyafood_n), (float) -1.0, null, imageId));
          }
          recognitions.add(new Recognition(
                  "" + osyahuman_n, labels.get(osyahuman_n), (float) -1.0, null, imageId));
      }else{
          if(outputs[osyahuman_n] > outputs[dasahuman_n]){
              recognitions.add(new Recognition(
                      "" + osyahuman_n, labels.get(osyahuman_n), outputs[osyahuman_n], null, imageId));
          }else{
              recognitions.add(new Recognition(
                      "" + osyahuman_n, labels.get(osyahuman_n), (float) -1.0, null, imageId));
          }
          recognitions.add(new Recognition(
                  "" + osyafood_n, labels.get(osyafood_n), (float) -1.0, null, imageId));
      }
      */
      for (int i = 0; i < outputs.length; ++i) {
          if (labels.get(i).equals("osyafood")||labels.get(i).equals("osyahuman")||
                  labels.get(i).equals("dasafood")|| labels.get(i).equals("dasahuman")||
                  labels.get(i).equals("other")) {
              recognitions.add(new Recognition(
                      "" + i, labels.get(i), outputs[i], null, imageId));
          }
      }


      return recognitions;
  }

  @Override
  public void enableStatLogging(boolean logStats) {
    this.logStats = logStats;
  }

  @Override
  public String getStatString() {
    return inferenceInterface.getStatString();
  }

  @Override
  public void close() {
    inferenceInterface.close();
  }
}


