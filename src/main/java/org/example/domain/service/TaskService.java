package org.example.domain.service;

import org.example.datasource.mapper.RegularTaskMapper;
import org.example.datasource.mapper.SimpleTaskMapper;
import org.example.datasource.model.RegularTaskEntity;
import org.example.datasource.model.TaskEntity;
import org.example.datasource.repositoryservice.RegularTaskRepositoryService;
import org.example.datasource.repositoryservice.TaskRepositoryService;
import org.example.domain.model.*;
import org.example.event.Event;
import org.example.exceptions.ValidationException;
import org.example.infrastructure.concurrency.LockManager;
import org.example.infrastructure.concurrency.TaskDispatcher;
import org.example.util.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TaskService {

    private final EventBus eventBus;
    private final RegularTaskRepositoryService regularTaskRepositoryService;
    private final TaskRepositoryService taskRepositoryService;
    private final TaskDispatcher taskDispatcher;
    private final LockManager lockManager;
    private final TaskCacheService taskCacheService;

    public TaskService(TaskRepositoryService taskRepositoryService, RegularTaskRepositoryService regularTaskRepositoryService, EventBus eventBus, LockManager lockManager, TaskDispatcher taskDispatcher, TaskCacheService taskCacheService) {
        this.taskRepositoryService = taskRepositoryService;
        this.regularTaskRepositoryService = regularTaskRepositoryService;
        this.eventBus = eventBus;
        this.lockManager = lockManager;
        this.taskDispatcher = taskDispatcher;
        this.taskCacheService = taskCacheService;
    }

    public void createAndSave(String title, String description, LocalDate date, LocalDate selectedDate, int priority) {
        if (title.isEmpty()) throw new ValidationException("Заголовок не может быть пустым");
        if (date == null) throw new ValidationException("Выберите дату для задачи");
        if (date.isBefore(LocalDate.now())) throw new ValidationException("Невозможно создать задачу до текущей даты");
        taskDispatcher.submitIo(() -> taskRepositoryService.save(new TaskEntity(null, title, description, date, false, priority, false, null)))
                .thenAcceptAsync(task -> {
                    taskCacheService.addIfCorrectDate(selectedDate, SimpleTaskMapper.toDomain(task));
                }, taskDispatcher.getCpuPool())
                .thenRun(() -> eventBus.publish(new Event.RefreshFullTaskListEvent()))
                .exceptionally(e -> {
                    eventBus.publish(new Event.CriticalErrorExceptionEvent(e, "Не удалось создать задачу"));
                    return null;
                });
    }

    public void createAndSaveRegularTemplate(String title, String description, int priority, Set<DayOfWeek> selectedDays, LocalDate selectedDate) {
        if (title.isEmpty()) throw new ValidationException("Заголовок не может быть пустым");
        if (selectedDays.isEmpty()) throw new ValidationException("Выберите дни для повторения задачи");
        taskDispatcher.submitIo(() -> regularTaskRepositoryService.save(new RegularTaskEntity(title, description, priority, selectedDays, LocalDate.now()))
                ).thenAcceptAsync(regularTask -> {
                    taskCacheService.addIfCorrectDayOfWeeks(selectedDate, RegularTaskMapper.toDomain(regularTask));
                }, taskDispatcher.getCpuPool())
                .thenRun(() -> eventBus.publish(new Event.RefreshFullTaskListEvent()))
                .exceptionally(e -> {
                    eventBus.publish(new Event.CriticalErrorExceptionEvent(e, "Не удалось создать регулярную задачу"));
                    return null;
                });
    }

    public void deleteSimpleTask(SimpleTask simpleTask) {
        taskRepositoryService.deleteById(simpleTask.getRawId());
        taskCacheService.remove(simpleTask);
    }

    public void deleteRegularTask(RegularTask regularTask, LocalDate selectedDate) {
        regularTaskRepositoryService.addExcludedDay(regularTask.getRawId(), selectedDate);
        taskCacheService.remove(regularTask);
    }

    public void deleteTask(Task task, LocalDate selectedDate) {
        taskDispatcher.submitIo(() -> {
                    task.delete(this, selectedDate);
                    return null;
                }).thenRun(() -> eventBus.publish(new Event.RefreshFullTaskListEvent()))
                .exceptionally(e -> {
                    eventBus.publish(new Event.CriticalErrorExceptionEvent(e.getCause(), "Не удалось удалить выбранную задачу"));
                    return null;
                });
    }

    public void updateSimpleTask(SimpleTask simpleTask, LocalDate selectedDate, TaskUpdatePayload payload) {
        simpleTask.setTitle(payload.title());
        simpleTask.setDescription(payload.description());
        simpleTask.setPriority(payload.priority());
        simpleTask.setCompleted(payload.completed());
        simpleTask.setDate(payload.newDate());
        taskRepositoryService.save(SimpleTaskMapper.toEntity(simpleTask));
        taskCacheService.addIfCorrectDate(selectedDate, simpleTask);
    }

    public void updateTask(Task task, LocalDate selectedDate, TaskUpdatePayload payload) {
        taskDispatcher.submitIo(() -> {
                    task.update(this, selectedDate, payload);
                    return null;
                })
                .thenRun(() -> eventBus.publish(new Event.RefreshFullTaskListEvent()))
                .exceptionally((e) -> {
                    eventBus.publish(new Event.CriticalErrorExceptionEvent(e, "Не удалось обновить задачу"));
                    return null;
                });
    }

    public void updateRegularTask(RegularTask regularTask, LocalDate selectedDate, TaskUpdatePayload payload) {
        SimpleTask simpleTaskMaterialized = materialize(regularTask);
        simpleTaskMaterialized.setDate(payload.newDate());
        simpleTaskMaterialized.setPriority(payload.priority());
        simpleTaskMaterialized.setCompleted(payload.completed());
        simpleTaskMaterialized.setTitle(payload.title());
        simpleTaskMaterialized.setDescription(payload.description());
        regularTaskRepositoryService.addExcludedDay(regularTask.getRawId(), selectedDate);
        TaskEntity taskEntity = taskRepositoryService.save(SimpleTaskMapper.toEntity(simpleTaskMaterialized));
        lockManager.writeWithLock(() -> {
            taskCacheService.remove(regularTask);
            SimpleTask simpleTask = SimpleTaskMapper.toDomain(taskEntity);
            if (selectedDate.equals(simpleTask.getDate())) {
                taskCacheService.put(simpleTask);
            }
        });
    }

//    public void updateRegularTemplate(Task task, TaskUpdatePayload payload, LocalDate selectedDate) {
//        RegularTask regularTask = new RegularTask();
//        regularTask.setId(task.getId());
//        regularTask.setTitle(payload.title());
//        regularTask.setDescription(payload.description());
//        regularTask.setPriority(payload.priority());
//        regularTask.setDayOfWeeks(task.getDayOfWeeks());
//        regularTask.setDate(selectedDate);
//        taskDispatcher.submitIo(() -> {
//                    regularTaskRepositoryService.save(RegularTaskMapper.toEntity(regularTask));
//                    return null;
//                }).thenRunAsync(() -> {
//                    taskCacheService.addIfCorrectDayOfWeeks(selectedDate, regularTask);
//                }, taskDispatcher.getCpuPool())
//                .thenRun(() -> eventBus.publish(new Event.RefreshFullTaskListEvent()))
//                .exceptionally(e -> {
//                    eventBus.publish(new Event.CriticalErrorExceptionEvent(e, "Не удалось обновить шаблон регулярной задачи"));
//                    return null;
//                });
//    }

//    public void deleteRegularTemplate(TaskId id, LocalDate selectedDate) {
//        taskDispatcher.submitIo(() -> {
//            regularTaskRepositoryService.deleteById(id.id());
//            lockManager.writeWithLock(() -> {
//                if (taskCache.containsKey(id) && taskCache.get(id).getDate().equals(selectedDate)) taskCache.remove(id);
//            });
//            return null;
//        });
//    }

    private SimpleTask materialize(RegularTask regularTask) {
        SimpleTask simpleTask = new SimpleTask();
        simpleTask.setTitle(regularTask.getTitle());
        simpleTask.setDescription(regularTask.getDescription());
        simpleTask.setPriority(regularTask.getPriority());
        simpleTask.setTemplateId(regularTask.getRawId());
        simpleTask.setRegular(true);
        return simpleTask;
    }

    public void loadTaskCacheForDate(LocalDate date) {
        taskDispatcher.submitIo(() -> {
                    List<SimpleTask> tasks = getSimpleTaskFromRepository(date);
                    List<RegularTask> regularTasks = getRegularTaskFromRepository(date);
                    return new DataPair<>(tasks, regularTasks);

                }).thenAcceptAsync(data -> {
                    lockManager.writeWithLock(() -> {
                        taskCacheService.loadCacheForDate(date, data);
                    });
                }, taskDispatcher.getCpuPool())
                .thenRun(() -> eventBus.publish(new Event.RefreshFullTaskListEvent()))
                .exceptionally(ex -> {
                    eventBus.publish(new Event.CriticalErrorExceptionEvent(ex.getCause(), "Не удалось загрузить задачи для выбранного дня"));
                    return null;
                });
    }

    private List<RegularTask> getRegularTaskFromRepository(LocalDate date) {
        return regularTaskRepositoryService.findAllByDate(date).stream()
                .map(RegularTaskMapper::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<SimpleTask> getSimpleTaskFromRepository(LocalDate date) {
        return taskRepositoryService.findAllByDate(date).stream()
                .map(SimpleTaskMapper::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public CompletableFuture<List<Task>> getSortedTaskByPriority() {
        return taskDispatcher.submitCpu(() ->
                taskCacheService.getAll().stream()
                        .sorted()
                        .toList());
    }

    public void clearSelectedDay(LocalDate selectedDate) {
        taskDispatcher.submitIo(() -> {
                    taskRepositoryService.deleteAllByDate(selectedDate);
                    regularTaskRepositoryService.excludeAllActiveTemplatesForDate(selectedDate);
                    return null;
                }).thenAcceptAsync(o -> {
                    lockManager.writeWithLock(() -> {
                        taskCacheService.clearForDate(selectedDate);
                    });
                }, taskDispatcher.getCpuPool())
                .thenRun(() -> {
                    eventBus.publish(new Event.RefreshFullTaskListEvent());
                    eventBus.publish(new Event.UserNotificationEvent.NotificationEvent("Задачи на " + selectedDate + " удалены"));
                })
                .exceptionally(ex -> {
                    eventBus.publish(new Event.CriticalErrorExceptionEvent(ex.getCause(), "Не удалось очистить задачи на выбранный день"));
                    return null;
                });
    }

    public List<Task> dirtySearch(String query) {
        return taskCacheService.getAll().stream()
                .filter(task -> task.getTitle().toLowerCase().contains(query.toLowerCase()))
                .toList();
    }

    public CompletableFuture<List<Task>> getAllTemplates() {
        return taskDispatcher.submitCpu(() ->
                regularTaskRepositoryService.findAll().stream()
                        .map(RegularTaskMapper::toDomain)
                        .collect(Collectors.toCollection(ArrayList::new)));
    }

    public void closeDBConnection() {
        HibernateUtil.shutdown();
    }
}
