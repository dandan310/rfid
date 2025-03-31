package com.uhf.scanlable;

import com.uhf.scanlable.entity.BarCode;
import com.uhf.scanlable.entity.Hotel;
import com.uhf.scanlable.entity.InboundEntity;
import com.uhf.scanlable.entity.Res;
import com.uhf.scanlable.entity.Resp;
import com.uhf.scanlable.entity.Rfid;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiInterface {
    @GET("/api/baseData/linenManage/rfidList")  // 替换成你的接口 URL
    Call<Res<Rfid>> getRfidList();

    @POST("/api/orderCenter/linenStore/inBound")  // 替换成你的接口 URL
    Call<Resp<BarCode>> inBound(@Body InboundEntity inboundEntity);

    @GET("/api/baseData/customerManage/hotelList")  // 替换成你的接口 URL
    Call<Res<Hotel>> getHotelList();

}
