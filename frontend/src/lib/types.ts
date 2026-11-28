// The JSON shapes the Spring Boot API sends. They mirror the records in com.endev.tutoring.dto.

export type Role = 'STUDENT' | 'TUTOR';
export type Status = 'REQUESTED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';

export type ApiError = { code: string; message: string; correlationId: string };
export type ApiResult<T> = { status: number; data: T | ApiError | null; correlationId: string };

export type TokenResponse = { accessToken: string; tokenType: 'Bearer'; expiresIn: number };
export type Me = { username: string; authorities: string[] };
export type Account = {
  username: string; role: Role; name: string; email: string;
  studentId: string | null; tutorId: string | null;
  subject: string | null; hourlyRate: number | null; bio: string | null;
};

export type TutorCard = {
  id: string; name: string; subject: string; hourlyRate: number; bio: string; lessonsTaught: number;
};

// GET /api/v1/lessons/mine and POST /book
export type LessonDetails = {
  id: string; businessKey: string; status: Status; subject: string; startsAt: string | null;
  tutorId: string | null; tutorName: string | null; studentId: string | null; studentName: string | null;
  hourlyRate: number | null;
};

// the Lab 4 shape: GET /lessons/{id}, POST /confirm, /complete, /cancel
export type Lesson = {
  id: string; businessKey: string; status: Status; subject: string; tutorId: string | null; studentId: string | null;
};
