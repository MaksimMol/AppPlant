package com.example.plant_app.controller;

import com.example.plant_app.dao.CSVPlantDAO;
import com.example.plant_app.dao.PlantDAO;
import com.example.plant_app.dao.PostgresPlantDAO;
import com.example.plant_app.dao.JSONPlantDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Duration;
import com.example.plant_app.model.Plant;
import com.example.plant_app.service.PlantService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class HelloController {

    @FXML private TableView<Plant> tableView;
    @FXML private TableColumn<Plant, String> nameCol;
    @FXML private TableColumn<Plant, String> typeCol;
    @FXML private TableColumn<Plant, String> lastWateredCol;
    @FXML private TableColumn<Plant, String> nextWateringCol;
    @FXML private TableColumn<Plant, String> statusCol;
    @FXML private TableColumn<Plant, String> nextRepotCol;
    @FXML private TableColumn<Plant, String> diagnosisCol;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> dataSourceComboBox;

    private PlantDAO csvDao;
    private PlantDAO postgresDao;

    private ObservableList<Plant> csvPlants;
    private ObservableList<Plant> postgresPlants;
    private ObservableList<Plant> plantData;         // текущий список для TableView
    private ObservableList<Plant> masterPlantData;   // полный список текущего источника (для фильтров)

    private PlantDAO jsonDao;
    private ObservableList<Plant> jsonPlants;
    private PlantService jsonService;
    private PlantController jsonController;

    private PlantService service;
    private PlantController controller;

    private PlantService csvService;
    private PlantService postgresService;
    private PlantController csvController;
    private PlantController postgresController;

    private String currentFilter = "Все";

    @FXML
    public void initialize() {
        // Инициализация DAO и контроллеров
        csvDao = new CSVPlantDAO();
        csvService = new PlantService(csvDao);
        csvController = new PlantController(csvService);
        csvPlants = FXCollections.observableArrayList(csvDao.getAllPlants());

        try {
            postgresDao = new PostgresPlantDAO("jdbc:postgresql://localhost:5432/plantdb", "postgres", "12345");
            postgresService = new PlantService(postgresDao);
            postgresController = new PlantController(postgresService);
            postgresPlants = FXCollections.observableArrayList(postgresDao.getAllPlants());
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Ошибка подключения", "Не удалось подключиться к PostgreSQL: " + e.getMessage());
            postgresPlants = FXCollections.observableArrayList();
        }

        // JSON
        jsonDao = new JSONPlantDAO();
        jsonService = new PlantService(jsonDao);
        jsonController = new PlantController(jsonService);
        jsonPlants = FXCollections.observableArrayList(jsonDao.getAllPlants());

        // Источник данных
        dataSourceComboBox.getItems().addAll("CSV", "PostgreSQL", "JSON"); // 👈 добавили JSON
        dataSourceComboBox.setValue("CSV");
        dataSourceComboBox.setOnAction(e -> switchDataSource());

        // Изначально CSV
        plantData = csvPlants;
        masterPlantData = FXCollections.observableArrayList(csvPlants);
        controller = csvController;
        service = csvService;
        tableView.setItems(plantData);

        // Настройка колонок
        setupColumns();

        // Фильтр
        statusFilter.getItems().addAll("Все","Активно","Неактивно");
        statusFilter.setValue("Все");
        statusFilter.setOnAction(e -> {
            currentFilter = statusFilter.getValue();
            applyFilterAndSearch();
        });

        // Поиск
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilterAndSearch());

        // Автообновление каждый час
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3600), e -> refreshTable()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void switchDataSource() {
        String selected = dataSourceComboBox.getValue();
        if ("CSV".equals(selected)) {
            plantData = csvPlants;
            masterPlantData = FXCollections.observableArrayList(csvPlants);
            controller = csvController;
            service = csvService;
        } else if ("PostgreSQL".equals(selected)) {
            plantData = postgresPlants;
            masterPlantData = FXCollections.observableArrayList(postgresPlants);
            controller = postgresController;
            service = postgresService;
        } else if ("JSON".equals(selected)) {
            plantData = jsonPlants;
            masterPlantData = FXCollections.observableArrayList(jsonPlants);
            controller = jsonController;
            service = jsonService;
        }
        tableView.setItems(plantData);
        currentFilter = "Все";
        statusFilter.setValue("Все");
        searchField.clear();
        applyFilterAndSearch();
    }

    private void setupColumns() {
        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        typeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType()));
        lastWateredCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getLastWatered() != null ? cell.getValue().getLastWatered().toString() : ""
        ));
        nextWateringCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getNextWatering() != null ? cell.getValue().getNextWatering().toString() : ""
        ));
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getStatus() != null ? cell.getValue().getStatus() : ""
        ));
        nextRepotCol.setCellValueFactory(cell -> new SimpleStringProperty(
                service.getNextRepotDate(cell.getValue()) != null ? service.getNextRepotDate(cell.getValue()).toString() : ""
        ));
        diagnosisCol.setCellValueFactory(cell -> new SimpleStringProperty(
                service.diagnoseDisease(cell.getValue())
        ));

        nextWateringCol.setCellFactory(col -> new TableCell<Plant,String>(){
            @Override
            protected void updateItem(String item, boolean empty){
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("");
                if(!empty && item != null){
                    Plant plant = getTableView().getItems().get(getIndex());
                    if(plant.getNextWatering() != null){
                        LocalDate today = LocalDate.now();
                        LocalDate next = plant.getNextWatering();
                        String bgColor;
                        if(next.isBefore(today)) bgColor="#ff9494";
                        else if(next.isEqual(today)) bgColor="#fffe80";
                        else bgColor="#a1ff9e";

                        setStyle("-fx-background-color:" + bgColor + ";" +
                                "-fx-text-fill:black;" +
                                "-fx-border-color:ebe8e6;" +
                                "-fx-border-width:0 1 1 0;" +
                                "-fx-alignment:CENTER;");
                    }
                }
            }
        });
    }

    private void refreshTable() {
        masterPlantData.setAll(controller.getAllPlants());
        applyFilterAndSearch();
    }

    @FXML
    private void handleAdd() {
        Plant newPlant = PlantDialogs.addPlantDialog(controller, masterPlantData, tableView);
        if (newPlant != null) {
            // проверка дубликатов только в текущем источнике
            boolean exists = masterPlantData.stream()
                    .anyMatch(p -> p.getName().equalsIgnoreCase(newPlant.getName()));
            if(exists){
                showAlert("Ошибка", "Растение '" + newPlant.getName() + "' уже существует в текущем источнике.");
                return;
            }

            controller.addPlant(newPlant);
            masterPlantData.add(newPlant);
            plantData.add(newPlant);
            tableView.refresh();
        }
    }

    @FXML
    private void handleEdit() {
        Plant selected = getSelectedPlantOrWarn("Выберите растение для редактирования.");
        if (selected != null) {
            Plant updated = PlantDialogs.editPlantDialog(controller, masterPlantData, tableView, currentFilter);
            if (updated != null) {
                controller.updatePlant(selected.getName(), updated);
                refreshTable();
            }
        }
    }

    @FXML
    private void handleDelete() {
        Plant selected = getSelectedPlantOrWarn("Выберите растение для удаления.");
        if(selected != null){
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Подтверждение удаления");
            confirm.setHeaderText(null);
            confirm.setContentText("Вы уверены, что хотите удалить растение '" + selected.getName() + "'?");

            Optional<ButtonType> result = confirm.showAndWait();
            if(result.isPresent() && result.get() == ButtonType.OK){
                controller.deletePlant(selected.getName());
                masterPlantData.remove(selected);
                plantData.remove(selected);
                showAlert("Удаление", "Растение '" + selected.getName() + "' удалено.");
                applyFilterAndSearch();
            }
        }
    }

    @FXML
    private void handleWater() {
        Plant selected = getSelectedPlantOrWarn("Выберите растение для полива.");
        if(selected != null){
            controller.performWatering(selected);
            controller.updatePlant(selected.getName(), selected);
            tableView.refresh();
            showAlert("Полив", "Полив растения '"+selected.getName()+"' произведён. Дата обновлена: "+LocalDate.now());
        }
    }

    @FXML
    private void handleShowHistory() {
        Plant selected = getSelectedPlantOrWarn("Выберите растение для просмотра истории полива.");
        if (selected == null) return;

        List<LocalDate> history = selected.getWateringHistory();
        StringBuilder sb = new StringBuilder();
        for(LocalDate d : history) sb.append(d.toString()).append("\n");
        if(sb.length() == 0) sb.append("История полива отсутствует.");

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("История полива: "+selected.getName());
        alert.setHeaderText(null);
        alert.getDialogPane().setContentText(sb.toString());

        ButtonType clearButton = new ButtonType("Очистить историю", ButtonBar.ButtonData.LEFT);
        ButtonType closeButton = new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(clearButton, closeButton);

        Optional<ButtonType> result = alert.showAndWait();
        if(result.isPresent() && result.get() == clearButton){
            selected.getWateringHistory().clear();
            controller.updatePlant(selected.getName(), selected);
            tableView.refresh();
            showAlert("Очистка истории", "История полива для '"+selected.getName()+"' очищена.");
        }
    }

    private Plant getSelectedPlantOrWarn(String warnMessage){
        Plant selected = tableView.getSelectionModel().getSelectedItem();
        if(selected == null) showAlert("Ошибка", warnMessage);
        return selected;
    }

    private void showAlert(String title, String message){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void applyFilterAndSearch() {
        if (masterPlantData == null) return;

        String search = searchField != null ? searchField.getText().toLowerCase() : "";
        List<Plant> filtered = masterPlantData.stream()
                .filter(p -> currentFilter.equals("Все") || (p.getStatus() != null && p.getStatus().equals(currentFilter)))
                .filter(p -> p.getName().toLowerCase().contains(search) ||
                        p.getType().toLowerCase().contains(search) ||
                        p.getSymptoms().stream().anyMatch(s -> s.toLowerCase().contains(search)))
                .toList();

        plantData.setAll(filtered);
        tableView.refresh();
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        statusFilter.setValue("Все");
        currentFilter = "Все";
        applyFilterAndSearch();
    }
}