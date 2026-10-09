-- Week 5: prove rollback by hand in psql before writing Java.
-- Run:  psql -h localhost -U css -d css -f docs/rollback-proof.sql
-- (create the table first with src/main/resources/db/schema.sql)

BEGIN;
INSERT INTO lesson (id, business_key, status, title)
VALUES ('11111111-1111-1111-1111-111111111111',
        'LES-2026-0042', 'REQUESTED', 'Algebra with Dana');
INSERT INTO lesson (id, business_key, status, title)
VALUES ('22222222-2222-2222-2222-222222222222',
        'LES-2026-0042', 'REQUESTED', 'Algebra again');
-- ERROR: duplicate key value violates unique constraint
-- The transaction is now aborted, so we ROLLBACK (not COMMIT).
ROLLBACK;

SELECT count(*) FROM lesson WHERE business_key = 'LES-2026-0042';
-- expected: 0
