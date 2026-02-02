package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.controller.MainController;
import org.example.domain.service.TaskService;
import org.example.util.DomainServiceUtil;

import java.time.LocalDate;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        Application.setUserAgentStylesheet(new atlantafx.base.theme.PrimerDark().getUserAgentStylesheet());
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainScene.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Мой Таск Трекер");
        stage.setScene(scene);
        stage.show();

        MainController mainController = loader.getController();
        TaskService taskService = DomainServiceUtil.getTaskService();
        taskService.loadCacheByDate(LocalDate.now());
        mainController.refreshTaskList();
        mainController.updateDateDisplay();
    }
}