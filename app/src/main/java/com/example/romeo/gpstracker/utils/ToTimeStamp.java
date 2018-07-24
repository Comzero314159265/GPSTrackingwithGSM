package com.example.romeo.gpstracker.utils;

public class ToTimeStamp {
    public static String parse(String temp){
        String timestamp = "";
        timestamp += temp.substring(0,4);
        timestamp += "/";
        timestamp += temp.substring(4,6);
        timestamp += "/";
        timestamp += temp.substring(6,8);
        timestamp += " ";
        timestamp += temp.substring(8,10);
        timestamp += ":";
        timestamp += temp.substring(10,12);
        timestamp += ":";
        timestamp += temp.substring(12);
        return timestamp;
    }
}
