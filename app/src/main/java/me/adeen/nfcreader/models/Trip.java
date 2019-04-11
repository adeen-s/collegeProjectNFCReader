package me.adeen.nfcreader.models;

public class Trip {
    String from, to, date, timein, timeout;
    int cost;

    public Trip(String from, String to, String date, String timein, String timeout) {
        this.from = from;
        this.to = to;
        this.date = date;
        this.timein = timein;
        this.timeout = timeout;
    }

    public Trip(String from, String to, String date, String timein, String timeout, int cost) {
        this.from = from;
        this.to = to;
        this.date = date;
        this.timein = timein;
        this.timeout = timeout;
        this.cost = cost;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public Trip() {
    }

    public Trip(String from, String date, String timein) {
        this.from = from;
        this.date = date;
        this.timein = timein;
        this.to = "NULL";
        this.timeout = "NULL";
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTimein() {
        return timein;
    }

    public void setTimein(String timein) {
        this.timein = timein;
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }
}
