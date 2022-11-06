package com.spreadsheet;

public class Formula {
    public String id;
    public String value;
    Formula(String id, String value) {
        this.id = id;
        this.value =  value;
    }
    public String toString() {
        return id + " " + value;
    }
}
