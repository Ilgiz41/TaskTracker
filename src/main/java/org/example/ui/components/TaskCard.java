package org.example.ui.components;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.controller.MainController;
import org.example.domain.model.Task;
import org.example.domain.model.TaskUpdatePayload;
import org.example.domain.service.TaskService;

import java.time.LocalDate;

public class TaskCard extends HBox {
    private final Task task;
    private final TaskService taskService;
    private final MainController controller;
    private final boolean isTemplateMode;
    private final LocalDate selectedDate;
    private VBox textContent;

    public TaskCard(Task task, TaskService taskService, MainController controller, boolean isTemplateMode, LocalDate selectedDate) {
        this.task = task;
        this.taskService = taskService;
        this.controller = controller;
        this.isTemplateMode = isTemplateMode;
        this.selectedDate = selectedDate;
        this.setSpacing(15);
        this.buildUI();
    }

    private void buildUI() {
        this.getStyleClass().add("task-card");
        this.getStyleClass().add(getPriorityBorderClass());
        this.setAlignment(Pos.TOP_LEFT);
        if (task.isCompleted() && !isTemplateMode) this.getStyleClass().add("completed");
        this.prefWidthProperty().bind(controller.getTaskListView().widthProperty().subtract(40));
        textContent = new VBox(8);
        HBox.setHgrow(textContent, Priority.ALWAYS);

        if (task.isRegular() && !isTemplateMode) {
            textContent.getChildren().add(createRegularBadge());
        }

        textContent.getChildren().addAll(
                createPriorityArea(),
                createTitleArea(),
                createDescriptionArea()
        );

        VBox actionBox = new VBox(12);
        actionBox.setAlignment(Pos.TOP_RIGHT);
        actionBox.setMinWidth(160);
        actionBox.getChildren().addAll(
                createStatusButton(),
                createActionBar()
        );

        this.getChildren().addAll(textContent, actionBox);
    }

    private Node createPriorityArea() {
        Label label = new Label(getPriorityText());
        label.getStyleClass().addAll("p-label", getPriorityTextClass());
        label.setCursor(Cursor.HAND);
        label.setOnMousePressed(e -> {
            if (task.isCompleted() && !isTemplateMode) return;
            ComboBox<String> combo = new ComboBox<>();
            combo.getItems().addAll("Низкий", "Средний", "Высокий");
            combo.setValue(getPriorityText());
            combo.getStyleClass().add("compact-combo");
            int idx = textContent.getChildren().indexOf(label);
            textContent.getChildren().set(idx, combo);
            Platform.runLater(combo::show);
            combo.valueProperty().addListener((obs, ov, nv) -> {
                if (nv != null) {
                    int p = nv.equals("Высокий") ? 3 : nv.equals("Средний") ? 2 : 1;
                    updateTaskData(task.getTitle(), task.getDescription(), p, task.isCompleted(), task.getDate());
                }
            });
            combo.focusedProperty().addListener((o, ov, nv) -> { if(!nv) textContent.getChildren().set(idx, label); });
        });
        return label;
    }

    private Node createTitleArea() {
        Label label = new Label(task.getTitle());
        label.getStyleClass().add("task-title");
        label.setWrapText(true);
        label.setCursor(Cursor.HAND);
        label.setOnMousePressed(e -> {
            if (task.isCompleted() && !isTemplateMode) return;
            TextField edit = new TextField(task.getTitle());
            edit.getStyleClass().add("task-title-edit");
            int idx = textContent.getChildren().indexOf(label);
            textContent.getChildren().set(idx, edit);
            Platform.runLater(edit::requestFocus);
            edit.setOnAction(ae -> {
                updateTaskData(edit.getText(), task.getDescription(), task.getPriority(), task.isCompleted(), task.getDate());
                textContent.getChildren().set(idx, label);
            });
            edit.focusedProperty().addListener((o, ov, nv) -> {
                if(!nv && textContent.getChildren().contains(edit)) {
                    updateTaskData(edit.getText(), task.getDescription(), task.getPriority(), task.isCompleted(), task.getDate());
                    textContent.getChildren().set(idx, label);
                }
            });
        });
        return label;
    }

    private Node createDescriptionArea() {
        String description = task.getDescription() == null ? "" : task.getDescription();
        String displayDesc = description.isEmpty() ? "Добавить описание..." : description;
        Label label = new Label(displayDesc);
        label.getStyleClass().add("task-desc");
        label.setWrapText(true);
        label.setCursor(Cursor.HAND);
        label.setMaxWidth(800);
        label.setOnMousePressed(e -> {
            if (task.isCompleted() && !isTemplateMode) return;
            TextArea edit = new TextArea(description);
            edit.setWrapText(true);
            edit.getStyleClass().add("task-desc-edit-large");
            long lineBreaks = description.chars().filter(ch -> ch == '\n').count();
            int rows = Math.max(3, Math.min((description.length() / 180) + (int)lineBreaks + 2, 15));
            double targetHeight = rows * 24 + 30;
            edit.setPrefHeight(targetHeight);
            int idx = textContent.getChildren().indexOf(label);
            textContent.getChildren().set(idx, edit);
            Platform.runLater(edit::requestFocus);
            edit.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                    updateTaskData(task.getTitle(), edit.getText(), task.getPriority(), task.isCompleted(), task.getDate());
                    textContent.getChildren().set(idx, label);
                    event.consume();
                }
            });
            edit.focusedProperty().addListener((o, ov, nv) -> {
                if (!nv && textContent.getChildren().contains(edit)) {
                    updateTaskData(task.getTitle(), edit.getText(), task.getPriority(), task.isCompleted(), task.getDate());
                    textContent.getChildren().set(idx, label);
                }
            });
        });
        return label;
    }

    private Node createActionBar() {
        HBox actionBar = new HBox(10);
        actionBar.setAlignment(Pos.CENTER_RIGHT);
        if (!isTemplateMode) {
            CheckBox completedCheck = new CheckBox();
            completedCheck.setSelected(task.isCompleted());
            completedCheck.setCursor(Cursor.HAND);
            completedCheck.setTooltip(new Tooltip("Завершить задачу"));
            completedCheck.selectedProperty().addListener((obs, ov, nv) -> {
                updateTaskData(task.getTitle(), task.getDescription(), task.getPriority(), nv, task.getDate());
            });
            actionBar.getChildren().add(completedCheck);
        }
        if (!isTemplateMode) {
            Button postponeBtn = new Button("📅");
            postponeBtn.getStyleClass().add("btn-action-postpone");
            postponeBtn.setTooltip(new Tooltip("Перенести на завтра (+1 день)"));
            postponeBtn.setCursor(Cursor.HAND);
            postponeBtn.setOnAction(e -> {
                LocalDate tomorrow = task.getDate().plusDays(1);
                updateTaskData(task.getTitle(), task.getDescription(), task.getPriority(), task.isCompleted(), tomorrow);
            });
            actionBar.getChildren().add(postponeBtn);
        }
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("btn-action-delete");
        deleteBtn.setTooltip(new Tooltip("Удалить задачу"));
        deleteBtn.setCursor(Cursor.HAND);
        deleteBtn.setOnAction(e -> taskService.deleteTask(task, selectedDate));
        actionBar.getChildren().add(deleteBtn);
        return actionBar;
    }

    private void updateTaskData(String t, String d, int p, boolean c, LocalDate newDate) {
        if (t.equals(task.getTitle()) &&
                d.equals(task.getDescription()) &&
                p == task.getPriority() &&
                c == task.isCompleted() &&
                newDate.equals(task.getDate())) return;
        TaskUpdatePayload payload = new TaskUpdatePayload(task.getId(), t, d, p, newDate, c);
        if (isTemplateMode) {
            taskService.updateRegularTemplate(task, payload, selectedDate);
        } else {
            taskService.updateTask(task, selectedDate, payload);
        }
    }

    private Button createStatusButton() {
        String text = task.isCompleted() ? "Готово" : (task.getDate().isBefore(LocalDate.now()) && !isTemplateMode ? "Просрочено" : "В процессе");
        Button btn = new Button(text);
        btn.getStyleClass().addAll("btn-status", task.isCompleted() ? "bg-done" : (task.getDate().isBefore(LocalDate.now()) && !isTemplateMode ? "bg-overdue" : "bg-process"));
        btn.setMinWidth(120);
        return btn;
    }

    private HBox createRegularBadge() {
        Label icon = new Label("🔄");
        Label text = new Label("РЕГУЛЯРНАЯ");
        HBox badge = new HBox(5, icon, text);
        badge.getStyleClass().add("regular-badge");
        badge.setAlignment(Pos.CENTER_LEFT);
        return badge;
    }

    private String getPriorityBorderClass() { return switch (task.getPriority()) { case 3 -> "p-high"; case 2 -> "p-medium"; default -> "p-low"; }; }
    private String getPriorityTextClass() { return switch (task.getPriority()) { case 3 -> "p-text-high"; case 2 -> "p-text-medium"; default -> "p-text-low"; }; }
    private String getPriorityText() { return switch (task.getPriority()) { case 3 -> "Высокий"; case 2 -> "Средний"; default -> "Низкий"; }; }
}