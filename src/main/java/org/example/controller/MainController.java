package org.example.controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import lombok.Getter;
import org.example.domain.model.Task;
import org.example.domain.service.TaskService;
import org.example.event.Event.*;
import org.example.exceptions.ValidationException;
import org.example.ui.components.TaskCard;
import org.example.util.DomainServiceUtil;
import org.example.util.EventBus;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MainController implements Initializable {

    private final TaskService taskService = DomainServiceUtil.getTaskService();

    @FXML
    private Pane overlay;
    @Getter
    @FXML
    private ListView<Task> taskListView;
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
    @FXML
    private TextField searchField;
    @FXML
    private ToggleButton normalTaskToggle;
    @FXML
    private ToggleButton regularTaskToggle;
    @FXML
    private ToggleGroup taskTypeGroup;
    @FXML
    private VBox daysOfWeekContainer;
    @FXML
    private VBox datePickerContainer;
    @FXML
    public HBox daysButtonsBox;

    private ContextMenu searchResultMenu = new ContextMenu();
    private LocalDate selectedDate = LocalDate.now();
    private boolean isTemplateMode = false;
    private final EventBus eventBus = DomainServiceUtil.getEventBus();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Platform.runLater(() -> {
            if (taskListView.getScene() != null) {
                String css = Objects.requireNonNull(getClass().getResource("/css/TaskCard.css")).toExternalForm();
                if (!taskListView.getScene().getStylesheets().contains(css)) {
                    taskListView.getScene().getStylesheets().add(css);
                }
            }
        });

        taskListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(new TaskCard(item, taskService, MainController.this, isTemplateMode, selectedDate));
                    setStyle("-fx-background-color: transparent; -fx-padding: 5;");
                }
            }
        });

        setupPriorityComboBox();
        setupSearchLogic();
        setupTaskTypeToggle();
        setupDayButtons();
        updateDateDisplay();
        refreshTaskList();

        eventBus.subscribe(RefreshFullTaskListEvent.class, e -> refreshTaskList());
    }

    public void smoothScrollToTask(Task targetTask) {
        int index = taskListView.getItems().indexOf(targetTask);
        if (index == -1) return;

        ScrollBar verticalBar = (ScrollBar) taskListView.lookup(".scroll-bar:vertical");

        if (verticalBar != null) {
            double targetValue = (double) index / (taskListView.getItems().size() - 1);
            Timeline scrollTimeline = new Timeline(
                    new KeyFrame(Duration.millis(600),
                            new KeyValue(verticalBar.valueProperty(), targetValue, Interpolator.EASE_BOTH)
                    )
            );

            scrollTimeline.setOnFinished(e -> {
                Platform.runLater(() -> {
                    for (Node node : taskListView.lookupAll(".list-cell")) {
                        if (node instanceof ListCell<?> cell) {
                            if (cell.getItem() != null && cell.getItem().equals(targetTask)) {
                                runPulseAnimation(cell);
                                break;
                            }
                        }
                    }
                });
            });
            scrollTimeline.play();
        } else {
            taskListView.scrollTo(index);
        }
    }

    private void runPulseAnimation(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(300), node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.03);
        st.setToY(1.03);
        st.setCycleCount(4);
        st.setAutoReverse(true);

        FadeTransition ft = new FadeTransition(Duration.millis(300), node);
        ft.setFromValue(1.0);
        ft.setToValue(0.6);
        ft.setCycleCount(4);
        ft.setAutoReverse(true);

        String oldStyle = node.getStyle();
        node.setStyle(oldStyle + "-fx-background-color: rgba(0, 150, 255, 0.2); -fx-background-radius: 12;");

        ParallelTransition pt = new ParallelTransition(node, st, ft);
        pt.setOnFinished(e -> {
            node.setStyle(oldStyle);
            node.setScaleX(1.0);
            node.setScaleY(1.0);
            node.setOpacity(1.0);
        });
        pt.play();
    }

    private void handleSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchResultMenu.hide();
            return;
        }

        List<Task> results = taskService.dirtySearch(query);

        if (results.isEmpty()) {
            searchResultMenu.hide();
            return;
        }

        searchResultMenu.getItems().clear();
        results.stream().limit(10).forEach(task -> {
            MenuItem item = new MenuItem(task.getTitle());
            item.setOnAction(e -> {
                smoothScrollToTask(task);
                searchField.clear();
            });
            searchResultMenu.getItems().add(item);
        });

        if (!searchResultMenu.isShowing()) {
            searchResultMenu.show(searchField, Side.BOTTOM, 0, 0);
        }
    }

    @FXML
    public void openCalendar() {
        hiddenDatePicker.show();
    }

    @FXML
    public void handleCalendarAction() {
        if (hiddenDatePicker.getValue() != null) {
            selectedDate = hiddenDatePicker.getValue();
            taskService.loadTaskCacheForDate(selectedDate);
            updateDateDisplay();
            refreshTaskList();
        }
    }

    @FXML
    public void refreshTaskList() {
        taskService.getSortedTaskByPriority()
                .thenAccept(tasks -> {
                    Platform.runLater(() -> {
                        isTemplateMode = false;
                        updateStatistic(tasks);
                        taskListView.getItems().setAll(tasks);
                        taskListView.setPlaceholder(new Label("На этот день задач нет"));
                    });
                });
    }

    @FXML
    public void showRegularTasksManager() {
        Platform.runLater(() -> {
            isTemplateMode = true;
            List<Task> templates = taskService.getAllTemplates();
            taskListView.getItems().setAll(templates);
            taskListView.setPlaceholder(new Label("Шаблоны отсутствуют"));
        });
    }

    @FXML
    public void deleteAllTasksForDay() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Удалить все задачи на этот день?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            taskService.deleteAllTasksForDate(selectedDate);
        }
    }

    @FXML
    public void handlePrevDay() {
        selectedDate = selectedDate.minusDays(1);
        taskService.loadTaskCacheForDate(selectedDate);
        updateDateDisplay();
        refreshTaskList();
    }

    @FXML
    public void handleNextDay() {
        selectedDate = selectedDate.plusDays(1);
        taskService.loadTaskCacheForDate(selectedDate);
        updateDateDisplay();
        refreshTaskList();
    }

    private void updateStatistic(List<Task> taskList) {
        long total = taskList.size();
        long completed = taskList.stream().filter(Task::isCompleted).count();
        totalTasksLabel.setText("Всего задач: " + total);
        completedTasksLabel.setText("Выполнено: " + completed);
        double progress = (total > 0) ? (double) completed / total : 0;
        dayProgressBar.setProgress(progress);
        percentLabel.setText((int) (progress * 100) + "%");
    }

    private void setupPriorityComboBox() {
        priorityComboBox.getItems().setAll("Низкий", "Средний", "Высокий");
        priorityComboBox.setValue("Низкий");
    }

    private void setupSearchLogic() {
        searchField.textProperty().addListener((obs, old, nv) -> handleSearch(nv));
    }

    private void setupTaskTypeToggle() {
        taskTypeGroup.selectedToggleProperty().addListener((obs, old, nv) -> {
            boolean isRegular = (nv == regularTaskToggle);
            datePickerContainer.setVisible(!isRegular);
            datePickerContainer.setManaged(!isRegular);
            daysOfWeekContainer.setVisible(isRegular);
            daysOfWeekContainer.setManaged(isRegular);
        });
    }

    private void setupDayButtons() {
        daysButtonsBox.getChildren().clear();
        String[] dayNames = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (int i = 0; i < dayNames.length; i++) {
            ToggleButton btn = new ToggleButton(dayNames[i]);
            btn.setUserData(i + 1);
            btn.setMinWidth(40);
            btn.setPrefHeight(40);
            btn.setCursor(javafx.scene.Cursor.HAND);
            btn.setStyle("-fx-background-radius: 20; -fx-border-radius: 20;");
            daysButtonsBox.getChildren().add(btn);
        }
    }

    private Set<DayOfWeek> getSelectedDays() {
        return daysButtonsBox.getChildren().stream()
                .filter(n -> n instanceof ToggleButton && ((ToggleButton) n).isSelected())
                .map(n -> DayOfWeek.of((Integer) n.getUserData()))
                .collect(Collectors.toSet());
    }

    public void updateDateDisplay() {
        currentDateLabel.setText(selectedDate.format(DateTimeFormatter.ofPattern("d MMMM", new Locale("ru"))));
    }

    @FXML
    private void handleSave() {
        try {
            String title = titleField.getText();
            String desc = descriptionField.getText();
            int priority = switch (priorityComboBox.getValue()) {
                case "Высокий" -> 3;
                case "Средний" -> 2;
                default -> 1;
            };

            if (taskTypeGroup.getSelectedToggle() == regularTaskToggle) {
                taskService.createAndSaveRegularTemplate(title, desc, priority, getSelectedDays(), selectedDate);
            } else {
                taskService.createAndSave(title, desc, datePicker.getValue(), selectedDate, priority);
            }
            hideOverlay();
        } catch (ValidationException ex) {
            errorLabel.setText(ex.getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    @FXML
    private void showOverlay() {
        errorLabel.setVisible(false);
        titleField.clear();
        descriptionField.clear();
        datePicker.setValue(selectedDate);
        overlay.setVisible(true);
    }

    @FXML
    private void hideOverlay() {
        overlay.setVisible(false);
    }
}