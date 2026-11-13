package com.endev.tutoring.service;

import com.endev.tutoring.exception.DuplicateEmailException;
import com.endev.tutoring.exception.TutorNotFoundException;
import com.endev.tutoring.mapper.TutorMapper;
import com.endev.tutoring.model.Tutor;
import com.endev.tutoring.repository.TutorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TutorService {

    private final TutorRepository tutors;

    public TutorService(TutorRepository tutors) {
        this.tutors = tutors;
    }

    @Transactional
    public Tutor create(String name, String email, String subject, BigDecimal hourlyRate) {
        Tutor tutor = new Tutor(UUID.randomUUID(), name, email, subject, hourlyRate);
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

    @Transactional(readOnly = true)
    public Tutor find(UUID id) {
        return tutors.find(id)
                .map(TutorMapper::toTutor)
                .orElseThrow(() -> new TutorNotFoundException(id));
    }
}
