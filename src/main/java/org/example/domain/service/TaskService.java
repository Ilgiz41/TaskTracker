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
import org.example.infrastructure.logging.LoggerService;
import org.example.util.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TaskService {

    private final EventBus eventBus;
    private final RegularTaskRepositoryService regularTaskRepositoryService;
    private final TaskRepositoryService taskRepositoryService;
    private final TaskDispatcher taskDispatcher;
    private final LockManager lockManager;

    private final Map<TaskId, Task> taskCache;

    public TaskService(TaskRepositoryService taskRepositoryService, RegularTaskRepositoryService regularTaskRepositoryService, EventBus eventBus, LockManager lockManager, TaskDispatcher taskDispatcher) {
        this.taskRepositoryService = taskRepositoryService;
        this.regularTaskRepositoryService = regularTaskRepositoryService;
        this.eventBus = eventBus;
        this.taskCache = new ConcurrentHashMap<>();
        this.lockManager = lockManager;
        this.taskDispatcher = taskDispatcher;
    }

    public void createAndSave(String title, String description, LocalDate date, LocalDate selectedDate, int priority) {
        if (title.isEmpty()) throw new ValidationException("Заголовок не может быть пустым");
        if (date == null) throw new ValidationException("Выберите дату для задачи");
        if (date.isBefore(LocalDate.now())) throw new ValidationException("Невозможно создать задачу до текущей даты");
        taskDispatcher.submitIo(() -> {
                    return taskRepositoryService.save(new TaskEntity(null, title, description, date, false, priority, false, null));
                }).thenAcceptAsync(task -> {
                    taskCache.compute(new TaskId(task.getId(), false), (id, existingTask) -> {
                        if (selectedDate.equals(task.getDate())) {
                            return SimpleTaskMapper.toDomain(task);
                        }
                        return existingTask;
                    });
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
        taskDispatcher.submitIo(() -> {
                    return regularTaskRepositoryService.save(new RegularTaskEntity(title, description, priority, selectedDays, LocalDate.now()));
                }).thenAcceptAsync(regularTask -> {
                    taskCache.compute(new TaskId(regularTask.getId(), true), (id, existingTask) -> {
                        if (regularTask.getDayOfWeeks().contains(selectedDate.getDayOfWeek())) {
                            RegularTask rTask = RegularTaskMapper.toDomain(regularTask);
                            rTask.setDate(selectedDate);
                            return rTask;
                        }
                        return existingTask;
                    });
                }, taskDispatcher.getCpuPool())
                .thenRun(() -> eventBus.publish(new Event.RefreshFullTaskListEvent()))
                .exceptionally(e -> {
                    eventBus.publish(new Event.CriticalErrorExceptionEvent(e, "Не удалось создать регулярную задачу"));
                    return null;
                });
    }

    public void deleteSimpleTask(SimpleTask simpleTask) {
        taskRepositoryService.deleteById(simpleTask.getRawId());
        taskCache.remove(simpleTask.getId());
    }

    public void deleteRegularTask(RegularTask regularTask, LocalDate selectedDate) {
        regularTaskRepositoryService.addExcludedDay(regularTask.getRawId(), selectedDate);
        taskCache.remove(regularTask.getId());
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
        addIfCorrectDate(selectedDate, simpleTask);
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
            taskCache.remove(regularTask.getId());
            SimpleTask simpleTask = SimpleTaskMapper.toDomain(taskEntity);
            if (selectedDate.equals(simpleTask.getDate())) {
                taskCache.put(simpleTask.getId(), simpleTask);
            }
        });
    }

    public void updateRegularTemplate(Task task, TaskUpdatePayload payload, LocalDate selectedDate) {
        RegularTask regularTask = new RegularTask();
        regularTask.setId(task.getId());
        regularTask.setTitle(payload.title());
        regularTask.setDescription(payload.description());
        regularTask.setPriority(payload.priority());
        regularTask.setDayOfWeeks(task.getDayOfWeeks());
        regularTask.setDate(selectedDate);
        taskDispatcher.submitIo(() -> {
            regularTaskRepositoryService.save(RegularTaskMapper.toEntity(regularTask));
            return null;
        }).thenRunAsync(() -> {
            taskCache.compute(regularTask.getId(), (taskId, existingTask) -> {
                if (regularTask.getDayOfWeeks().contains(selectedDate.getDayOfWeek())) {
                    return regularTask;
                }
                return null;
            });
        }, taskDispatcher.getCpuPool())
                .thenRun(() -> eventBus.publish(new Event.RefreshFullTaskListEvent()))
                .exceptionally(e -> {
                   eventBus.publish(new Event.CriticalErrorExceptionEvent(e, "Не удалось обновить шаблон регулярной задачи"));
                   return null;
                });
    }

    public void deleteRegularTemplate(TaskId id, LocalDate selectedDate) {
        taskDispatcher.submitIo(() -> {
            regularTaskRepositoryService.deleteById(id.id());
            lockManager.writeWithLock(() -> {
                if (taskCache.containsKey(id) && taskCache.get(id).getDate().equals(selectedDate)) taskCache.remove(id);
            });
            return null;
        });
    }

    private SimpleTask materialize(RegularTask regularTask) {
        SimpleTask simpleTask = new SimpleTask();
        simpleTask.setTitle(regularTask.getTitle());
        simpleTask.setDescription(regularTask.getDescription());
        simpleTask.setPriority(regularTask.getPriority());
        simpleTask.setTemplateId(regularTask.getRawId());
        simpleTask.setRegular(true);
        return simpleTask;
    }

    private void addIfCorrectDate(LocalDate selectedDate, Task task) {
        taskCache.computeIfPresent(task.getId(), (id, existingTask) -> {
            if (task.getDate().equals(selectedDate)) {
                return task;
            }
            return null;
        });
    }

    public void loadTaskCacheForDate(LocalDate date) {
        taskDispatcher.submitIo(() -> {
                    List<SimpleTask> tasks = getSimpleTaskFromRepository(date);
                    List<RegularTask> regularTasks = getRegularTaskFromRepository(date);
                    return new DataPair<>(tasks, regularTasks);

                }).thenAcceptAsync(data -> {
                    lockManager.writeWithLock(() -> {
                        if (!taskCache.isEmpty()) taskCache.clear();
                        Set<Long> tasksTemplateId = data.first().stream().
                                map(SimpleTask::getTemplateId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());

                        data.first().forEach(task -> taskCache.put(task.getId(), task));

                        data.second().stream()
                                .filter(regularTask -> !tasksTemplateId.contains(regularTask.getRawId()))
                                .forEach(regularTask -> {
                                    regularTask.setDate(date);
                                    taskCache.put(regularTask.getId(), regularTask);
                                });
                        eventBus.publish(new Event.RefreshFullTaskListEvent());
                    });
                }, taskDispatcher.getCpuPool())
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
        return taskDispatcher.runParallel(() ->
                taskCache.values().stream()
                        .sorted()
                        .toList());
    }

    public void clearSelectedDay(LocalDate selectedDate) {
        taskDispatcher.submitIo(() -> {
                    taskRepositoryService.deleteAllByDate(selectedDate);
                    regularTaskRepositoryService.excludeAllActiveTemplatesForDate(selectedDate);
                    return null;
                }).thenAcceptAsync(o -> {
                    if (taskCache.isEmpty()) return;
                    lockManager.writeWithLock(() -> {
                        taskCache.values().stream()
                                .filter(task -> task.getDate().equals(selectedDate) || task.getDayOfWeeks().contains(selectedDate.getDayOfWeek()))
                                .forEach(task -> taskCache.remove(task.getId()));
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
        query = ".*" + String.join(".*", query.toLowerCase().split("")) + ".*";
        Pattern pattern = Pattern.compile(query);
        return taskCache.values().stream()
                .filter(task -> pattern.matcher(task.getTitle().toLowerCase()).matches())
                .toList();
    }

    public CompletableFuture<List<Task>> getAllTemplates() {
        return taskDispatcher.runParallel(() ->
                regularTaskRepositoryService.findAll().stream()
                        .map(RegularTaskMapper::toDomain)
                        .collect(Collectors.toCollection(ArrayList::new)));
    }

    public void closeDBConnection() {
        HibernateUtil.shutdown();
    }
}
