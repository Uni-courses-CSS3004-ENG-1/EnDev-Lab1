# Tutoring marketplace

This project is a small tutoring marketplace. A vendor sends a lesson, the application applies
lesson rules, records the lesson in PostgreSQL, and coordinates payment with a port. The code uses
Java 21, Spring Boot, Hibernate/JPA, Flyway, and PostgreSQL 16.

## Core item

The core item is a lesson. A lesson has a business key, subject, tutor, and a lifecycle status.
The database id is a surrogate UUID; the business key is the human-facing identity.

| Status | Meaning |
|---|---|
| `REQUESTED` | The vendor has requested a lesson. |
| `CONFIRMED` | The lesson is confirmed and payment can be released. |
| `COMPLETED` | The lesson has happened. |
| `CANCELLED` | The lesson will not happen. |

The database CHECK constraint and `LessonStatus` use these four uppercase words. Keeping the rule
in the database matters because another client must not be able to write an unknown status.

## Package diagram

The arrows point inward: outer adapters depend on the application handlers, and handlers depend on
the domain and persistence ports they use.

```text
client   -> handler -> domain
dto      -> handler
config   -> handler, domain, client
persistence -> handler

client/
  LoggingPaymentClient

dto/
  VendorLessonPayload

handler/
  LessonDemoRunner
  LessonRegistration
  LessonService

persistence/
  LessonEntity (@Entity)
  LessonJpa (@Repository, EntityManager)
  LessonMapper (plain final class)

domain/
  DuplicateLesson, LessonId, LessonPolicy, LessonStatus
  PaymentPort, Rule, TransitionRule, UnpaidCannotComplete

config/
  Application, RuleConfig
```

`domain` imports no Spring, no `java.sql` and no `jakarta.persistence`. `LessonEntity` is kept in
`persistence`, so persistence annotations do not leak into the rules and value objects.

`Application` starts the application and `RuleConfig` supplies the rule and payment wiring.
`LessonRegistration` handles database writes. `LessonService` applies lesson rules and uses the
`PaymentPort`; `LoggingPaymentClient` is the current adapter for that port.

## Storage

The schema is born from the versioned migrations in `src/main/resources/db/migration/`.

`V1__create_lesson.sql` creates the original lesson table:

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

`V2__add_tutor.sql` adds the tutor column with a value for existing rows:

```sql
ALTER TABLE lesson
  ADD COLUMN tutor text NOT NULL DEFAULT 'UNASSIGNED';
```

On startup, Flyway applies pending migrations first. Hibernate then runs with
`spring.jpa.hibernate.ddl-auto: validate` and compares `LessonEntity` with the table. A mismatch
stops startup. Hibernate never creates or alters a table; `update` and `create` are not used because
schema changes must be explicit, reviewable, and repeatable in deployment.

Flyway records each applied migration in `flyway_schema_history`: its version, description,
checksum, installation order, and success. An applied file is frozen because changing its bytes
changes its checksum. The next change is always a new `V<n>` file. V1 was not edited to add
`tutor`; that change is V2.

After both migrations have run, the history query is:

```text
SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;

 installed_rank | version |  description  | success
----------------+---------+---------------+---------
              1 | 1       | create lesson | t
              2 | 2       | add tutor     | t
(2 rows)
```

There is no standalone schema source outside the migrations. One owner of the schema prevents a
test or a manual startup script from drifting away from the versioned database history.

The surrogate `id` is the stable database primary key. `business_key` is a separate `UNIQUE`
constraint because it is the lesson identity used by vendor requests. `status` has the
`lesson_status_known` CHECK constraint shown above.

`LessonEntity` maps those columns. Java sets the UUID, so there is no `@GeneratedValue`.
`created_at` is left to PostgreSQL's `DEFAULT now()` with `insertable = false`; `status` is stored
as the CHECK words, and `tutor` is the non-null column added by V2.

`LessonJpa` receives an `EntityManager`. It uses `persist`, not Spring Data `save()`: the UUID is
already set, and `save()` would check whether that id exists first. Its count and status queries
are JPQL over entity fields such as `l.businessKey`, and `flush()` sends pending SQL to PostgreSQL.
`LessonMapper` is a plain class with static methods. Status mapping lives outside the repository
because repository exception translation would wrap an `IllegalStateException` from an unknown
database value in `InvalidDataAccessApiUsageException`.

## Transactions and isolation

`@Transactional` sits on `LessonRegistration`, the service boundary. One public registration
method is one transaction. `status` is a read-only transaction, `open-in-view` is false, and the
service never returns `LessonEntity`.

`persist` only makes Hibernate remember the row. `flush()` is inside the `try`, so the INSERT is
sent while the `DataIntegrityViolationException` catch is active. A PostgreSQL SQLState `23505`
becomes `DuplicateLesson`; CHECK and NOT NULL violations are rethrown. Without the flush, the
constraint error would appear at commit, outside the catch.

`DuplicateLesson` is unchecked, so Spring rolls the transaction back. A catch that returned
normally could commit a half-write: the kind of bug that fails at 02:00 when nobody is watching.
`insertTwice` is one transaction, so its first insert is rolled back too and the count is 0. Two
`register` calls are two transactions, so the first committed request stays and the count is 1.

PostgreSQL's default isolation is `READ COMMITTED`. There are no dirty reads: another transaction
does not see uncommitted rows, and each statement sees rows committed before that statement began.
This is why the integration tests are not `@Transactional`: each count is its own query and sees
only committed rows. `REPEATABLE READ` keeps one transaction's snapshot stable; `SERIALIZABLE`
also detects conflicts so the result is equivalent to a serial order. This project does not need
either stricter level.

## Tests

| Test class | Kind | What it proves |
|---|---|---|
| `ApplicationTest` | `@SpringBootTest` integration | The application context and JPA entity model start. |
| `DuplicateLessonTest` | Unit | Duplicate lesson error text and unchecked exception behavior. |
| `LessonIdTest` | Unit | Lesson id value behavior. Its malformed-format test is disabled because format validation is not implemented yet. |
| `LessonPolicyTest` | Unit | Status transition policy using `@CsvSource` and `assertAll`. |
| `TransitionRuleTest` | Unit | Transition rules, grouped with `@Nested`, `@CsvSource`, and `assertAll`. |
| `UnpaidCannotCompleteTest` | Unit | An unpaid lesson cannot be completed. |
| `VendorLessonPayloadTest` | Unit | Vendor payload value behavior. |
| `LessonServicePaymentTest` | Mockito | A mocked `PaymentPort` receives the payment calls. |
| `LessonServiceTest` | `@SpringBootTest` integration | Lesson service wiring and rule behavior. |
| `LessonRegistrationTest` | `@SpringBootTest` integration | Registration, tutor persistence, duplicate translation, and the committed counts 0 and 1. It is not `@Transactional`. |
| `FlywayMigrationTest` | Migration integration | V1 and V2 are the two successful migrations and the old standalone resource is absent. |
| `LessonJpaTest` | PostgreSQL `@DataJpaTest` slice | EntityManager `persist`, JPQL count/status access, status CHECK values, parameter binding, and flush-time integrity errors. |
| `LessonMapperTest` | Unit | Every status maps to and from its database word; unknown and null values fail. |
| `SchemaTest` | `@SpringBootTest` integration | Database constraints and the V2 tutor default. |
| `TransactionIsolationTest` | `@SpringBootTest` integration | PostgreSQL's default isolation, invisible uncommitted rows, and rollback visibility. It is not `@Transactional`. |

Database tests run `DELETE FROM lesson` in `@BeforeEach` so each method starts with no lesson rows.
They never DROP the table: doing so would leave Flyway history saying the migration ran while the
table was gone. The JPA slice uses the real PostgreSQL database, keeps the existing application
context and exception translation configuration, and lets Flyway migrate before Hibernate
validates.

## Run it

The default local connection is PostgreSQL 16 at `localhost:5432`, database `css`, user `css`,
password `css`. Start the database and run the application with:

```text
docker compose up -d
mvn -q verify
mvn spring-boot:run
docker compose down
```

The first boot of an empty database logs the migration and then the lesson demo:

```text
Current version of schema "public": << Empty Schema >>
Migrating schema "public" to version "1 - create lesson"
Migrating schema "public" to version "2 - add tutor"
Successfully applied 2 migrations to schema "public", now at version v2 (execution time 00:00.012s)
Lesson LES-2026-0042 arrived from the vendor as REQUESTED
  REQUESTED -> COMPLETED: forbidden (An unpaid lesson cannot be completed)
  payment: hold for LES-2026-0042
  REQUESTED -> CONFIRMED: allowed
  payment: release to the tutor for LES-2026-0042
  CONFIRMED -> COMPLETED: allowed
  COMPLETED -> CANCELLED: forbidden (Cannot move a lesson from COMPLETED to CANCELLED)
Lesson LES-2026-0042 ends as COMPLETED
```

On a second boot, Flyway reports:

```text
Current version of schema "public": 2
Schema "public" is up to date. No migration necessary.
```

### Database already has the old table?

If a database contains the lesson table but has no Flyway history, startup fails with:

```text
Found non-empty schema(s) "public" but no schema history table
```

Reset the local `css` database by connecting to the `postgres` database, not to `css`:

```text
docker compose exec -T postgres psql -U css -d postgres -c 'DROP DATABASE IF EXISTS css WITH (FORCE);' -c 'CREATE DATABASE css OWNER css;'
```

Then run `mvn spring-boot:run` again. `baseline-on-migrate: false` is intentional and is not the
fix: setting it to true would mark V1 as done without running V1, leaving an empty database without
the lesson table.
