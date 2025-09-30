package com.example.plant_app.controller;

import com.example.plant_app.model.Plant;
import com.example.plant_app.service.PlantService;

import java.util.List;

public class PlantController {
    private final PlantService plantService;

    public PlantController(PlantService plantService) {
        this.plantService = plantService;
    }

    public PlantService getPlantService() {
        return plantService;
    }

    public void performWatering(Plant plant) {
        if (plant != null) {
            plant.setLastWatered(plant.getNextWatering());
            plantService.calculateNextWatering(plant);
            plantService.updatePlant(plant.getName(), plant);
        }
    }

    public void autoWaterPlant(Plant plant) {
        plantService.calculateNextWatering(plant);
    }

    public void addPlant(Plant plant) {
        plantService.addPlant(plant);
    }

    public void updatePlant(String oldName, Plant plant) {
        plantService.updatePlant(oldName, plant);
    }

    public void deletePlant(String name) {
        plantService.deletePlant(name);
    }

    public List<Plant> getAllPlants() {
        return plantService.getAllPlants();
    }
}
