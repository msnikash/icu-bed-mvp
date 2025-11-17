package com.icu;

public class Patient {
    private int id;
    private String name;
    private int age;
    private int conditionPriority;
    private boolean bedAllocated;
    private Integer allocatedBedId;

    public Patient(int id, String name, int age, int conditionPriority, boolean bedAllocated, Integer allocatedBedId) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.conditionPriority = conditionPriority;
        this.bedAllocated = bedAllocated;
        this.allocatedBedId = allocatedBedId;
    }

    // constructor for new patients (no id yet)
    public Patient(String name, int age, int conditionPriority) {
        this(0, name, age, conditionPriority, false, null);
    }

    // getters / setters
    public int getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public int getConditionPriority() { return conditionPriority; }
    public boolean isBedAllocated() { return bedAllocated; }
    public Integer getAllocatedBedId() { return allocatedBedId; }

    public void setId(int id) { this.id = id; }
    public void setBedAllocated(boolean bedAllocated) { this.bedAllocated = bedAllocated; }
    public void setAllocatedBedId(Integer allocatedBedId) { this.allocatedBedId = allocatedBedId; }
}
