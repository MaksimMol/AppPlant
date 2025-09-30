package com.example.plant_app.service;

import com.example.plant_app.dao.CSVPlantDAO;
import com.example.plant_app.dao.PlantDAO;
import com.example.plant_app.model.Plant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PlantService {

    private final List<PlantDAO> daos = new ArrayList<>(); // любой DAO можно добавить
    private final Map<String, CatalogEntry> catalogByName = new HashMap<>();
    private final Set<String> knownTypes = new HashSet<>();

    private static class PlantRule {
        int spring, summer, autumn, winter, repotWeeks;

        PlantRule(int spring, int summer, int autumn, int winter, int repotWeeks) {
            this.spring = spring;
            this.summer = summer;
            this.autumn = autumn;
            this.winter = winter;
            this.repotWeeks = repotWeeks;
        }
    }

    private static class CatalogEntry {
        final String name;
        final String type;
        final PlantRule rule;

        CatalogEntry(String name, String type, PlantRule rule) {
            this.name = name;
            this.type = type;
            this.rule = rule;
        }
    }

    public PlantService(PlantDAO... initialDaos) {
        if (initialDaos != null) daos.addAll(Arrays.asList(initialDaos));

        CSVPlantDAO csvDao = new CSVPlantDAO();
        daos.add(csvDao);

        loadCatalog();
    }

    public void addDAO(PlantDAO dao) {
        if (dao != null) daos.add(dao);
    }

    private void loadCatalog() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("plants_catalog.csv")) {
            if (is == null) {
                System.out.println("Не найден plants_catalog.csv в resources!");
                return;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                br.readLine(); // заголовок
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    String[] parts = line.split(",");
                    if (parts.length < 7) continue;

                    String name = parts[0].trim().toLowerCase();
                    String type = parts[1].trim().toLowerCase();
                    int spring = Integer.parseInt(parts[2].trim());
                    int summer = Integer.parseInt(parts[3].trim());
                    int autumn = Integer.parseInt(parts[4].trim());
                    int winter = Integer.parseInt(parts[5].trim());
                    int repot = Integer.parseInt(parts[6].trim());

                    PlantRule rule = new PlantRule(spring, summer, autumn, winter, repot);
                    catalogByName.put(name, new CatalogEntry(name, type, rule));
                    knownTypes.add(type);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String detectTypeByName(String name) {
        if (name == null) return null;
        CatalogEntry e = catalogByName.get(name.trim().toLowerCase());
        return (e == null) ? null : e.type;
    }

    public boolean isValidPlantName(String name) {
        if (name == null) return false;
        return catalogByName.containsKey(name.trim().toLowerCase());
    }

    public boolean isValidType(String type) {
        return type != null && knownTypes.contains(type.trim().toLowerCase());
    }

    public void addPlant(Plant plant) {
        if (plant == null || plant.getName() == null) return;

        String nameKey = plant.getName().trim().toLowerCase();
        if (!isValidPlantName(nameKey))
            throw new IllegalArgumentException("Растение '" + plant.getName() + "' не найдено в справочнике.");

        boolean exists = getAllPlants().stream().anyMatch(p -> p.getName().equalsIgnoreCase(plant.getName()));
        if (exists) throw new IllegalArgumentException("Растение с таким названием уже существует!");

        String correctType = detectTypeByName(nameKey);
        if (correctType != null) plant.setType(correctType);

        plant.setNextWatering(calculateNextWateringForNewPlant(plant));
        updateStatus(plant);

        daos.forEach(dao -> dao.addPlant(plant));
    }

    public void updatePlant(String oldName, Plant plant) {
        if (plant == null || plant.getName() == null) return;

        String nameKey = plant.getName().trim().toLowerCase();
        if (!isValidPlantName(nameKey))
            throw new IllegalArgumentException("Растение '" + plant.getName() + "' не найдено в справочнике.");

        String correctType = detectTypeByName(nameKey);
        if (correctType != null) plant.setType(correctType);

        updateStatus(plant);

        daos.forEach(dao -> dao.updatePlant(oldName, plant));
    }

    public void deletePlant(String name) {
        daos.forEach(dao -> dao.deletePlant(name));
    }

    public Plant getPlant(String name) {
        for (PlantDAO dao : daos) {
            Plant plant = dao.getPlant(name);
            if (plant != null) {
                updateStatus(plant);
                return plant;
            }
        }
        return null;
    }

    public List<Plant> getAllPlants() {
        Map<String, Plant> merged = new LinkedHashMap<>();
        for (PlantDAO dao : daos) {
            for (Plant p : dao.getAllPlants()) {
                merged.putIfAbsent(p.getName(), p);
            }
        }
        merged.values().forEach(this::updateStatus);
        return new ArrayList<>(merged.values());
    }

    public LocalDate calculateNextWateringForNewPlant(Plant plant) {
        if (plant == null) return null;
        LocalDate last = plant.getLastWatered() != null ? plant.getLastWatered() : LocalDate.now();
        CatalogEntry entry = catalogByName.get(plant.getName().trim().toLowerCase());
        PlantRule rule = entry != null ? entry.rule : new PlantRule(7, 7, 7, 14, 52);

        int days;
        switch (getCurrentSeason()) {
            case "весна": days = rule.spring; break;
            case "лето": days = rule.summer; break;
            case "осень": days = rule.autumn; break;
            default: days = rule.winter;
        }

        return last.plusDays(days);
    }

    public void calculateNextWatering(Plant plant) {
        if (plant == null) return;
        plant.setNextWatering(calculateNextWateringForNewPlant(plant));
        updateStatus(plant);
    }

    public void performWatering(Plant plant){
        if(plant==null) return;
        if(plant.getWateringHistory()==null) plant.setWateringHistory(new ArrayList<>());
        plant.getWateringHistory().add(LocalDate.now());
        plant.setLastWatered(LocalDate.now());
        calculateNextWatering(plant);
        updateStatus(plant);
        daos.forEach(dao->dao.updatePlant(plant.getName(),plant));
    }

    private void updateStatus(Plant plant) {
        if (plant == null || plant.getLastWatered() == null) return;
        long days = ChronoUnit.DAYS.between(plant.getLastWatered(), LocalDate.now());
        plant.setStatus(days > 30 ? "Неактивно" : "Активно");
    }

    private String getCurrentSeason() {
        int m = LocalDate.now().getMonthValue();
        if (m >= 3 && m <= 5) return "весна";
        if (m >= 6 && m <= 8) return "лето";
        if (m >= 9 && m <= 11) return "осень";
        return "зима";
    }

    public LocalDate getNextRepotDate(Plant plant) {
        if (plant == null) return null;
        CatalogEntry entry = catalogByName.get(plant.getName().trim().toLowerCase());
        PlantRule rule = entry != null ? entry.rule : new PlantRule(7, 7, 7, 14, 52);
        return plant.getLastWatered().plusWeeks(rule.repotWeeks);
    }

    public String diagnoseDisease(Plant plant) {
        List<String> symptoms = plant.getSymptoms();
        if (symptoms == null || symptoms.isEmpty() ||
                (symptoms.size() == 1 && (symptoms.get(0).trim().isEmpty() ||
                        symptoms.get(0).equalsIgnoreCase("Признаков заболеваний не выявлено"))))
            return "Признаков заболеваний не выявлено";

        Map<String, String> symptomDictionary = new LinkedHashMap<>();
        symptomDictionary.put("желтые листья", "Недостаток азота или старение листьев");
        symptomDictionary.put("жёлтые листья", "Недостаток азота или старение листьев");
        symptomDictionary.put("коричневые кончики", "Недостаток влаги или сухой воздух");
        symptomDictionary.put("сухие листья", "Недостаток влаги или слишком жаркий воздух");
        symptomDictionary.put("вялые листья", "Недостаток воды");
        symptomDictionary.put("белый налёт", "Мучнистая роса (грибковое заболевание)");
        symptomDictionary.put("черные пятна", "Чёрная пятнистость (грибковое заболевание)");
        symptomDictionary.put("опадают листья", "Стресс: перемена условий, сквозняки, недостаток света");
        symptomDictionary.put("паутинка", "Паутинный клещ");
        symptomDictionary.put("липкие листья", "Тля или щитовка");
        symptomDictionary.put("дыры на листьях", "Насекомые-вредители");
        symptomDictionary.put("гниль", "Корневая гниль из-за переувлажнения");
        symptomDictionary.put("бутоны осыпаются", "Недостаток света или питания");
        symptomDictionary.put("бледные жилки", "Хлороз: нехватка железа или магния");

        Set<String> detectedIssues = new LinkedHashSet<>();
        for (String s : symptoms) {
            String symptom = s.trim().toLowerCase();
            boolean matched = false;
            for (Map.Entry<String, String> entry : symptomDictionary.entrySet()) {
                if (symptom.contains(entry.getKey())) {
                    detectedIssues.add(entry.getValue());
                    matched = true;
                }
            }
            if (!matched && !symptom.isEmpty() &&
                    !symptom.equalsIgnoreCase("Признаков заболеваний не выявлено")) {
                detectedIssues.add("Неопределённый симптом: \"" + s.trim() + "\"");
            }
        }

        return detectedIssues.isEmpty() ? "Признаков заболеваний не выявлено"
                : String.join("; ", detectedIssues);
    }
}
