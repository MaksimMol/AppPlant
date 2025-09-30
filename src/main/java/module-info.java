module Plant_app {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.sql;

    opens com.example.plant_app.model to com.fasterxml.jackson.databind;
    opens com.example.plant_app to javafx.fxml;
    opens com.example.plant_app.controller to javafx.fxml;
    exports com.example.plant_app;
}
