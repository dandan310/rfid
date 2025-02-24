package com.uhf.scanlable.entity;

import java.math.BigDecimal;

public class Rfid {
    private String epc;
    private Integer hotelId;
    private String hotelName;
    private Integer type;
    private String typeName;
    private Integer classification;
    private String classificationName;
    private BigDecimal size;
    private String remark;

    public String getEpc() {
        return epc;
    }

    public void setEpc(String epc) {
        this.epc = epc;
    }

    public Integer getHotelId() {
        return hotelId;
    }

    public void setHotelId(Integer hotelId) {
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
            default: return "";
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
                return "床单";
            case 1:
                return "被套";
            case 2:
                return "枕套";
            default: return "";
        }
    }


    public BigDecimal getSize() {
        return size;
    }

    public void setSize(BigDecimal size) {
        this.size = size;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
