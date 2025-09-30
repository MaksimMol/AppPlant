package com.example.plant_app.dao;


import com.example.plant_app.model.Plant;
import java.util.List;

public interface PlantDAO {
    void addPlant(Plant plant);
    void updatePlant(String oldName, Plant plant); // <-- изменили сигнатуру
    void deletePlant(String plantName);
    Plant getPlant(String plantName);
    List<Plant> getAllPlants();
}