package com.example.romeo.gpstracker.utils;

public class Location {
    String key;
    String name;
    double lat;
    double lng;
    String timestamp;

    public Location() {

    }

    public Location(String name) {
        this.name = name;
    }

    public Location(String key, String name, double lat, double lng) {
        this.key = key;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    public Location(double lat, double lng, String timestamp) {
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }

    public Location(String key, String name, double lat, double lng, String timestamp) {
        this.key = key;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }

    public Location(String key, double lat, double lng, String timestamp) {
        this.key = key;
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this)
            return true;
        Location location = (Location)obj;
        return location.getKey().equals(this.key) || location.getName().equals(this.name);
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }
}

