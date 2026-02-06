package org.example.datasource.repositoryservice;

import org.example.datasource.model.RegularTaskEntity;
import org.example.datasource.repository.BaseRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;


public class RegularTaskRepositoryService extends BaseRepository<RegularTaskEntity> {

    public RegularTaskRepositoryService() {
        super(RegularTaskEntity.class);
    }

    public List<RegularTaskEntity> findAllByDate(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return execute(session -> session.createQuery("SELECT t FROM  RegularTaskEntity t " +
                        "JOIN t.dayOfWeeks d LEFT JOIN t.excludedDays ed ON ed =:date WHERE d =:dayOfWeek AND ed IS NULL")
                .setParameter("date", date)
                .setParameter("dayOfWeek", dayOfWeek)
                .getResultList());
    }

    public void addExcludedDay(Long id, LocalDate date) {
        execute(session -> {
            RegularTaskEntity regularTaskTemplate = findById(id).orElse(null);
            if (regularTaskTemplate != null) {
                regularTaskTemplate.getExcludedDays().add(date);
                save(regularTaskTemplate);
            }
            return null;
        });
    }
}
