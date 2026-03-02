package org.example;

import javafx.application.Application;
import javafx.application.Platform;
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
        //createNewTasks(taskService, 100);
        taskService.loadTaskCacheForDate(LocalDate.now());
        mainController.refreshTaskList();
        mainController.updateDateDisplay();
        onClose(stage, taskService);
    }

    public void onClose(Stage stage, TaskService taskService) {
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