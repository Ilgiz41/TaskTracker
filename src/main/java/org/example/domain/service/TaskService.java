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
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TaskService {

    private final EventBus eventBus;
    private final RegularTaskRepositoryService regularTaskRepositoryService;
    private final TaskRepositoryService taskRepositoryService;
    private final TaskDispatcher taskDispatcher;
    private final LockManager lockManager;
    private final LoggerService loggerService;

    private final Map<TaskId, Task> taskCache;

    public TaskService() {
        this.taskRepositoryService = TaskRepositoryUtil.getTaskRepositoryService();
        this.regularTaskRepositoryService = RegularTaskRepositoryUtil.getRegularTaskRepositoryService();
        this.eventBus = DomainServiceUtil.getEventBus();
        this.taskCache = new ConcurrentHashMap<>();
        this.lockManager = new LockManager();
        this.taskDispatcher = InfrastructureUtil.getTaskDispatcher();
        this.loggerService = InfrastructureUtil.getLoggerService();
    }

    public void createAndSave(String title, String description, LocalDate date, LocalDate selectedDate, int priority) {
        if (title.isEmpty()) throw new ValidationException("Заголовок не может быть пустым");
        if (date == null) throw new ValidationException("Выберите дату для задачи");
        if (date.isBefore(LocalDate.now())) throw new ValidationException("Дата не может быть после текущей");
        taskDispatcher.submitIo(() -> {
            SimpleTask task = new SimpleTask(title, description, date, false, priority, false, null);
            return taskRepositoryService.save(SimpleTaskMapper.toEntity(task));
        }).thenAcceptAsync(task -> {
            lockManager.writeWithLock(() -> {
                if (selectedDate.equals(task.getDate())) {
                    taskCache.put(new TaskId(task.getId(), false), SimpleTaskMapper.toDomain(task));
                    eventBus.publish(new Event.RefreshFullTaskListEvent());
                }
            });
        }, taskDispatcher.getCpuPool());
    }

    public void createAndSaveRegularTemplate(String title, String description, int priority, Set<DayOfWeek> selectedDays, LocalDate selectedDate) {
        if (title.isEmpty()) throw new ValidationException("Заголовок не может быть пустым");
        if (selectedDays.isEmpty()) throw new ValidationException("Выберите дни для повторения задачи");
        RegularTaskEntity regularTaskEntity = regularTaskRepositoryService.save(new RegularTaskEntity(title, description, priority, selectedDays, LocalDate.now()));
        RegularTask regularTask = RegularTaskMapper.toDomain(regularTaskEntity);
        if (selectedDays.contains(selectedDate.getDayOfWeek())) {
            regularTask.setDate(selectedDate);
            taskCache.put(regularTask.getId(), regularTask);
            eventBus.publish(new Event.RefreshFullTaskListEvent());
        }
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
        task.delete(this, selectedDate);
        eventBus.publish(new Event.RefreshFullTaskListEvent());
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

    public void updateRegularTask(RegularTask regularTask, LocalDate selectedDate, TaskUpdatePayload payload) {
        SimpleTask simpleTaskMaterialized = materialize(regularTask);
        simpleTaskMaterialized.setDate(payload.newDate());
        simpleTaskMaterialized.setPriority(payload.priority());
        simpleTaskMaterialized.setCompleted(payload.completed());
        simpleTaskMaterialized.setTitle(payload.title());
        simpleTaskMaterialized.setDescription(payload.description());
        regularTaskRepositoryService.addExcludedDay(regularTask.getRawId(), selectedDate);
        TaskEntity taskEntity = taskRepositoryService.save(SimpleTaskMapper.toEntity(simpleTaskMaterialized));
        addIfCorrectDate(selectedDate, SimpleTaskMapper.toDomain(taskEntity));
        taskCache.remove(regularTask.getId());
    }

    public void updateTask(Task task, LocalDate selectedDate, TaskUpdatePayload payload) {
        task.update(this, selectedDate, payload);
        eventBus.publish(new Event.RefreshFullTaskListEvent());
    }

    public void updateRegularTemplate(Task task, TaskUpdatePayload payload, LocalDate selectedDate) {
        RegularTask regularTask = new RegularTask();
        regularTask.setId(task.getId());
        regularTask.setTitle(payload.title());
        regularTask.setDescription(payload.description());
        regularTask.setPriority(payload.priority());
        regularTask.setDayOfWeeks(task.getDayOfWeeks());
        regularTask.setDate(selectedDate);
        regularTaskRepositoryService.save(RegularTaskMapper.toEntity(regularTask));
        if (taskCache.containsKey(regularTask.getId())) taskCache.put(regularTask.getId(), regularTask);
    }

    public void deleteRegularTemplate(TaskId id, LocalDate selectedDate) {
        regularTaskRepositoryService.deleteById(id.id());
        if (taskCache.containsKey(id) && taskCache.get(id).getDate().equals(selectedDate)) taskCache.remove(id);
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
        if (selectedDate.equals(task.getDate())) {
            taskCache.put(task.getId(), task);
        } else {
            taskCache.remove(task.getId());
        }
        eventBus.publish(new Event.RefreshFullTaskListEvent());
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
                    eventBus.publish(new Event.CriticalErrorExceptionEvent(ex.getCause()));
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
        return taskDispatcher.runParallel(() -> taskCache.values().stream()
                .sorted()
                .toList());
    }

    public void deleteAllTasksForDate(LocalDate selectedDate) {
        if (taskCache.isEmpty()) return;
        taskDispatcher.submitIo(() -> {
            lockManager.writeWithLock(() -> {
                taskCache.values().forEach(task -> task.delete(this, selectedDate));
                taskCache.clear();
                eventBus.publish(new Event.RefreshFullTaskListEvent());
            });
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

    public List<Task> getAllTemplates() {
        return regularTaskRepositoryService.findAll().stream()
                .map(RegularTaskMapper::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void closeDBConnection() {
        HibernateUtil.shutdown();
    }
}
