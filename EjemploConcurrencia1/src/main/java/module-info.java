module org.example.ejemploconcurrencia1 {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.ejemploconcurrencia1 to javafx.fxml;
    exports org.example.ejemploconcurrencia1;
}