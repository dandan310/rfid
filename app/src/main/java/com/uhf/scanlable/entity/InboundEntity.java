package com.uhf.scanlable.entity;

import com.uhf.scanlable.ScanMode;

import java.util.List;

public class InboundEntity {
    private String barCode;
    private String hotelId;

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    private String hotelName;
    private Integer type;
    private List<ScanMode.InventoryTagMap> dataList;

    public String getHotelId() {
        return hotelId;
    }

    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public List<ScanMode.InventoryTagMap> getDataList() {
        return dataList;
    }

    public void setDataList(List<ScanMode.InventoryTagMap> dataList) {
        this.dataList = dataList;
    }

    public String getBarCode() {
        return barCode;
    }

    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }
}
