package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.example.domain.service.TaskService;
import org.example.exceptions.ValidationException;
import org.example.util.DomainServiceUtil;

import java.time.LocalDate;

public class MainController {

    private final TaskService taskService = DomainServiceUtil.getTaskService();

    @FXML private Pane overlay;
    @FXML private VBox taskContainer;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker datePicker;
    @FXML private Label errorLabel;

    @FXML
    private void showOverlay() {
        clearErrorState();
        titleField.clear();
        descriptionField.clear();
        titleField.setStyle("");
        descriptionField.setStyle("");
        datePicker.setValue(LocalDate.now());
        overlay.setVisible(true);
    }

    @FXML
    private void hideOverlay() {
        overlay.setVisible(false);
    }

    @FXML
    private void handleSave() {
        try {
            String title = titleField.getText();
            String description = descriptionField.getText();
            LocalDate date = datePicker.getValue();

            taskService.createAndSave(title, description, date);
            hideOverlay();
        } catch (ValidationException ex){
            errorLabel.setText(ex.getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);

            titleField.setStyle("-fx-border-color: #e74c3c;");
        }
    }

    private void clearErrorState(){
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setText("");
    }

}