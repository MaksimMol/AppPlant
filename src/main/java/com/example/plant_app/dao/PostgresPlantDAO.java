package com.example.plant_app.dao;

import com.example.plant_app.model.Plant;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostgresPlantDAO implements PlantDAO {

    private final Connection connection;

    public PostgresPlantDAO(String url, String user, String password) throws SQLException {
        this.connection = DriverManager.getConnection(url, user, password);
        createTableIfNotExists();
    }

    private void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS plants (" +
                "name TEXT PRIMARY KEY," +
                "type TEXT," +
                "last_watered DATE," +
                "next_watering DATE," +
                "growth_rate DOUBLE PRECISION," +
                "status TEXT," +
                "symptoms TEXT" + // можно хранить как JSON или CSV
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    public void addPlant(Plant plant) {
        String sql = "INSERT INTO plants (name, type, last_watered, next_watering, growth_rate, status, symptoms) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, plant.getName());
            ps.setString(2, plant.getType());
            ps.setDate(3, plant.getLastWatered() != null ? Date.valueOf(plant.getLastWatered()) : null);
            ps.setDate(4, plant.getNextWatering() != null ? Date.valueOf(plant.getNextWatering()) : null);
            ps.setDouble(5, plant.getGrowthRate());
            ps.setString(6, plant.getStatus());
            ps.setString(7, String.join(",", plant.getSymptoms()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updatePlant(String oldName, Plant plant) {
        String sql = "UPDATE plants SET name=?, type=?, last_watered=?, next_watering=?, growth_rate=?, status=?, symptoms=? " +
                "WHERE name=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, plant.getName());
            ps.setString(2, plant.getType());
            ps.setDate(3, plant.getLastWatered() != null ? Date.valueOf(plant.getLastWatered()) : null);
            ps.setDate(4, plant.getNextWatering() != null ? Date.valueOf(plant.getNextWatering()) : null);
            ps.setDouble(5, plant.getGrowthRate());
            ps.setString(6, plant.getStatus());
            ps.setString(7, String.join(",", plant.getSymptoms()));
            ps.setString(8, oldName);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deletePlant(String name) {
        String sql = "DELETE FROM plants WHERE name=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Plant getPlant(String name) {
        String sql = "SELECT * FROM plants WHERE name=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Plant plant = new Plant();
                plant.setName(rs.getString("name"));
                plant.setType(rs.getString("type"));
                plant.setLastWatered(rs.getDate("last_watered") != null ? rs.getDate("last_watered").toLocalDate() : null);
                plant.setNextWatering(rs.getDate("next_watering") != null ? rs.getDate("next_watering").toLocalDate() : null);
                plant.setGrowthRate(rs.getDouble("growth_rate"));
                plant.setStatus(rs.getString("status"));
                String symptoms = rs.getString("symptoms");
                plant.setSymptoms(symptoms != null ? List.of(symptoms.split(",")) : new ArrayList<>());
                return plant;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Plant> getAllPlants() {
        List<Plant> plants = new ArrayList<>();
        String sql = "SELECT * FROM plants";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Plant plant = new Plant();
                plant.setName(rs.getString("name"));
                plant.setType(rs.getString("type"));
                plant.setLastWatered(rs.getDate("last_watered") != null ? rs.getDate("last_watered").toLocalDate() : null);
                plant.setNextWatering(rs.getDate("next_watering") != null ? rs.getDate("next_watering").toLocalDate() : null);
                plant.setGrowthRate(rs.getDouble("growth_rate"));
                plant.setStatus(rs.getString("status"));
                String symptoms = rs.getString("symptoms");
                plant.setSymptoms(symptoms != null ? List.of(symptoms.split(",")) : new ArrayList<>());
                plants.add(plant);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return plants;
    }
}
