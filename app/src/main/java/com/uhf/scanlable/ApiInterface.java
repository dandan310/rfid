package com.uhf.scanlable;

import com.uhf.scanlable.entity.Hotel;
import com.uhf.scanlable.entity.Rfid;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiInterface {
    @GET("/epcInfo?apipost_id=1e230abb9c002")  // 替换成你的接口 URL
    Call<List<Rfid>> getRfidList();

    @GET("https://your-api-url.com/")  // 替换成你的接口 URL
    Call<List<Hotel>> getHotelList();

}
