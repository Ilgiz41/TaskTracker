package org.example.ui.components;

import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
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
        this.setCache(true);
        this.setCacheHint(CacheHint.SPEED);
        buildUI();
    }

    private void buildUI() {
        this.setAlignment(Pos.CENTER_LEFT);
        this.getStyleClass().add("task-card");
        this.getStyleClass().add(getPriorityBorderClass());
        if (task.isCompleted() && !isTemplateMode) this.getStyleClass().add("completed");
        this.prefWidthProperty().bind(controller.getTaskListView().widthProperty().subtract(25));
        this.maxWidthProperty().bind(controller.getTaskListView().widthProperty().subtract(25));
        textContent = new VBox(5);
        HBox.setHgrow(textContent, Priority.ALWAYS);
        textContent.setMinWidth(0);
        if (task.isRegular() && !isTemplateMode) {
            textContent.getChildren().add(createRegularBadge());
        }
        textContent.getChildren().addAll(
                createPriorityLabel(),
                createTitleLabel(),
                createDescriptionLabel()
        );
        VBox actionBox = new VBox(createStatusButton());
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.setMinWidth(140);
        actionBox.setMaxWidth(140);
        this.getChildren().addAll(textContent, actionBox);
    }

    private Label createTitleLabel() {
        Label title = new Label(task.getTitle());
        title.getStyleClass().add("task-title");
        title.setWrapText(true);
        title.maxWidthProperty().bind(this.prefWidthProperty().subtract(170));
        if (task.isCompleted() && !isTemplateMode) title.getStyleClass().add("strikethrough");
        title.setOnMouseClicked(e -> {
            if (!task.isCompleted() || isTemplateMode) {
                TextField edit = new TextField(title.getText());
                edit.maxWidthProperty().bind(title.maxWidthProperty());
                int index = textContent.getChildren().indexOf(title);
                textContent.getChildren().set(index, edit);
                edit.requestFocus();
                edit.setOnAction(ae -> finalizeTitleEdit(edit, title, index));
                edit.focusedProperty().addListener((obs, ov, nv) -> {
                    if (!nv) finalizeTitleEdit(edit, title, index);
                });
            }
        });
        return title;
    }

    private Label createDescriptionLabel() {
        Label desc = new Label(task.getDescription());
        desc.getStyleClass().add("task-desc");
        desc.setWrapText(true);
        desc.setTextOverrun(OverrunStyle.ELLIPSIS);
        desc.maxWidthProperty().bind(this.prefWidthProperty().subtract(170));
        desc.setOnMouseClicked(e -> {
            if (!task.isCompleted() || isTemplateMode) {
                TextArea edit = new TextArea(desc.getText());
                edit.setWrapText(true);
                edit.setPrefRowCount(3);
                edit.maxWidthProperty().bind(desc.maxWidthProperty());
                int index = textContent.getChildren().indexOf(desc);
                textContent.getChildren().set(index, edit);
                edit.requestFocus();
                edit.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                        finalizeDescEdit(edit, desc, index);
                        event.consume();
                    }
                });
                edit.focusedProperty().addListener((obs, ov, nv) -> {
                    if (!nv) finalizeDescEdit(edit, desc, index);
                });
            }
        });
        return desc;
    }

    private Button createStatusButton() {
        Button btn = new Button();
        btn.getStyleClass().add("btn-status");
        btn.setMinWidth(130);
        btn.setCursor(Cursor.HAND);
        if (isTemplateMode) {
            btn.setText("Удалить");
            btn.getStyleClass().add("bg-overdue");
            btn.setOnAction(e -> taskService.deleteRegularTemplate(task.getId(), selectedDate));
        } else {
            if (task.isCompleted()) {
                btn.setText("Готово");
                btn.getStyleClass().add("bg-done");
            } else if (task.getDate().isBefore(LocalDate.now())) {
                btn.setText("Просрочено");
                btn.getStyleClass().add("bg-overdue");
            } else {
                btn.setText("В процессе");
                btn.getStyleClass().add("bg-process");
            }
            btn.setOnAction(e -> createActionMenu().show(btn, Side.BOTTOM, 0, 0));
        }
        return btn;
    }

    private ContextMenu createActionMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem complete = new MenuItem(task.isCompleted() ? "В работу" : "Завершить");
        complete.setOnAction(e -> updateTaskData(task.getTitle(), task.getDescription(), task.getPriority(), !task.isCompleted()));
        MenuItem move = new MenuItem("На завтра");
        move.setOnAction(e -> {
            TaskUpdatePayload payload = new TaskUpdatePayload(
                    task.getId(), task.getTitle(), task.getDescription(),
                    task.getPriority(), selectedDate.plusDays(1), task.isCompleted()
            );
            taskService.updateTask(task, selectedDate, payload);
            controller.refreshTaskList();
        });
        MenuItem delete = new MenuItem("Удалить");
        delete.setOnAction(e -> {
            taskService.deleteTask(task, selectedDate);
            controller.refreshTaskList();
        });
        menu.getItems().addAll(complete, move, new SeparatorMenuItem(), delete);
        return menu;
    }

    private void finalizeTitleEdit(TextField f, Label l, int i) {
        if (!textContent.getChildren().contains(f)) return;
        String newTitle = f.getText().trim();
        textContent.getChildren().set(i, l);
        if (!newTitle.isEmpty() && !newTitle.equals(task.getTitle())) {
            updateTaskData(newTitle, task.getDescription(), task.getPriority(), task.isCompleted());
        }
    }

    private void finalizeDescEdit(TextArea a, Label l, int i) {
        if (!textContent.getChildren().contains(a)) return;
        String newDesc = a.getText().trim();
        textContent.getChildren().set(i, l);
        if (!newDesc.equals(task.getDescription())) {
            updateTaskData(task.getTitle(), newDesc, task.getPriority(), task.isCompleted());
        }
    }

    private void updateTaskData(String t, String d, int p, boolean c) {
        TaskUpdatePayload payload = new TaskUpdatePayload(task.getId(), t, d, p, task.getDate(), c);
        if (isTemplateMode) {
            taskService.updateRegularTemplate(task, payload, selectedDate);
        } else {
            taskService.updateTask(task, selectedDate, payload);
        }
        controller.refreshTaskList();
    }

    private HBox createRegularBadge() {
        HBox badge = new HBox(5);
        badge.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("\uD83D\uDD04");
        icon.getStyleClass().add("regular-icon");
        Label text = new Label("РЕГУЛЯРНАЯ");
        text.getStyleClass().add("regular-text");
        badge.getChildren().addAll(icon, text);
        return badge;
    }

    private Label createPriorityLabel() {
        Label pLabel = new Label(getPriorityText());
        pLabel.getStyleClass().addAll("p-label", getPriorityTextClass());
        return pLabel;
    }

    private String getPriorityBorderClass() {
        return switch (task.getPriority()) {
            case 3 -> "p-high";
            case 2 -> "p-medium";
            default -> "p-low";
        };
    }

    private String getPriorityTextClass() {
        return switch (task.getPriority()) {
            case 3 -> "p-text-high";
            case 2 -> "p-text-medium";
            default -> "p-text-low";
        };
    }

    private String getPriorityText() {
        return switch (task.getPriority()) {
            case 3 -> "Высокий";
            case 2 -> "Средний";
            default -> "Низкий";
        };
    }
}