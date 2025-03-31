package com.uhf.scanlable.entity;

import com.google.gson.annotations.SerializedName;
import com.uhf.scanlable.ScanMode;

import java.math.BigDecimal;

public class Rfid {
    @SerializedName("rfid")
    private String epc;
    @SerializedName("cus_id")
    private String hotelId;
    private String hotelName;
    private Integer type;
    private String typeName;
    private Integer classification;
    private String classificationName;
    private String size;
    private String remark;

    public Rfid() {
    }
    public Rfid(ScanMode.InventoryTagMap map){
        this.epc = map.strEPC;
        this.type = map.type;
        this.hotelId = map.hotelId;
        this.hotelName = map.hotel;
        this.classification = map.classification;
        this.remark = map.remark;
    }


    public String getEpc() {
        return epc;
    }

    public void setEpc(String epc) {
        this.epc = epc;
    }

    public String getHotelId() {
        return hotelId;
    }

    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getTypeName() {
        switch (this.type){
            case 0:
                return "传统";
            case 1:
                return "共享";
            default: return null;
        }
    }


    public Integer getClassification() {
        return classification;
    }

    public void setClassification(Integer classification) {
        this.classification = classification;
    }

    public String getClassificationName() {
        switch (this.classification){
            case 0:
                return "1米2床单";
            case 1:
                return "1米5床单";
            case 2:
                return "1米8床单";
            case 3:
                return "1米2被套";
            case 4:
                return "1米5被套";
            case 5:
                return "1米8被套";
            case 6:
                return "浴巾";
            case 7:
                return "面巾";
            default:
                return "";
        }
    }


    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
