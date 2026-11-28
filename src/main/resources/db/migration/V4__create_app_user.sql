-- Accounts that can log in. The password is a BCrypt hash ({bcrypt}$2a$10$...), never the text.
-- Each account is either a student or a tutor, and points at that profile row.
CREATE TABLE app_user (
  id             uuid PRIMARY KEY,
  username       text NOT NULL UNIQUE,
  password_hash  text NOT NULL,
  role           text NOT NULL,
  student_id     uuid REFERENCES student (id),
  tutor_id       uuid REFERENCES tutor (id),
  created_at     timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT app_user_role_known CHECK (role IN ('STUDENT', 'TUTOR')),
  -- a student account links a student row, a tutor account links a tutor row
  CONSTRAINT app_user_profile_matches_role CHECK (
    (role = 'STUDENT' AND student_id IS NOT NULL AND tutor_id IS NULL) OR
    (role = 'TUTOR'   AND tutor_id IS NOT NULL AND student_id IS NULL))
);
