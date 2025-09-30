package com.example.plant_app.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Plant {

    private String name;
    private String type;
    private LocalDate lastWatered;
    private LocalDate nextWatering;
    private double growthRate;
    private List<String> symptoms;
    private String status;
    private List<LocalDate> wateringHistory = new ArrayList<>();

    public Plant() {}

    public Plant(String name, String type, LocalDate lastWatered, double growthRate, List<String> symptoms, String status) {
        this(name, type, lastWatered, growthRate, symptoms, status, null);
    }

    public Plant(String name, String type, LocalDate lastWatered, double growthRate, List<String> symptoms, String status, LocalDate nextWatering) {
        this.name = name;
        this.type = type;
        this.lastWatered = lastWatered;
        this.growthRate = growthRate;
        this.symptoms = symptoms;
        this.status = status;
        this.nextWatering = nextWatering;
    }

    public String getName() { return name; }

    public String getType() { return type; }

    public LocalDate getLastWatered() { return lastWatered; }

    public LocalDate getNextWatering() { return nextWatering; }

    public double getGrowthRate() { return growthRate; }

    public List<String> getSymptoms() { return symptoms; }

    public String getStatus() { return status; }

    public List<LocalDate> getWateringHistory() {
        if (wateringHistory == null) wateringHistory = new ArrayList<>();
        return wateringHistory;
    }

    public void setName(String name) { this.name = name; }

    public void setType(String type) { this.type = type; }

    public void setLastWatered(LocalDate lastWatered) {
        if (this.lastWatered != null && !this.lastWatered.equals(lastWatered)) {
            getWateringHistory().add(this.lastWatered);
        }
        this.lastWatered = lastWatered;
    }

    public void setWateringHistory(List<LocalDate> wateringHistory) {
        this.wateringHistory = wateringHistory;
    }

    public void setNextWatering(LocalDate nextWatering) { this.nextWatering = nextWatering; }

    public void setGrowthRate(double growthRate) { this.growthRate = growthRate; }

    public void setSymptoms(List<String> symptoms) { this.symptoms = symptoms; }

    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Plant{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", lastWatered=" + lastWatered +
                ", nextWatering=" + nextWatering +
                ", growthRate=" + growthRate +
                ", symptoms=" + symptoms +
                ", status='" + status + '\'' +
                ", wateringHistory=" + wateringHistory +
                '}';
    }
}
