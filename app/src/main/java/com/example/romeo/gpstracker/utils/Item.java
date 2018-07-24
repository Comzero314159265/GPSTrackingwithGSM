package com.example.romeo.gpstracker.utils;

public class Item {
    String uid;
    String name;


    public Item() {
    }

    public Item(String uid , String name) {
        this.name = name;
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override
    public boolean equals(Object obj) {
        Item b = (Item)obj;
        return b.getName().equals(this.name) || b.getUid().equals(this.uid)  ;
    }
}
