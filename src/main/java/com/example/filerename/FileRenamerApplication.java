package com.example.filerename;

import com.example.filerename.controller.MainController;
import com.example.filerename.service.RenameExecutor;
import com.example.filerename.service.RenamePlanner;
import com.example.filerename.strategy.NamingStrategy;
import com.example.filerename.strategy.SequentialNamingStrategy;
import com.example.filerename.validation.RenameValidator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

public final class FileRenamerApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        NamingStrategy namingStrategy = new SequentialNamingStrategy();
        RenamePlanner renamePlanner = new RenamePlanner(namingStrategy);
        RenameValidator renameValidator = new RenameValidator();
        RenameExecutor renameExecutor = new RenameExecutor();

        FXMLLoader loader = new FXMLLoader(
                FileRenamerApplication.class.getResource("main-view.fxml")
        );
        loader.setControllerFactory(type -> {
            if (type == MainController.class) {
                return new MainController(renamePlanner, renameValidator, renameExecutor);
            }
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unable to create controller: " + type.getName(), exception);
            }
        });

        Scene scene = new Scene(loader.load(), 860, 600);
        scene.setFill(Color.web("#0A0D14"));

        stage.setTitle("FileRenamer");
        stage.setMinWidth(560);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
