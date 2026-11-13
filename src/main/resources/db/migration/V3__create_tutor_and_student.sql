CREATE TABLE tutor (
  id           uuid PRIMARY KEY,
  name         text NOT NULL,
  email        text NOT NULL UNIQUE,
  subject      text NOT NULL,
  hourly_rate  numeric(10, 2) NOT NULL CHECK (hourly_rate > 0),
  created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE student (
  id          uuid PRIMARY KEY,
  name        text NOT NULL,
  email       text NOT NULL UNIQUE,
  created_at  timestamptz NOT NULL DEFAULT now()
);

-- optional links: a lesson may be requested before a tutor or student is known
ALTER TABLE lesson
  ADD COLUMN tutor_id   uuid REFERENCES tutor (id),
  ADD COLUMN student_id uuid REFERENCES student (id);
