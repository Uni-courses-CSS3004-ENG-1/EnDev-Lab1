## Product

We build a tutoring marketplace. Students and tutors track lessons.

A student finds a tutor, books a time slot and pays through the platform. The platform
holds the payment until the lesson has taken place, then pays the tutor and keeps a
commission on every lesson. Money moves each time a lesson changes status, so the order
of those changes is enforced in code rather than left to whoever edits the record.

Java 21 and PostgreSQL. Start the database with `docker compose up -d`, run the tests with
`mvn -q verify` and start the app with `mvn spring-boot:run`.

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
  dto                    client          handler              config
  VendorLessonPayload    LoggingPayment- LessonService        Application
  (vendor JSON →         Gateway         (@Service,           RuleConfig
   LessonStatus)         (stand-in,      injects Rule         (@Bean: LessonPolicy)
                          HTTP later)    and PaymentGateway)
                                         LessonRegistry
                                         (@Transactional)
                                         LessonDemoRunner

  persistence
  LessonJdbc
  (JdbcTemplate, all SQL)
       \                    |                 |                   /
        \                   |                 |                  /
         v                  v                 v                 v
  +-------------------------------------------------------------------+
  |                             domain                                |
  |          LessonId      LessonStatus      LessonPolicy             |
  |   Rule  ←  TransitionRule, UnpaidCannotComplete, LessonPolicy     |
  |   PaymentGateway (port)       DuplicateLesson                     |
  |                  (no Spring, no java.sql)                         |
  +-------------------------------------------------------------------+
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
- `LessonService` (in `handler`) is a `@Service`. Spring passes it that `Rule` through
  the constructor, and `move(from, to)` calls `rules.check(from, to)` and returns `to`.
- `VendorLessonPayload` (in `dto`) turns the vendor's status names (`PENDING`, `BOOKED`,
  `FINISHED`, `CANCELED`) into a `LessonStatus` and rejects anything else.
- `LessonDemoRunner` (in `handler`) runs once on start-up and walks one lesson through
  `LessonService`, so you can watch the rules work.

## Run it

```
mvn spring-boot:run
```

```
Lesson LES-2026-0042 arrived from the vendor as REQUESTED
  REQUESTED -> COMPLETED: forbidden (An unpaid lesson cannot be completed)
  REQUESTED -> CONFIRMED: allowed
  CONFIRMED -> COMPLETED: allowed
  COMPLETED -> CANCELLED: forbidden (Cannot move a lesson from COMPLETED to CANCELLED)
Lesson LES-2026-0042 ends as COMPLETED
```

## Database (Lab 3)

PostgreSQL, plain JDBC. No JPA, no Hibernate, no Flyway. The table is written by hand in
`src/main/resources/db/schema.sql` and nothing generates it for us.

Start only the database (classroom laptop credentials `css` / `css`, not a production secret):

```
docker compose up -d
```

`src/main/resources/application.yml` points at `jdbc:postgresql://localhost:5432/css`.

### The `lesson` table

| Column | Type | Why |
| --- | --- | --- |
| `id` | `uuid` primary key | Surrogate key. Never shown to a person, so a format change never touches a foreign key |
| `business_key` | `text NOT NULL UNIQUE` | The lesson number a person sees, e.g. `LES-2026-0042`. It is the value of `LessonId` |
| `status` | `text NOT NULL` + `CHECK` | One of `REQUESTED`, `CONFIRMED`, `COMPLETED`, `CANCELLED`, the same four as `LessonStatus` |
| `title` | `text NOT NULL` | What the lesson is about |
| `created_at` | `timestamptz NOT NULL DEFAULT now()` | When the row was written |

`UNIQUE` allows many `NULL`s in PostgreSQL, so `business_key` is also `NOT NULL`. A blank
lesson number is rejected in Java by `LessonId`. The `CHECK` and the enum must list the same
four statuses, and an unknown text in the column makes `LessonJdbc` throw instead of
inventing a status.

### Rollback and duplicates

`docs/rollback-proof.sql` shows by hand in psql that a failed second `INSERT` followed by
`ROLLBACK` leaves count 0.

Two integration tests in `LessonRegistryTest` assert opposite counts on purpose:

| Test | Transactions | Result | Count |
| --- | --- | --- | --- |
| `secondStatementRollsBack` | `insertTwice` is **one** transaction with two inserts | the second insert fails, so the first is rolled back too | **0** |
| `secondRequestKeepsTheFirst` | `register` is **one** transaction per call | the first call commits, the second call throws | **1** |

- `LessonRegistry` (in `handler`) is the `@Transactional` service. It catches the duplicate
  key error from the database and throws `DuplicateLesson`.
- `DuplicateLesson` lives in `domain` and is unchecked. Spring rolls back on an unchecked
  exception, and a `catch` that returns normally would commit the first insert.
- The tests do not use `@Transactional`, so the count they read is what was really committed.
- Each test drops the table and runs `schema.sql` again, so it starts from an empty database.

### Payments (week 4 port)

`PaymentGateway` is the outbound port in `domain`: `hold`, `payTutor`, `refund`.
`LessonService.move(lesson, from, to)` checks the rules first and only then moves money.
Forbidden moves and `REQUESTED → CANCELLED` move none. `LessonPaymentTest` mocks the port,
so no real HTTP call is made. `LoggingPaymentGateway` in `client` only prints for now.
