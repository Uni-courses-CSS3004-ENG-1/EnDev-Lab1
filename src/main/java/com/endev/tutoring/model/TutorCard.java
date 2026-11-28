package com.endev.tutoring.model;

// A tutor in the public catalogue, with how many lessons they have completed on the marketplace.
public record TutorCard(Tutor tutor, long lessonsTaught) {
}
