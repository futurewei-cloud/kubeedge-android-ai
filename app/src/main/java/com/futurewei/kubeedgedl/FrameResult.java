package com.futurewei.kubeedgedl;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class FrameResult {

    enum Emotion {
        Angry,
        Disgust,
        Fear,
        Happy,
        Sad,
        Surprise,
        Neutral
    }

    @SerializedName("predictions")
    @Expose
    private List<List<Double>> predictions = null;

    public List<List<Double>> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<List<Double>> predictions) {
        this.predictions = predictions;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}