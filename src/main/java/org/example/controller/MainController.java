package org.example.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.domain.model.Task;
import org.example.domain.service.TaskService;
import org.example.exceptions.ValidationException;
import org.example.util.DomainServiceUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MainController {

    private final TaskService taskService = DomainServiceUtil.getTaskService();

    @FXML
    private Pane overlay;
    @FXML
    private VBox taskContainer;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Label errorLabel;
    @FXML
    private Label currentDateLabel;
    @FXML
    private DatePicker hiddenDatePicker;
    @FXML
    private Label totalTasksLabel;
    @FXML
    private Label completedTasksLabel;
    @FXML
    private ProgressBar dayProgressBar;
    @FXML
    private Label percentLabel;

    private LocalDate selectedDate = LocalDate.now();

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

            taskService.createAndSave(title, description, date, selectedDate);
            hideOverlay();
            refreshTaskList();
        } catch (ValidationException ex) {
            errorLabel.setText(ex.getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);

            titleField.setStyle("-fx-border-color: #e74c3c;");
        }
    }

    @FXML
    public void refreshTaskList() {
        taskContainer.getChildren().clear();

        List<Task> taskList = taskService.getSortedTasksByDate();

        updateStatistic(taskList);

        if (taskList.isEmpty()) {
            Label emptyLabel = new Label("No tasks found");
            emptyLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-style: italic;");
            taskContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Task task : taskList) {
            HBox taskCard = createCard(task);
            taskContainer.getChildren().add(taskCard);
        }
    }

    private HBox createCard(Task task) {
        HBox card = new HBox();
        card.setSpacing(15);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 12; -fx-border-color: -color-border-muted; -fx-border-radius: 12;");

        VBox textContent = new VBox(5);
        HBox.setHgrow(textContent, Priority.ALWAYS);

        textContent.setMinWidth(0);
        textContent.setPrefWidth(100);

        Label title = new Label(task.getTitle());
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label desc = new Label(task.getDescription());
        desc.setWrapText(true);
        desc.setMaxWidth(Double.MAX_VALUE);
        desc.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 14px;");

        if (task.isCompleted()) {
            title.setStyle(title.getStyle() + "-fx-opacity: 0.5; -fx-strikethrough: true;");
            desc.setStyle(desc.getStyle() + "-fx-opacity: 0.5;");
        }

        textContent.getChildren().addAll(title, desc);

        //кнопка
        Button statusBtn = new Button();
        statusBtn.setMinWidth(110);
        statusBtn.setMaxWidth(110);
        updateStatusBtnStyle(statusBtn, task);

        ContextMenu menu = new ContextMenu();
        MenuItem completeItem = new MenuItem(task.isCompleted() ? "Вернуть в работу" : "Завершить");
        completeItem.setOnAction(e -> {
            task.setCompleted(!task.isCompleted());
            taskService.updateTask(task, selectedDate);
            refreshTaskList();
        });

        MenuItem moveItem = new MenuItem("Перенести на завтра");
        moveItem.setOnAction(e -> {
            task.setDate(task.getDate().plusDays(1));
            taskService.updateTask(task, selectedDate);
            refreshTaskList();
        });

        MenuItem deleteItem = new MenuItem("Удалить");
        deleteItem.setStyle("-fx-text-fill: #e74c3c;");
        deleteItem.setOnAction(e -> {
            taskService.deleteTask(task);
            refreshTaskList();
        });

        menu.getItems().addAll(completeItem, moveItem, new SeparatorMenuItem(), deleteItem);
        statusBtn.setOnAction(e -> menu.show(statusBtn, Side.BOTTOM, 0, 0));

        card.getChildren().addAll(textContent, statusBtn);
        return card;
    }

    private void updateStatusBtnStyle(Button btn, Task task) {
        String color = "#f1c40f";
        String text = "В процессе";

        if (task.isCompleted()) {
            color = "#2ecc71";
            text = "Готово";
        } else if (task.getDate().isBefore(LocalDate.now())) {
            color = "#e74c3c";
            text = "Просрочено";
        }

        btn.setText(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 15;");
    }

    private void updateStatistic(List<Task> taskList) {
        long taskCount = taskList.size();
        long completedTaskCount = taskList.stream().filter(Task::isCompleted).count();

        totalTasksLabel.setText(String.valueOf(taskCount));
        completedTasksLabel.setText(String.valueOf(completedTaskCount));

        if (taskCount > 0 && completedTaskCount > 0) {
            double progress = (double) completedTaskCount / (double) taskCount;
            dayProgressBar.setProgress(progress);

            int percent = (int)(progress * 100);
            percentLabel.setText(percent + "%");

            if (percent == 100) {
                dayProgressBar.setStyle("-fx-accent: #2ecc71;");
            } else {
                dayProgressBar.setStyle("");
            }
        } else {
            dayProgressBar.setProgress(0);
            percentLabel.setText("0%");
        }
    }

    private void clearErrorState() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setText("");
    }

    public void handlePrevDay() {
        selectedDate = selectedDate.minusDays(1);
        taskService.loadCacheByDate(selectedDate);
        updateDateDisplay();
        refreshTaskList();
    }

    public void openCalendar() {
        hiddenDatePicker.show();
    }

    public void handleCalendarAction() {
        if (hiddenDatePicker.getValue() != null) {
            selectedDate = hiddenDatePicker.getValue();
            taskService.loadCacheByDate(selectedDate);
            updateDateDisplay();
            refreshTaskList();
        }
    }

    public void handleNextDay() {
        selectedDate = selectedDate.plusDays(1);
        taskService.loadCacheByDate(selectedDate);
        updateDateDisplay();
        refreshTaskList();
    }

    public void updateDateDisplay() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM", new Locale("ru"));
        String formattedDate = selectedDate.format(formatter);

        currentDateLabel.setText(formattedDate);
    }

    @FXML
    private void deleteAllTasksForDay() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверждение");
        alert.setHeaderText("Удалить все задачи на " + currentDateLabel.getText() + "?");
        alert.setContentText("Это действие нельзя будет отменить.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            taskService.deleteAllTasksForDate();
            refreshTaskList();
        }
    }
}