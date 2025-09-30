package com.example.plant_app.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.plant_app.model.Plant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JSONPlantDAO implements PlantDAO {

    private final File file;
    private final ObjectMapper mapper;

    public JSONPlantDAO() {
        this.file = new File("data/plants.json");
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules(); // чтобы Jackson понимал LocalDate
        ensureFileExists();
    }

    private void ensureFileExists() {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
                saveAll(new ArrayList<>()); // создаём пустой список
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать JSON файл: " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public void addPlant(Plant plant) {
        List<Plant> plants = getAllPlants();
        plants.add(plant);
        saveAll(plants);
    }

    @Override
    public void updatePlant(String oldName, Plant plant) {
        List<Plant> plants = getAllPlants();
        boolean updated = false;
        for (int i = 0; i < plants.size(); i++) {
            if (plants.get(i).getName().equals(oldName)) {
                plants.set(i, plant);
                updated = true;
                break;
            }
        }
        if (!updated) {
            plants.add(plant);
        }
        saveAll(plants);
    }

    @Override
    public void deletePlant(String plantName) {
        List<Plant> plants = getAllPlants();
        plants.removeIf(p -> p.getName().equals(plantName));
        saveAll(plants);
    }

    @Override
    public Plant getPlant(String plantName) {
        return getAllPlants().stream()
                .filter(p -> p.getName().equals(plantName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Plant> getAllPlants() {
        try {
            if (file.length() == 0) {
                return new ArrayList<>();
            }
            return mapper.readValue(file, new TypeReference<List<Plant>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void saveAll(List<Plant> plants) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, plants);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
