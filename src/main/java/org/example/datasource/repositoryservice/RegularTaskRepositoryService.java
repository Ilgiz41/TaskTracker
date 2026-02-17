package org.example.datasource.repositoryservice;

import org.example.datasource.model.RegularTaskEntity;
import org.example.datasource.repository.BaseRepository;
import org.hibernate.Session;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;


public class RegularTaskRepositoryService extends BaseRepository<RegularTaskEntity> {

    public RegularTaskRepositoryService() {
        super(RegularTaskEntity.class);
    }

    public List<RegularTaskEntity> findAllByDate(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return execute(session -> session.createQuery(
                        "SELECT r FROM RegularTaskEntity r " +
                                "WHERE r.startDate <= :date " +
                                "AND :dayOfWeek MEMBER OF r.dayOfWeeks " +
                                "AND :date NOT MEMBER OF r.excludedDays",
                        RegularTaskEntity.class)
                .setParameter("date", date)
                .setParameter("dayOfWeek", dayOfWeek)
                .getResultList());
    }

    public void addExcludedDay(Long id, LocalDate date, Session session) {
        RegularTaskEntity regularTaskTemplate = findById(id).orElse(null);
        if (regularTaskTemplate != null) {
            regularTaskTemplate.getExcludedDays().add(date);
            save(regularTaskTemplate, session);
        }
    }
}
