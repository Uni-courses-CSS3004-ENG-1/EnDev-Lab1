## Product

We build a tutoring marketplace. Students and tutors track lessons.

A student finds a tutor, books a time slot and pays through the platform. The platform
holds the payment until the lesson has taken place, then pays the tutor and keeps a
commission on every lesson. Money moves each time a lesson changes status, so the order
of those changes is enforced in code rather than left to whoever edits the record.

Java 21 and PostgreSQL. Start the database with `docker compose up -d`, run the tests with
`mvn -q verify` and start the app with `mvn spring-boot:run` (see [Run it](#run-it)).

## Core item

The core item is a **lesson**: one student, one tutor, one time slot.

It is identified by `LessonId` (for example `LES-2026-0042`), which rejects a null or
blank value. A lesson has exactly one `LessonStatus` at a time. `LessonStatus` is an
enum with four values:

- `REQUESTED`: the student asked the tutor for a slot; no money has moved
- `CONFIRMED`: the tutor accepted; the platform holds the student's payment
- `COMPLETED`: the lesson took place; the tutor is paid and the platform keeps its commission (final)
- `CANCELLED`: called off before it took place; any held payment goes back to the student (final)

`LessonPolicy.move(from, to)` returns the new status when the change is allowed and
throws `IllegalStateException` when it is forbidden.

## Status table

| From | To | Result | Reason |
| --- | --- | --- | --- |
| `REQUESTED` | `CONFIRMED` | Allowed | The tutor accepted the slot, so the student's payment is taken and held |
| `REQUESTED` | `CANCELLED` | Allowed | Nobody paid yet, so nothing has to be refunded |
| `CONFIRMED` | `COMPLETED` | Allowed | The lesson took place, so the held payment goes to the tutor minus the commission |
| `CONFIRMED` | `CANCELLED` | Allowed | Called off before it took place, so the held payment goes back to the student |
| `REQUESTED` | `COMPLETED` | Forbidden | Nobody paid, so there is no payment to release and no commission to take |
| `COMPLETED` | `CANCELLED` | Forbidden | The tutor is already paid, so a cancellation would refund money the platform no longer holds |

Any move not in the table is forbidden too: nothing leaves `COMPLETED` or `CANCELLED`,
and a lesson never moves backwards.

## Forbidden — why

**`REQUESTED` → `COMPLETED`.** A requested lesson has not been accepted by the tutor,
and the student has not paid for it. Marking it completed would tell the platform to pay
the tutor out of a payment that was never taken, so the platform would pay from its own
pocket and the commission report would count income that never arrived. It would also
let a tutor get paid for a slot they never agreed to. The lesson has to pass through
`CONFIRMED`, where the payment is held, before it can be completed.

**`COMPLETED` → `CANCELLED`.** Completing a lesson pays the tutor and books the
platform's commission. Cancelling it afterwards would refund the student with money
that has already been paid out, and the lesson would disappear from the tutor's
earnings and from the commission the platform reports. If a student disputes a lesson
that took place, that is a separate refund case. The record that the lesson happened
stays as it is.

## Package diagram

The code sits in six packages. Arrows point inward: the outer packages use `domain`,
and `domain` uses none of them. **`domain` imports no Spring and no `java.sql`.**

```
  dto                  client               handler               persistence      config
  VendorLessonPayload  LoggingPaymentClient LessonService         LessonJdbc       Application
  (vendor JSON →       (PaymentPort,        (@Service: Rule +     (@Repository,    RuleConfig
   LessonStatus)        prints only)         PaymentPort)          JdbcTemplate)   (@Bean: LessonPolicy)
                                            LessonRegistration         ^
                                            (@Service,                 |
                                             @Transactional) ----------+
                                            LessonDemoRunner
       \                    |                     |                    |              /
        v                   v                     v                    v             v
  +-----------------------------------------------------------------------------------------+
  |                                        domain                                           |
  |      LessonId     LessonStatus     LessonPolicy     PaymentPort     DuplicateLesson       |
  |      Rule  ←  TransitionRule, UnpaidCannotComplete, LessonPolicy                         |
  |                              (no Spring, no java.sql)                                   |
  +-----------------------------------------------------------------------------------------+
```

## How Spring is joined

- `Rule` has one method, `check(from, to)`, and throws `IllegalStateException` when a
  move is forbidden. Two plain-Java classes implement it:
  - `TransitionRule`: the status table above
  - `UnpaidCannotComplete`: the stop-factor. A lesson cannot be completed before the
    platform holds the payment
- `LessonPolicy` is also a `Rule`: it runs a list of rules in order and rejects a
  missing status.
- `RuleConfig` (in `config`) has one `@Bean` that builds a `LessonPolicy` from
  `UnpaidCannotComplete` and then `TransitionRule`. This is the only place the order is set.
- `LessonService` (in `handler`) is a `@Service`. Spring passes it that `Rule` and a
  `PaymentPort` through the constructor. `move(from, to)` calls `rules.check(from, to)` and
  returns `to`. `move(lesson, from, to)` does the same and then moves the money: `CONFIRMED`
  holds the payment, `COMPLETED` releases it to the tutor, `CONFIRMED → CANCELLED` refunds it.
- `PaymentPort` (in `domain`) is the outbound port to the payment provider.
  `LoggingPaymentClient` (in `client`) stands in for it and only prints. In
  `LessonServicePaymentTest` the port is a Mockito mock, so no test calls a real endpoint.
- `VendorLessonPayload` (in `dto`) turns the vendor's status names (`PENDING`, `BOOKED`,
  `FINISHED`, `CANCELED`) into a `LessonStatus` and rejects anything else.
- `LessonDemoRunner` (in `handler`) runs once on start-up and walks one lesson through
  `LessonService`, so you can watch the rules work.

## Storage

One table, written by hand in `src/main/resources/db/schema.sql`. Nothing generates it
(no JPA, no Hibernate, no Flyway).

```sql
CREATE TABLE lesson (
  id            uuid PRIMARY KEY,
  business_key  text NOT NULL UNIQUE,
  status        text NOT NULL,
  subject       text NOT NULL,
  created_at    timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT lesson_status_known
    CHECK (status IN ('REQUESTED', 'CONFIRMED', 'COMPLETED', 'CANCELLED'))
);
```

- `id` is the surrogate key. `business_key` is the `LessonId` people read (`LES-2026-0042`).
  It is `UNIQUE` and `NOT NULL`, but it is not the primary key, so a change to its format
  never touches a foreign key. `LessonId` rejects blank values before they reach the database.
- The `CHECK` lists the same four statuses as the table above.
- `LessonJdbc` (in `persistence`) is the only class that writes SQL. It runs the `INSERT`,
  the `count(*)` and a status read through `JdbcTemplate` with `?` placeholders, and maps
  `LessonStatus` to and from the `text` column. An unknown text from the database throws.
  It does not become a new status.
- `LessonRegistration` (in `handler`) is the `@Service` that holds `@Transactional`:
  - `register(id, lesson, subject)` is one transaction per call. A second lesson with the
    same business key throws `DuplicateLesson`, and the first one stays (count 1).
  - `insertTwice(lesson)` is one transaction with two inserts of the same key. The second
    fails, so the first is rolled back too (count 0).
  - Both catch Spring's `DuplicateKeyException` (SQLState `23505`, a
    `DataIntegrityViolationException`) and throw `DuplicateLesson`. That exception lives
    in `domain` and is unchecked, so the transaction rolls back.
- The tests apply `schema.sql` to a clean table themselves. None of them is
  `@Transactional`, so every count comes from a separate query that sees only committed rows.

## Run it

The tests need the classroom PostgreSQL (`localhost:5432/css`, user `css`, password `css`,
set in `application.yml`). `compose.yaml` runs only that database:

```
docker compose up -d
mvn -q verify
mvn spring-boot:run
docker compose down
```

`mvn spring-boot:run` prints:

```
Lesson LES-2026-0042 arrived from the vendor as REQUESTED
  REQUESTED -> COMPLETED: forbidden (An unpaid lesson cannot be completed)
  payment: hold for LES-2026-0042
  REQUESTED -> CONFIRMED: allowed
  payment: release to the tutor for LES-2026-0042
  CONFIRMED -> COMPLETED: allowed
  COMPLETED -> CANCELLED: forbidden (Cannot move a lesson from COMPLETED to CANCELLED)
Lesson LES-2026-0042 ends as COMPLETED
```
