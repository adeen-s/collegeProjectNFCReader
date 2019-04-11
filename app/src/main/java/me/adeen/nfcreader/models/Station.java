package me.adeen.nfcreader.models;

public class Station {
    String name;
    int key;

    public Station() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public Station(String name, int key) {
        this.name = name;
        this.key = key;
    }
}
