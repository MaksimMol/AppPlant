package com.example.plant_app.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PlantRules {

    public static class Rule {
        public int summerWater;
        public int winterWater;
        public int repotWeeks;

        public Rule(int summerWater, int winterWater, int repotWeeks) {
            this.summerWater = summerWater;
            this.winterWater = winterWater;
            this.repotWeeks = repotWeeks;
        }
    }

    private final Map<String, Rule> rules = new HashMap<>();

    public PlantRules() {
        loadRules();
    }

    private void loadRules() {
        try (BufferedReader br = new BufferedReader(new FileReader("src/main/resources/plant_rules.csv"))) {
            String line = br.readLine(); // пропускаем заголовок
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String type = parts[0].toLowerCase();
                    int summerWater = Integer.parseInt(parts[1]);
                    int winterWater = Integer.parseInt(parts[2]);
                    int repotWeeks = Integer.parseInt(parts[3]);
                    rules.put(type, new Rule(summerWater, winterWater, repotWeeks));
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка при чтении plant_rules.csv");
            e.printStackTrace();
        }
    }

    public Rule getRule(String type) {
        return rules.getOrDefault(type.toLowerCase(), new Rule(5,10,52)); // дефолтное правило
    }
}
