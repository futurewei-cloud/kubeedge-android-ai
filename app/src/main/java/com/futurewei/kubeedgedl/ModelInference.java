package com.futurewei.kubeedgedl;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ModelInference {
    @POST("v1/models/model_emotion/versions/4:predict")
    Call<FrameResult> doModelInference(@Body FrameRequest req);
}
