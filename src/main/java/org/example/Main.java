package org.example;

import org.example.datasource.mapper.TaskMapper;
import org.example.datasource.repository.TaskRepository;
import org.example.datasource.repositoryservice.TaskRepositoryService;
import org.example.domain.model.Task;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
            TaskRepository taskRepository = new TaskRepositoryService();
            Task task = new Task("Бебра", "сделать бебру", LocalDateTime.now());
            taskRepository.save(TaskMapper.toEntity(task));
            Task task1 = TaskMapper.toDomain(taskRepository.findAll().get(0));
            System.out.println(task1.getId());
            System.out.println(task1.getTitle());
            System.out.println(task1.getDescription());
            System.out.println(task1.getDate());
        }
    }