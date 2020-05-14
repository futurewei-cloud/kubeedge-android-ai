package com.futurewei.kubeedgedl;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.opencv.core.Mat;

public class FrameRequest {

    public static final int MODEL_INPUT_HEIGHT = 48;
    public static final int MODEL_INPUT_WIDTH = 48;

    public FrameRequest(Mat img) {
        this.signatureName = "serving_default";
        this.instances = new double[1][MODEL_INPUT_HEIGHT][MODEL_INPUT_WIDTH][3];

        for(int i=0; i < MODEL_INPUT_HEIGHT; ++i) {
            for(int j=0; j < MODEL_INPUT_WIDTH; ++j) {
                for(int k=0; k < 3; ++k)
                this.instances[0][i][j][k] = img.get(i, j)[k] / 255.0;
            }
        }
    }

    @SerializedName("signature_name")
    @Expose
    private String signatureName;

    @SerializedName("instances")
    @Expose
    private double[][][][] instances = null;

    public String getSignatureName() {
        return signatureName;
    }

    public void setSignatureName(String signatureName) {
        this.signatureName = signatureName;
    }

    public double[][][][] getInstances() {
        return instances;
    }

    public void setInstances(double[][][][] instances) {
        this.instances = instances;
    }

}