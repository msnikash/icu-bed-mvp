package com.icu;

import java.time.LocalDate;

public class Patient {
    private int id;
    private String name;
    private int age;
    private String conditionPriority;
    private boolean bedAllocated;
    private Integer allocatedBedId;
    private Integer allocatedDays;
    private LocalDate startDate;
    private LocalDate endDate;

    public Patient(int id, String name, int age, String conditionPriority, boolean bedAllocated, Integer allocatedBedId, Integer allocatedDays, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.conditionPriority = conditionPriority;
        this.bedAllocated = bedAllocated;
        this.allocatedBedId = allocatedBedId;
        this.allocatedDays = allocatedDays;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // constructor for new patients (no id yet)
    public Patient(String name, int age, String conditionPriority, int allocatedDays, LocalDate startDate, LocalDate endDate) {
        this(0, name, age, conditionPriority, false, null, allocatedDays, startDate, endDate);
    }

    // getters / setters
    public int getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getConditionPriority() { return conditionPriority; }
    public boolean isBedAllocated() { return bedAllocated; }
    public Integer getAllocatedBedId() { return allocatedBedId; }
    public Integer getAllocatedDays() { return allocatedDays; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }

    public void setId(int id) { this.id = id; }
    public void setBedAllocated(boolean bedAllocated) { this.bedAllocated = bedAllocated; }
    public void setAllocatedBedId(Integer allocatedBedId) { this.allocatedBedId = allocatedBedId; }
    public void setConditionPriority(String conditionPriority) { this.conditionPriority = conditionPriority; }
    public void setAllocatedDays(Integer allocatedDays) { this.allocatedDays = allocatedDays; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
