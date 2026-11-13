package com.endev.tutoring.mapper;

import com.endev.tutoring.entity.TutorEntity;
import com.endev.tutoring.model.Tutor;

public final class TutorMapper {

    private TutorMapper() {
    }

    public static TutorEntity toEntity(Tutor tutor) {
        TutorEntity entity = new TutorEntity();
        entity.setId(tutor.id());
        entity.setName(tutor.name());
        entity.setEmail(tutor.email());
        entity.setSubject(tutor.subject());
        entity.setHourlyRate(tutor.hourlyRate());
        return entity;
    }

    public static Tutor toTutor(TutorEntity entity) {
        return new Tutor(entity.getId(), entity.getName(), entity.getEmail(), entity.getSubject(),
                entity.getHourlyRate());
    }
}
