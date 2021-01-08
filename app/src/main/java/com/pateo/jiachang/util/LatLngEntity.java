package com.pateo.jiachang.util;


import com.amap.api.services.core.LatLonPoint;

public class LatLngEntity {

    private final double longitude; // 经度
    private final double latitude;  // 纬度

    /**
     * @param value e.g "113.4114889842685,23.172753522587282"
     */
    public LatLngEntity(String value) {
        this.longitude = Double.parseDouble(value.split(",")[0]);
        this.latitude = Double.parseDouble(value.split(",")[1]);
    }

    public LatLngEntity(double latitude, double longitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public LatLngEntity(LatLonPoint latLonPoint) {
        this.longitude = latLonPoint.getLongitude();
        this.latitude = latLonPoint.getLatitude();
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
