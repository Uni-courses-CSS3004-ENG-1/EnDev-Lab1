package com.endev.tutoring.service;

import com.endev.tutoring.exception.DuplicateEmailException;
import com.endev.tutoring.exception.TutorNotFoundException;
import com.endev.tutoring.mapper.TutorMapper;
import com.endev.tutoring.model.Tutor;
import com.endev.tutoring.model.TutorCard;
import com.endev.tutoring.repository.LessonRepository;
import com.endev.tutoring.repository.TutorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TutorService {

    private final TutorRepository tutors;
    private final LessonRepository lessons;

    public TutorService(TutorRepository tutors, LessonRepository lessons) {
        this.tutors = tutors;
        this.lessons = lessons;
    }

    @Transactional
    public Tutor create(String name, String email, String subject, BigDecimal hourlyRate) {
        return create(name, email, subject, hourlyRate, "");
    }

    // the sign-up form also sends a bio
    @Transactional
    public Tutor create(String name, String email, String subject, BigDecimal hourlyRate, String bio) {
        Tutor tutor = new Tutor(UUID.randomUUID(), name, email, subject, hourlyRate, bio == null ? "" : bio);
        try {
            tutors.insert(TutorMapper.toEntity(tutor));
            // flush inside the try, so a taken email is caught here and not at commit
            tutors.flush();
        } catch (DataIntegrityViolationException e) {
            if (UniqueViolation.is(e)) {
                throw new DuplicateEmailException(email);
            }
            throw e;
        }
        return tutor;
    }

    // every tutor, A to Z, with their completed-lesson count for the public catalogue
    @Transactional(readOnly = true)
    public List<TutorCard> catalogue() {
        Map<UUID, Long> taught = lessons.completedCountByTutor();
        return tutors.findAll().stream()
                .map(TutorMapper::toTutor)
                .map(tutor -> new TutorCard(tutor, taught.getOrDefault(tutor.id(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Tutor find(UUID id) {
        return tutors.find(id)
                .map(TutorMapper::toTutor)
                .orElseThrow(() -> new TutorNotFoundException(id));
    }
}
