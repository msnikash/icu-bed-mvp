package com.icu;

import java.time.LocalDate;

public class AllocatedEntry {
    private final int patientId;
    private final String name;
    private final Integer age;
    private final String priority;
    private final Integer days;
    private final int bedId;
    private final String bedNumber;
    private final LocalDate allocatedOn;
    private final LocalDate endDate;

    public AllocatedEntry(int patientId, String name, Integer age, String priority, Integer days, int bedId, String bedNumber, LocalDate allocatedOn, LocalDate endDate) {
        this.patientId = patientId;
        this.name = name;
        this.age = age;
        this.priority = priority;
        this.days = days;
        this.bedId = bedId;
        this.bedNumber = bedNumber;
        this.allocatedOn = allocatedOn;
        this.endDate = endDate;
    }

    public int getPatientId() { return patientId; }
    public String getName() { return name; }
    public Integer getAge() { return age; }
    public String getPriority() { return priority; }
    public Integer getDays() { return days; }
    public int getBedId() { return bedId; }
    public String getBedNumber() { return bedNumber; }
    public LocalDate getAllocatedOn() { return allocatedOn; }
    public LocalDate getEndDate() { return endDate; }
}
