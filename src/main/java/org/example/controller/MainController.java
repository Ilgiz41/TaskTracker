package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import org.example.domain.model.Task;
import org.example.domain.service.TaskService;
import org.example.exceptions.ValidationException;
import org.example.util.DomainServiceUtil;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class MainController implements Initializable {

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
    @FXML
    private ComboBox<String> priorityComboBox;

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

            int priority = switch (priorityComboBox.getValue()){
                case "Высокий" -> 3;
                case "Средний" -> 2;
                default -> 1;
            };

            taskService.createAndSave(title, description, date, selectedDate, priority);
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

        List<Task> taskList = taskService.getSortedTaskByPriority();

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

        String priorityColor;
        String priorityText;

        switch (task.getPriority()) {
            case 3 -> { priorityColor = "#e74c3c"; priorityText = "Высокий"; }
            case 2 -> { priorityColor = "#fbc02d"; priorityText = "Средний"; }
            default -> { priorityColor = "#2ecc71"; priorityText = "Низкий"; }
        }

        card.setStyle(String.format(
                "-fx-background-color: -color-bg-subtle; -fx-background-radius: 12; " +
                        "-fx-border-color: %s; -fx-border-width: 1.5; -fx-border-radius: 12;",
                priorityColor
        ));

        VBox textContent = new VBox(5);
        HBox.setHgrow(textContent, Priority.ALWAYS);
        textContent.setMinWidth(0);

        Label pLabel = new Label(priorityText);
        pLabel.setCursor(Cursor.HAND);
        pLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + priorityColor + ";");

        pLabel.setOnMouseClicked(e -> {
            if (!task.isCompleted()) {
                ComboBox<String> pCombo = new ComboBox<>();
                pCombo.getItems().addAll("Низкий", "Средний", "Высокий");
                pCombo.setValue(priorityText);
                pCombo.setPrefWidth(120);

                int index = textContent.getChildren().indexOf(pLabel);
                if (index != -1) {
                    textContent.getChildren().set(index, pCombo);
                    pCombo.requestFocus();
                    pCombo.show();

                    pCombo.setOnAction(ae -> {
                        String selected = pCombo.getValue();
                        if (selected != null) {
                            int newP = switch (selected) {
                                case "Высокий" -> 3;
                                case "Средний" -> 2;
                                default -> 1;
                            };
                            if (task.getPriority() != newP) {
                                task.setPriority(newP);
                                taskService.updateTask(task, selectedDate);
                                refreshTaskList();
                            } else {
                                textContent.getChildren().set(index, pLabel);
                            }
                        }
                    });

                    pCombo.setOnHidden(ce -> {
                        Platform.runLater(() -> {
                            if (textContent.getChildren().contains(pCombo)) {
                                textContent.getChildren().set(index, pLabel);
                            }
                        });
                    });
                }
            }
        });

        Label title = new Label(task.getTitle());
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setCursor(Cursor.HAND);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        title.setOnMouseClicked(e -> {
            if (!task.isCompleted()) {
                TextField titleEdit = new TextField(title.getText());
                titleEdit.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
                int index = textContent.getChildren().indexOf(title);
                textContent.getChildren().set(index, titleEdit);
                titleEdit.requestFocus();
                titleEdit.setOnAction(ae -> finalizeTitleEdit(textContent, titleEdit, title, task, index));
                titleEdit.focusedProperty().addListener((obs, ov, nv) -> { if (!nv) finalizeTitleEdit(textContent, titleEdit, title, task, index); });
            }
        });

        Label desc = new Label(task.getDescription());
        desc.setWrapText(true);
        desc.setMaxWidth(Double.MAX_VALUE);
        desc.setCursor(Cursor.HAND);
        desc.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 14px;");

        desc.setOnMouseClicked(e -> {
            if (!task.isCompleted()) {
                TextArea descEdit = new TextArea(desc.getText());
                descEdit.setWrapText(true);
                descEdit.setPrefRowCount(3);
                int index = textContent.getChildren().indexOf(desc);
                textContent.getChildren().set(index, descEdit);
                descEdit.requestFocus();
                descEdit.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        if (event.isShiftDown()) { descEdit.appendText("\n"); event.consume(); }
                        else { finalizeDescEdit(textContent, descEdit, desc, task, index); event.consume(); }
                    }
                });
                descEdit.focusedProperty().addListener((obs, ov, nv) -> {
                    if (!nv && textContent.getChildren().contains(descEdit)) finalizeDescEdit(textContent, descEdit, desc, task, index);
                });
            }
        });

        if (task.isCompleted()) {
            title.setStyle(title.getStyle() + "-fx-opacity: 0.5; -fx-strikethrough: true;");
            desc.setStyle(desc.getStyle() + "-fx-opacity: 0.5;");
            pLabel.setStyle(pLabel.getStyle() + "-fx-opacity: 0.5;");
            card.setStyle(card.getStyle() + "-fx-border-color: -color-border-muted;");
        }

        textContent.getChildren().addAll(pLabel, title, desc);

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

    private void finalizeTitleEdit(VBox container, TextField field, Label label, Task task, int index) {
        String text = field.getText().trim();
        if (!text.isEmpty()) {
            task.setTitle(text);
            label.setText(text);
            taskService.updateTask(task, selectedDate);
        }
        container.getChildren().set(index, label);
    }

    private void finalizeDescEdit(VBox container, TextArea area, Label label, Task task, int index) {
        if (container.getChildren().contains(area)) {
            String text = area.getText().trim();
            task.setDescription(text);
            label.setText(text);
            taskService.updateTask(task, selectedDate);

            container.getChildren().set(index, label);
            refreshTaskList();
        }
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

            int percent = (int) (progress * 100);
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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        priorityComboBox.getItems().addAll("Низкий", "Средний", "Высокий");
        priorityComboBox.setValue("Низкий");

        priorityComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    if (item.equals("Высокий")) setStyle("-fx-text-fill: -color-danger-emphasis; -fx-font-weight: bold;");
                    else if (item.equals("Средний")) setStyle("-fx-text-fill: -color-warning-emphasis; -fx-font-weight: bold;");
                    else setStyle("-fx-text-fill: -color-success-emphasis; -fx-font-weight: bold;");
                }
            }
        });

        priorityComboBox.setButtonCell(priorityComboBox.getCellFactory().call(null));
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
