module com.example.filerename {
    requires javafx.controls;
    requires javafx.fxml;

    exports com.example.filerename;

    opens com.example.filerename.controller to javafx.fxml;
}
