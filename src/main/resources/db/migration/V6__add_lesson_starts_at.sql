-- When a booked lesson starts. Optional, so lessons created before booking existed stay valid.
ALTER TABLE lesson
  ADD COLUMN starts_at timestamptz;
