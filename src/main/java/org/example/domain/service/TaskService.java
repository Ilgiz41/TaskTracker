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
import org.example.util.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TaskService {

    private final EventBus eventBus;
    private final RegularTaskRepositoryService regularTaskRepositoryService;
    private final TaskRepositoryService taskRepositoryService;

    private final Map<TaskId, Task> taskCache;

    public TaskService() {
        this.taskRepositoryService = TaskRepositoryUtil.getTaskRepositoryService();
        this.regularTaskRepositoryService = RegularTaskRepositoryUtil.getRegularTaskRepositoryService();
        this.eventBus = DomainServiceUtil.getEventBus();
        this.taskCache = new ConcurrentHashMap<>();
    }

    public void createAndSave(String title, String description, LocalDate date, LocalDate selectedDate, int priority) {
        if (title.isEmpty()) throw new ValidationException("Заголовок не может быть пустым");
        if (date == null) throw new ValidationException("Выберите дату для задачи");
        if (date.isBefore(LocalDate.now())) throw new ValidationException("Дата не может быть после текущей");
        executeInTransaction(session -> {
            SimpleTask task = new SimpleTask(title, description, date, false, priority, false, null);
            TaskEntity taskEntity = taskRepositoryService.save(SimpleTaskMapper.toEntity(task), session);
            if (selectedDate.equals(taskEntity.getDate())) {
                taskCache.put(new TaskId(taskEntity.getId(), false), SimpleTaskMapper.toDomain(taskEntity));
                eventBus.publish(new Event.TaskCacheChanged());
            }
        });
    }

    public void createAndSaveRegularTemplate(String title, String description, int priority, Set<DayOfWeek> selectedDays, LocalDate selectedDate) {
        if (title.isEmpty()) throw new ValidationException("Заголовок не может быть пустым");
        if (selectedDays.isEmpty()) throw new ValidationException("Выберите дни для повторения задачи");
        executeInTransaction(session -> {
            RegularTaskEntity regularTaskEntity = regularTaskRepositoryService.save(new RegularTaskEntity(title, description, priority, selectedDays, LocalDate.now()), session);
            RegularTask regularTask = RegularTaskMapper.toDomain(regularTaskEntity);
            if (selectedDays.contains(selectedDate.getDayOfWeek())) {
                regularTask.setDate(selectedDate);
                taskCache.put(regularTask.getId(), regularTask);
                eventBus.publish(new Event.TaskCacheChanged());
            }
        });
    }

    public void deleteSimpleTask(SimpleTask simpleTask) {
        executeInTransaction(session -> {
            taskRepositoryService.deleteById(simpleTask.getRawId(), session);
            taskCache.remove(simpleTask.getId());
        });
    }

    public void deleteRegularTask(RegularTask regularTask, LocalDate selectedDate) {
        executeInTransaction(session -> {
            regularTaskRepositoryService.addExcludedDay(regularTask.getRawId(), selectedDate, session);
            taskCache.remove(regularTask.getId());
        });
    }

    public void deleteTask(Task task, LocalDate selectedDate) {
        task.delete(this, selectedDate);
        eventBus.publish(new Event.TaskCacheChanged());
    }

    public void updateSimpleTask(SimpleTask simpleTask, LocalDate selectedDate, TaskUpdatePayload payload) {
        simpleTask.setTitle(payload.title());
        simpleTask.setDescription(payload.description());
        simpleTask.setPriority(payload.priority());
        simpleTask.setCompleted(payload.completed());
        simpleTask.setDate(payload.newDate());
        executeInTransaction(session -> {
            taskRepositoryService.save(SimpleTaskMapper.toEntity(simpleTask), session);
            addIfCorrectDate(selectedDate, simpleTask);
        });
    }

    public void updateRegularTask(RegularTask regularTask, LocalDate selectedDate, TaskUpdatePayload payload) {
        SimpleTask simpleTaskMaterialized = materialize(regularTask);
        simpleTaskMaterialized.setDate(payload.newDate());
        simpleTaskMaterialized.setPriority(payload.priority());
        simpleTaskMaterialized.setCompleted(payload.completed());
        simpleTaskMaterialized.setTitle(payload.title());
        simpleTaskMaterialized.setDescription(payload.description());
        executeInTransaction(session -> {
            regularTaskRepositoryService.addExcludedDay(regularTask.getRawId(), selectedDate, session);
            TaskEntity taskEntity = taskRepositoryService.save(SimpleTaskMapper.toEntity(simpleTaskMaterialized), session);
            addIfCorrectDate(selectedDate, SimpleTaskMapper.toDomain(taskEntity));
            taskCache.remove(regularTask.getId());
        });
    }

    public void updateTask(Task task, LocalDate selectedDate, TaskUpdatePayload payload) {
        task.update(this, selectedDate, payload);
        eventBus.publish(new Event.TaskCacheChanged());
    }

    public void updateRegularTemplate(Task task, TaskUpdatePayload payload, LocalDate selectedDate) {
        RegularTask regularTask = new RegularTask();
        regularTask.setId(task.getId());
        regularTask.setTitle(payload.title());
        regularTask.setDescription(payload.description());
        regularTask.setPriority(payload.priority());
        regularTask.setDayOfWeeks(task.getDayOfWeeks());
        regularTask.setDate(selectedDate);
        executeInTransaction(session -> {
            regularTaskRepositoryService.save(RegularTaskMapper.toEntity(regularTask), session);
            if (taskCache.containsKey(regularTask.getId())) taskCache.put(regularTask.getId(), regularTask);
        });
    }

    public void deleteRegularTemplate(TaskId id, LocalDate selectedDate) {
        executeInTransaction(session -> {
            regularTaskRepositoryService.deleteById(id.id(), session);
            if (taskCache.containsKey(id) && taskCache.get(id).getDate().equals(selectedDate)) taskCache.remove(id);
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
        if (selectedDate.equals(task.getDate())) {
            taskCache.put(task.getId(), task);
        } else {
            taskCache.remove(task.getId());
        }
        eventBus.publish(new Event.TaskCacheChanged());
    }

    public void loadTaskCacheForDate(LocalDate date) {
        List<SimpleTask> tasks = getSimpleTaskFromRepository(date);
        List<RegularTask> regularTasks = getRegularTaskFromRepository(date);
        Set<Long> tasksTemplateId = tasks.stream().
                map(SimpleTask::getTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!taskCache.isEmpty()) taskCache.clear();
        tasks.forEach(task -> taskCache.put(task.getId(), task));
        regularTasks.stream()
                .filter(regularTask -> !tasksTemplateId.contains(regularTask.getRawId()))
                .forEach(regularTask -> {
                    regularTask.setDate(date);
                    taskCache.put(regularTask.getId(), regularTask);
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

    public List<Task> getSortedTaskByPriority() {
        return taskCache.values().stream()
                .sorted()
                .toList();
    }

    public void deleteAllTasksForDate(LocalDate selectedDate) {
        if (taskCache.isEmpty()) return;
        executeInTransaction(session -> {
            taskCache.values().forEach(task -> task.delete(this, selectedDate));
            taskCache.clear();
            eventBus.publish(new Event.TaskCacheChanged());
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

    public void executeInTransaction(Consumer<Session> consumer) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.getTransaction();
            boolean isActive = transaction.isActive();
            try {
                if (!isActive) {
                    transaction = session.beginTransaction();
                }
                consumer.accept(session);
                if (!isActive) {
                    transaction.commit();
                }
            } catch (Exception e) {
                if (transaction.isActive()) transaction.rollback();
                throw e;
            }
        }
    }

    public void closeDBConnection() {
        HibernateUtil.shutdown();
    }
}
