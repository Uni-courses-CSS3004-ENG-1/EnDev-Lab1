-- A short line a tutor writes about themselves, shown on the tutor card.
-- Existing tutors get an empty bio from the default.
ALTER TABLE tutor
  ADD COLUMN bio text NOT NULL DEFAULT '';
