package com.uhf.scanlable;

import com.uhf.scanlable.entity.InboundEntity;
import com.uhf.scanlable.entity.Resp;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiInterface {

    @POST("/retrieval")  // 替换成你的接口 URL
    Call<Resp> retrieval(@Body InboundEntity inboundEntity);


}
