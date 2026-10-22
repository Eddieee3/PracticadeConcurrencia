module org.example.ejemploconcurrencia1 {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.ejemploconcurrencia1.model to javafx.fxml;
    opens org.example.ejemploconcurrencia1.controller to javafx.fxml;
    exports org.example.ejemploconcurrencia1.model;
    exports org.example.ejemploconcurrencia1.controller;
}
