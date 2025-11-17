package com.icu;


public class Bed {
    private int id;
    private String bedNumber;
    private boolean occupied;

    public Bed(int id, String bedNumber, boolean occupied) {
        this.id = id;
        this.bedNumber = bedNumber;
        this.occupied = occupied;
    }

    public int getId() { return id; }
    public String getBedNumber() { return bedNumber; }
    public boolean isOccupied() { return occupied; }
}
