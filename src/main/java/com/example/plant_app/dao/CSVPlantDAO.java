package com.example.plant_app.dao;

import com.example.plant_app.model.Plant;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSVPlantDAO implements PlantDAO {

    private final Path filePath;

    public CSVPlantDAO() {
        this.filePath = Paths.get("data", "plants.csv");
        ensureFileExists();
    }

    private void ensureFileExists() {
        try {
            if (!Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Не удалось создать файл CSV: " + filePath);
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
        List<Plant> plants = new ArrayList<>();
        if (!Files.exists(filePath)) return plants;

        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 7) {
                    String name = parts[0];
                    String type = parts[1];
                    LocalDate lastWatered = parts[2].isEmpty() ? null : LocalDate.parse(parts[2]);
                    double growthRate = Double.parseDouble(parts[3]);
                    List<String> symptoms = parts[4].isEmpty() ? new ArrayList<>() : Arrays.asList(parts[4].split(";"));
                    String status = parts[5];
                    LocalDate nextWatering = parts[6].isEmpty() ? null : LocalDate.parse(parts[6]);

                    Plant plant = new Plant(name, type, lastWatered, growthRate, symptoms, status, nextWatering);

                    // История полива (если есть 8-й столбец)
                    if (parts.length >= 8 && !parts[7].isEmpty()) {
                        List<LocalDate> history = new ArrayList<>();
                        for (String d : parts[7].split(";")) {
                            history.add(LocalDate.parse(d));
                        }
                        plant.setWateringHistory(history);
                    }

                    plants.add(plant);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return plants;
    }

    private void saveAll(List<Plant> plants) {
        try (BufferedWriter bw = Files.newBufferedWriter(filePath)) {
            for (Plant p : plants) {
                String historyStr = p.getWateringHistory() != null ?
                        String.join(";", p.getWateringHistory().stream().map(LocalDate::toString).toList())
                        : "";
                String line = String.join(",",
                        p.getName(),
                        p.getType(),
                        p.getLastWatered() != null ? p.getLastWatered().toString() : "",
                        String.valueOf(p.getGrowthRate()),
                        p.getSymptoms() != null ? String.join(";", p.getSymptoms()) : "",
                        p.getStatus() != null ? p.getStatus() : "",
                        p.getNextWatering() != null ? p.getNextWatering().toString() : "",
                        historyStr
                );
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}