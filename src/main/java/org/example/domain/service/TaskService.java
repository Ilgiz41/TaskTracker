package org.example.domain.service;

import lombok.Getter;
import org.example.datasource.mapper.RegularTaskMapper;
import org.example.datasource.mapper.SimpleTaskMapper;
import org.example.datasource.model.RegularTaskEntity;
import org.example.datasource.model.TaskEntity;
import org.example.datasource.repositoryservice.RegularTaskRepositoryService;
import org.example.datasource.repositoryservice.TaskRepositoryService;
import org.example.domain.model.*;
import org.example.exceptions.ValidationException;
import org.example.util.RegularTaskRepositoryUtil;
import org.example.util.TaskRepositoryUtil;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TaskService {

    private final RegularTaskRepositoryService regularTaskRepositoryService;
    private final TaskRepositoryService taskRepositoryService;
    @Getter
    private Map<TaskId, Task> taskCache;

    public TaskService() {
        this.taskRepositoryService = TaskRepositoryUtil.getTaskRepositoryService();
        this.regularTaskRepositoryService = RegularTaskRepositoryUtil.getRegularTaskRepositoryService();
        this.taskCache = new ConcurrentHashMap<>();
    }

    public void createAndSave(String title, String description, LocalDate date, LocalDate selectedDate, int priority) {
        if (title.isEmpty()) throw new ValidationException("Заголовок не может быть пустым");
        if (date == null) throw new ValidationException("Выберите дату для задачи");
        if (date.isBefore(LocalDate.now())) throw new ValidationException("Дата не может быть после текущей");

        SimpleTask task = new SimpleTask(title, description, date, false, priority, false, null);
        TaskEntity taskEntity = taskRepositoryService.save(SimpleTaskMapper.toEntity(task));
        if (selectedDate.equals(taskEntity.getDate())) {
            taskCache.put(new TaskId(taskEntity.getId(), false), SimpleTaskMapper.toDomain(taskEntity));
        }
    }

    public void createAndSaveRegularTemplate(String title, String description, int priority, Set<DayOfWeek> selectedDays, LocalDate selectedDate) {
        if (title.isEmpty()) throw new ValidationException("Заголовок не может быть пустым");
        if (selectedDays.isEmpty()) throw new ValidationException("Выберите дни для повторения задачи");

        RegularTaskEntity regularTaskEntity = regularTaskRepositoryService.save(new RegularTaskEntity(title, description, priority, selectedDays));
        RegularTask regularTask = RegularTaskMapper.toDomain(regularTaskEntity);
        if (selectedDays.contains(selectedDate.getDayOfWeek())) {
            regularTask.setDate(selectedDate);
            taskCache.put(regularTask.getId(), regularTask);
        }
    }

    public void deleteSimpleTask(SimpleTask simpleTask, LocalDate selectedDate) {
        taskCache.remove(simpleTask.getId());
        taskRepositoryService.deleteById(simpleTask.getRawId());
    }

    public void deleteRegularTask(RegularTask regularTask, LocalDate selectedDate) {
        taskCache.remove(regularTask.getId());
        regularTaskRepositoryService.addExcludedDay(regularTask.getRawId(), selectedDate);
    }

    public void deleteTask(Task task, LocalDate selectedDate) {
        task.delete(this, selectedDate);
    }

    public void updateSimpleTask(SimpleTask simpleTask, LocalDate selectedDate, TaskUpdatePayload payload) {
        simpleTask.setTitle(payload.title());
        simpleTask.setDescription(payload.description());
        simpleTask.setPriority(payload.priority());
        simpleTask.setCompleted(payload.completed());
        simpleTask.setDate(payload.newDate());
        addIfCorrectDate(selectedDate, simpleTask);
        taskRepositoryService.save(SimpleTaskMapper.toEntity(simpleTask));
    }

    public void updateRegularTask(RegularTask regularTask, LocalDate selectedDate, TaskUpdatePayload payload) {
        regularTaskRepositoryService.addExcludedDay(regularTask.getRawId(), selectedDate);
        taskCache.remove(regularTask.getId());
        SimpleTask simpleTaskMaterialized = materialize(regularTask);
        simpleTaskMaterialized.setDate(payload.newDate());
        simpleTaskMaterialized.setPriority(payload.priority());
        simpleTaskMaterialized.setCompleted(payload.completed());
        simpleTaskMaterialized.setTitle(payload.title());
        simpleTaskMaterialized.setDescription(payload.description());
        TaskEntity taskEntity = taskRepositoryService.save(SimpleTaskMapper.toEntity(simpleTaskMaterialized));
        addIfCorrectDate(selectedDate, SimpleTaskMapper.toDomain(taskEntity));
    }

    public void updateTask(Task task, LocalDate selectedDate, TaskUpdatePayload payload) {
        task.update(this, selectedDate, payload);
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

    private void addIfCorrectDate(LocalDate selectedDate, SimpleTask task) {
        if (selectedDate.equals(task.getDate())) {
            taskCache.put(task.getId(), task);
        } else {
            taskCache.remove(task.getId());
        }
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

    public List<Task> getSortedTaskDtoByPriority() {
        return taskCache.values().stream()
                .sorted()
                .toList();
    }

    public void deleteAllTasksForDate(LocalDate selectedDate) {
        if (taskCache.isEmpty()) return;
        taskCache.values().forEach(task -> task.delete(this, selectedDate));
        taskCache.clear();
    }

    public List<Task> dirtySearch(String query) {
        query = ".*" + String.join(".*", query.toLowerCase().split("")) + ".*";
        Pattern pattern = Pattern.compile(query);
        return taskCache.values().stream()
                .filter(task -> pattern.matcher(task.getTitle().toLowerCase()).matches())
                .toList();
    }
}
