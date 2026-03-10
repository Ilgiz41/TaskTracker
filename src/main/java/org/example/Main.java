package org.example;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.example.controller.MainController;
import org.example.datasource.repositoryservice.RegularTaskRepositoryService;
import org.example.datasource.repositoryservice.TaskRepositoryService;
import org.example.domain.model.Task;
import org.example.domain.model.TaskId;
import org.example.domain.service.TaskCacheService;
import org.example.domain.service.TaskService;
import org.example.infrastructure.cache.Cache;
import org.example.infrastructure.concurrency.LockManager;
import org.example.infrastructure.concurrency.TaskDispatcher;
import org.example.infrastructure.file.FileService;
import org.example.infrastructure.logging.LoggerService;
import org.example.util.EventBus;

import java.io.IOException;
import java.time.LocalDate;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        initialize(stage);
    }

    public void initialize(Stage stage) throws IOException {
        Font reg = Font.loadFont(getClass().getResourceAsStream("/fonts/NotoSans_Condensed-Regular.ttf"), 14);
        Font med = Font.loadFont(getClass().getResourceAsStream("/fonts/NotoSans_Condensed-Medium.ttf"), 14);
        Font bold = Font.loadFont(getClass().getResourceAsStream("/fonts/NotoSans_Condensed-Bold.ttf"), 14);
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Cache<TaskId, Task> taskCache = new Cache<>(50);
        EventBus eventBus = new EventBus();
        FileService fileService = new FileService();
        LockManager lockManager = new LockManager();
        TaskDispatcher taskDispatcher = new TaskDispatcher(eventBus);
        LoggerService loggerService = new LoggerService(fileService, eventBus);

        TaskRepositoryService taskRepo = new TaskRepositoryService(eventBus);
        RegularTaskRepositoryService regRepo = new RegularTaskRepositoryService(eventBus);
        TaskCacheService taskCacheService = new TaskCacheService(taskCache);

        TaskService taskService = new TaskService(taskRepo, regRepo, eventBus, lockManager, taskDispatcher, taskCacheService);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainScene.fxml"));
        newControllerFactory(loader, taskService, eventBus);

        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.initializeEvent();
        taskService.loadTaskCacheForDate(LocalDate.now());
        controller.updateDateDisplay();

        stage.setScene(new Scene(root, 1150, 750));
        stage.show();

       // createNewTasks(taskService, 10000);
        onClose(stage, taskService);
    }

    private void newControllerFactory(FXMLLoader loader, TaskService taskService, EventBus eventBus) {
        loader.setControllerFactory(clazz -> {
            if (clazz == MainController.class) {
                return new MainController(taskService, eventBus);
            } else {
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void onClose(Stage stage, TaskService taskService) {
        stage.setOnCloseRequest(e -> {
            taskService.closeDBConnection();
            Platform.exit();
        });
    }

    public void createNewTasks(TaskService taskService, int n) {
        for (int i = 0; i < n; i++) {
            taskService.createAndSave(String.valueOf(i),
                    "Зада́ча — проблемная ситуация с явно заданной целью, которую необходимо достичь; в более узком смысле задачей также называют саму эту цель, данную в рамках проблемной ситуации, то есть то, что требуется сделать[1]. Ещё более узкое определение называет задачей ситуацию с известным начальным состоянием системы и необходимым конечным состоянием системы, причём способ достижения конечного состояния от начального известен (в отличие от проблемы, в случае которой способ достижения конечного состояния системы неизвестен).\n" +
                            "\n" +
                            "Другие определения понятия «задача» согласно международным стандартам:\n" +
                            "\n" +
                            "деятельность, необходимая для достижения некоторой цели[2];\n" +
                            "требуемые, рекомендуемые или допустимые действия, направленные на содействие достижению одного или нескольких результатов некоторого процесса[3];\n" +
                            "наименьшая единица работы, подлежащая учёту; чётко определённое рабочее задание для одного или нескольких участников проекта[4].\n" +
                            "В самом широком смысле под задачей понимается то, что нужно выполнить — задание, поручение, дело, упражнение, например логическая задача, математическая задача, шахматная задача.\n" +
                            "\n" +
                            "В отличие от функции, которая может осуществляться постоянно, задача предполагает при заданных её условиях выход на достижение конечного результата (решение задачи)." + "Зада́ча — проблемная ситуация с явно заданной целью, которую необходимо достичь; в более узком смысле задачей также называют саму эту цель, данную в рамках проблемной ситуации, то есть то, что требуется сделать[1]. Ещё более узкое определение называет задачей ситуацию с известным начальным состоянием системы и необходимым конечным состоянием системы, причём способ достижения конечного состояния от начального известен (в отличие от проблемы, в случае которой способ достижения конечного состояния системы неизвестен).\\n\" +\n" +
                            "                            \"\\n\" +\n" +
                            "                            \"Другие определения понятия «задача» согласно международным стандартам:\\n\" +\n" +
                            "                            \"\\n\" +\n" +
                            "                            \"деятельность, необходимая для достижения некоторой цели[2];\\n\" +\n" +
                            "                            \"требуемые, рекомендуемые или допустимые действия, направленные на содействие достижению одного или нескольких результатов некоторого процесса[3];\\n\" +\n" +
                            "                            \"наименьшая единица работы, подлежащая учёту; чётко определённое рабочее задание для одного или нескольких участников проекта[4].\\n\" +\n" +
                            "                            \"В самом широком смысле под задачей понимается то, что нужно выполнить — задание, поручение, дело, упражнение, например логическая задача, математическая задача, шахматная задача.\\n\" +\n" +
                            "                            \"\\n\" +\n" +
                            "                            \"В отличие от функции, которая может осуществляться постоянно, задача предполагает при заданных её условиях выход на достижение конечного результата (решение задачи).",
                    LocalDate.now(), LocalDate.now(), 2);
        }
    }
}