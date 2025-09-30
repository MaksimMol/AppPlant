package com.example.plant_app.controller;

import com.example.plant_app.model.Plant;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class PlantDialogs {

    @FXML private TextField nameField;
    @FXML private TextField typeField;
    @FXML private DatePicker lastWateredField;
    @FXML private TextField growthRateField;
    @FXML private TextField symptomsField;
    @FXML private Label symptomsHint;
    @FXML private ComboBox<String> statusBox;
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    private PlantController controller;
    private ObservableList<Plant> plantData;
    private Plant editingPlant;
    private Stage dialogStage;

    private PlantDialogs() { }

    public static Plant addPlantDialog(PlantController controller,
                                       ObservableList<Plant> masterPlantData,
                                       TableView<Plant> tableView) {
        return loadDialog(controller, masterPlantData, null);
    }

    public static Plant editPlantDialog(PlantController controller,
                                        ObservableList<Plant> masterPlantData,
                                        TableView<Plant> tableView,
                                        String currentFilter) {
        Plant selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return null;
        return loadDialog(controller, masterPlantData, selected);
    }

    private static Plant loadDialog(PlantController controller,
                                    ObservableList<Plant> plantData,
                                    Plant editingPlant) {
        try {
            FXMLLoader loader = new FXMLLoader(PlantDialogs.class.getResource("/com/example/plantapp/fxml/PlantDialog.fxml"));
            GridPane root = loader.load();
            PlantDialogs dialog = loader.getController();
            dialog.controller = controller;
            dialog.plantData = plantData;
            dialog.editingPlant = editingPlant;
            dialog.initDialog(root);
            dialog.showAndWait();
            return dialog.getResult();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initDialog(GridPane root) {
        dialogStage = new Stage();
        dialogStage.initStyle(StageStyle.UTILITY);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setScene(new Scene(root));

        if (editingPlant != null) {
            nameField.setText(editingPlant.getName());
            typeField.setText(editingPlant.getType());
            lastWateredField.setValue(editingPlant.getLastWatered());
            growthRateField.setText(String.valueOf(editingPlant.getGrowthRate()));
            symptomsField.setText(String.join(",", editingPlant.getSymptoms()));
            statusBox.getItems().addAll("Активно", "Неактивно");
            statusBox.setValue(editingPlant.getStatus() != null ? editingPlant.getStatus() : "Активно");
        } else {
            statusBox.getItems().addAll("Активно", "Неактивно");
            statusBox.setValue("Активно");
            lastWateredField.setValue(LocalDate.now());
        }

        // Автоподстановка типа по имени
        nameField.focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) {
                String nm = nameField.getText().trim();
                if (!nm.isEmpty()) {
                    String detected = controller.getPlantService().detectTypeByName(nm);
                    typeField.setText(detected != null ? detected : "");
                }
            }
        });

        // Кнопка OK
        okButton.setOnAction(ev -> {
            String nm = nameField.getText().trim();
            if (nm.isEmpty()) {
                showAlert("Ошибка", "Поле 'Название' обязательно.");
                return;
            }
            if (!controller.getPlantService().isValidPlantName(nm)) {
                showAlert("Ошибка", "Растение '" + nm + "' не найдено в справочнике.");
                return;
            }

            boolean exists = plantData.stream()
                    .anyMatch(p -> editingPlant == null
                            ? p.getName().equalsIgnoreCase(nm)
                            : !p.getName().equalsIgnoreCase(editingPlant.getName()) && p.getName().equalsIgnoreCase(nm));
            if (exists) {
                showAlert("Ошибка", "Растение '" + nm + "' уже существует в списке.");
                return;
            }

            List<String> symptoms = Arrays.stream(symptomsField.getText().trim().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (symptoms.isEmpty()) {
                symptoms.add("Признаков заболеваний не выявлено");
            }

            double growthRate;
            try {
                growthRate = Double.parseDouble(growthRateField.getText());
            } catch (NumberFormatException e) {
                growthRate = 1.0;
            }

            if (editingPlant != null) {
                editingPlant.setName(nm);
                editingPlant.setType(typeField.getText().trim());
                editingPlant.setLastWatered(lastWateredField.getValue());
                editingPlant.setGrowthRate(growthRate);
                editingPlant.setSymptoms(symptoms);
                editingPlant.setStatus(statusBox.getValue());
                controller.getPlantService().calculateNextWatering(editingPlant);
            } else {
                Plant newPlant = new Plant(
                        nm,
                        typeField.getText().trim(),
                        lastWateredField.getValue(),
                        growthRate,
                        symptoms,
                        statusBox.getValue(),
                        controller.getPlantService().calculateNextWateringForNewPlant(
                                new Plant(nm, typeField.getText().trim(), lastWateredField.getValue(), growthRate, symptoms, statusBox.getValue(), null)
                        )
                );
                plantData.add(newPlant);
            }

            dialogStage.close();
        });

        // Кнопка Cancel
        cancelButton.setOnAction(ev -> dialogStage.close());
    }

    private Plant getResult() {
        return editingPlant;
    }

    private void showAndWait() {
        dialogStage.showAndWait();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
