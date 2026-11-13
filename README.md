# Tutoring marketplace

This project is a small tutoring marketplace. A vendor sends a lesson, the application applies
lesson rules, records the lesson in PostgreSQL, and coordinates payment with a port. Other teams
reach it through a versioned REST API under `/api/v1`. The code uses Java 21, Spring Boot 3.5,
Hibernate/JPA, Flyway, springdoc-openapi, Spring Boot Actuator, and PostgreSQL 16.

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

## Package structure

A request flows controller -> service -> repository -> database. Each folder has one job:

```text
com.endev.tutoring/
  TutoringApplication            starts the app (root package, so Spring scans everything below)

  controller/   LessonController, TutorController, StudentController (@RestController, /api/v1/...)
  service/      LessonRegistrationService (@Transactional writes and reads), LessonService (rules + payment),
                TutorService, StudentService
  repository/   LessonRepository, TutorRepository, StudentRepository (@Repository, EntityManager)
  entity/       LessonEntity, TutorEntity, StudentEntity (@Entity, one per table)
  mapper/       LessonMapper, TutorMapper, StudentMapper (entity <-> model)
  model/        Lesson, LessonId, LessonStatus, Tutor, Student
  rule/         Rule, TransitionRule, UnpaidCannotComplete, LessonPolicy
  dto/          Create...Request and ...Response for lesson, tutor and student, ApiError, VendorLessonPayload
  exception/    DuplicateLessonException, DuplicateEmailException, LessonNotFoundException,
                TutorNotFoundException, StudentNotFoundException, GlobalExceptionHandler (@RestControllerAdvice)
  client/       PaymentPort, LoggingPaymentClient, DownstreamProperties (@ConfigurationProperties)
  config/       RuleConfig, ClientConfig
  demo/         LessonDemoRunner (prints the lesson process on start-up)
```

`model` and `rule` import no Spring, no `java.sql`, no `jakarta.persistence` and no
`jakarta.validation`, and the two lesson exceptions are plain unchecked exceptions. Only
`GlobalExceptionHandler` turns them into HTTP statuses. `LessonEntity` never leaves the
repository and service layer, so Jackson never sees it.

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

`V3__create_tutor_and_student.sql` adds two tables and links a lesson to them:

```sql
CREATE TABLE tutor (
  id uuid PRIMARY KEY, name text NOT NULL, email text NOT NULL UNIQUE, subject text NOT NULL,
  hourly_rate numeric(10, 2) NOT NULL CHECK (hourly_rate > 0), created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE student (
  id uuid PRIMARY KEY, name text NOT NULL, email text NOT NULL UNIQUE,
  created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE lesson
  ADD COLUMN tutor_id   uuid REFERENCES tutor (id),
  ADD COLUMN student_id uuid REFERENCES student (id);
```

Both links are optional, so lessons created before V3, or without a tutor yet, stay valid. When a
lesson is created with a `tutorId`, the V2 `tutor` text column gets that tutor's name instead of
`UNASSIGNED`.

On startup, Flyway applies pending migrations first. Hibernate then runs with
`spring.jpa.hibernate.ddl-auto: validate` and compares `LessonEntity` with the table. A mismatch
stops startup. Hibernate never creates or alters a table; `update` and `create` are not used because
schema changes must be explicit, reviewable, and repeatable in deployment.

Flyway records each applied migration in `flyway_schema_history`: its version, description,
checksum, installation order, and success. An applied file is frozen because changing its bytes
changes its checksum. The next change is always a new `V<n>` file. V1 was not edited to add
`tutor`; that change is V2.

After all three migrations have run, the history query is:

```text
SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;

 installed_rank | version |       description        | success
----------------+---------+--------------------------+---------
              1 | 1       | create lesson            | t
              2 | 2       | add tutor                | t
              3 | 3       | create tutor and student | t
(3 rows)
```

There is no standalone schema source outside the migrations. One owner of the schema prevents a
test or a manual startup script from drifting away from the versioned database history.

The surrogate `id` is the stable database primary key. `business_key` is a separate `UNIQUE`
constraint because it is the lesson identity used by vendor requests. `status` has the
`lesson_status_known` CHECK constraint shown above.

`LessonEntity` maps those columns. Java sets the UUID, so there is no `@GeneratedValue`.
`created_at` is left to PostgreSQL's `DEFAULT now()` with `insertable = false`; `status` is stored
as the CHECK words, and `tutor` is the non-null column added by V2.

`LessonRepository` receives an `EntityManager`. It uses `persist`, not Spring Data `save()`: the UUID is
already set, and `save()` would check whether that id exists first. Its count and status queries
are JPQL over entity fields such as `l.businessKey`, and `flush()` sends pending SQL to PostgreSQL.
`LessonMapper` is a plain class with static methods. Status mapping lives outside the repository
because repository exception translation would wrap an `IllegalStateException` from an unknown
database value in `InvalidDataAccessApiUsageException`.

## Transactions and isolation

`@Transactional` sits on `LessonRegistrationService`, the service boundary. One public registration
method is one transaction. `status` is a read-only transaction, `open-in-view` is false, and the
service never returns `LessonEntity`.

`persist` only makes Hibernate remember the row. `flush()` is inside the `try`, so the INSERT is
sent while the `DataIntegrityViolationException` catch is active. A PostgreSQL SQLState `23505`
becomes `DuplicateLessonException`; CHECK and NOT NULL violations are rethrown. Without the flush, the
constraint error would appear at commit, outside the catch.

`DuplicateLessonException` is unchecked, so Spring rolls the transaction back. A catch that returned
normally could commit a half-write: the kind of bug that fails at 02:00 when nobody is watching.
`insertTwice` is one transaction, so its first insert is rolled back too and the count is 0. Two
`register` calls are two transactions, so the first committed request stays and the count is 1.

PostgreSQL's default isolation is `READ COMMITTED`. There are no dirty reads: another transaction
does not see uncommitted rows, and each statement sees rows committed before that statement began.
This is why the integration tests are not `@Transactional`: each count is its own query and sees
only committed rows. `REPEATABLE READ` keeps one transaction's snapshot stable; `SERIALIZABLE`
also detects conflicts so the result is equivalent to a serial order. This project does not need
either stricter level.

## REST API

The lesson rules stay plain Java; HTTP is an adapter. Another team gets a URL, a JSON body and a status code.

| Call | Meaning |
|---|---|
| `POST /api/v1/lessons` | Create one lesson |
| `GET /api/v1/lessons/{id}` | Read that lesson by its UUID |
| `POST /api/v1/tutors` | Create a tutor: name, email (unique), subject, hourly rate |
| `GET /api/v1/tutors/{id}` | Read that tutor by its UUID |
| `POST /api/v1/students` | Create a student: name, email (unique) |
| `GET /api/v1/students/{id}` | Read that student by its UUID |

A lesson may name an existing tutor and student with the optional `tutorId` and `studentId`
fields. Old clients that send only `businessKey` and `subject` keep working on v1. An unknown
`tutorId` or `studentId` is **404** and writes nothing; a second tutor or student with the same
email is **409** `DUPLICATE`.

```json
{ "businessKey": "LES-2026-0500", "subject": "Algebra",
  "tutorId": "<tutor uuid>", "studentId": "<student uuid>" }
```

The version sits in the path. The path id is the row's UUID from Lab 3; the business key stays in
the body. There is no `PUT`, `PATCH`, `DELETE` or list endpoint in this lab, and a `GET` never
registers, moves or flushes anything. The assignment's `title` is this product's `subject`.

Create request (both fields required, `subject` at most 120 characters):

```json
{ "businessKey": "LES-2026-0042", "subject": "Algebra" }
```

A good create answers **201** with a `Location` header that is the `GET` for the new UUID:

```text
HTTP/1.1 201
Location: http://localhost:8080/api/v1/lessons/ae61b8be-ee78-4768-8b15-cfea4c906bd3

{"id":"ae61b8be-ee78-4768-8b15-cfea4c906bd3","businessKey":"LES-2026-0042","status":"REQUESTED","subject":"Algebra"}
```

`CreateLessonRequest` carries `@NotBlank` and `@Size(max = 120)` in the `dto` package, and
`@Valid` runs before the service: a bad body never reaches `LessonRegistrationService`. `LessonResponse`
is built from the model `Lesson`, never from `LessonEntity`, so `tutor` and `created_at` stay off
the wire. A new lesson starts as `REQUESTED` with the tutor `UNASSIGNED`; `status` is the same
token the CHECK constraint stores.

### Outcomes

The status is the outcome. A 200 whose body hides an error would be a bug.

| Situation | Status | `code` | `message` |
|---|---|---|---|
| Row inserted | 201 | — | body is the lesson |
| Lesson found by UUID | 200 | — | body is the lesson |
| `subject` or `businessKey` blank | 400 | `VALIDATION` | `subject must not be blank` |
| `subject` longer than 120 | 400 | `VALIDATION` | `subject size must be between 0 and 120` |
| Path id is not a UUID | 400 | `VALIDATION` | `id must be a UUID` |
| Body is not JSON | 400 | `VALIDATION` | `body must be JSON` |
| UUID parses and is not in the table | 404 | `NOT_FOUND` | `lesson not found: <uuid>` |
| Business key already stored | 409 | `DUPLICATE` | `duplicate lesson: <business key>` |
| Wrong `Content-Type` | 415 | `UNSUPPORTED` | `content type must be application/json` |
| Unknown path / wrong method | 404 / 405 | `NOT_FOUND` / `METHOD_NOT_ALLOWED` | `no such path` / `method not allowed` |
| A bug nobody mapped | 500 | `INTERNAL` | `unexpected error` |

When several fields fail, the message names the first one by field name, so the answer is stable.

409 is the Lab 3 `DuplicateLessonException`, now visible to the caller. The first committed row stays:

| After this call | Rows for that business key |
|---|---|
| 201 with a real subject | 1 |
| 400 subject blank | unchanged (0 on a first attempt) |
| 200 GET by UUID | unchanged |
| 409 same key again | 1 |
| 201 on a different key | 1 for each key |

### Error body and correlation id

Every failure body has exactly three fields:

```json
{ "code": "DUPLICATE", "message": "duplicate lesson: LES-2026-0042", "correlationId": "demo-1" }
```

`GlobalExceptionHandler` is the only class that knows 400, 404, 409, 415 and 500. `LessonNotFoundException` and
`DuplicateLessonException` are plain unchecked exceptions without a Spring import. The advice reads
`X-Correlation-Id`: if the caller sent one, the body repeats it; if it is missing or blank, a new
UUID is minted. A 500 logs the exception with the correlation id, but the exception text and class
name never go into the body.

Boot's own error payload is switched off too, for anything that bypasses the advice:
`server.error.include-stacktrace`, `include-message` and `include-binding-errors` are `never`, and
`include-exception` is `false`.

### Contract and versioning

springdoc publishes the contract at `/v3/api-docs` and the UI at `/swagger-ui.html`. Create lists
201, 400 and 409; get lists 200 and 404. `businessKey` and `subject` are required, and the response
schema is `LessonResponse`, not the entity.

| Still `/api/v1` | Needs `/api/v2`, v1 left alone |
|---|---|
| A new optional field old clients can ignore | Renaming `subject` |
| Clearer message text | 201 becoming 200 without `Location` |
| | A status token becoming a number |

## Configuration and profiles

Nothing that varies between a laptop and a server is a Java string. `src/main/java` contains no
`jdbc:postgresql` and no `http://` literal; `ProfileFilesTest` fails if one appears.

| File | Holds |
|---|---|
| `application.yaml` | What every run shares: `ddl-auto: validate`, `open-in-view: false`, Flyway, the error leak settings, actuator exposure, and `spring.profiles.default: dev`. No URL and no password. |
| `application-dev.yaml` | The laptop: PostgreSQL `localhost:5432/css`, user and password `css` (the classroom default), payment provider at `http://localhost:9090` with a `2s` timeout. |
| `application-prod.yaml` | Reads `CSS_DB_URL`, `CSS_DB_USER`, `CSS_DB_PASSWORD` and `CSS_DOWNSTREAM_URL` from the environment. No default secret. |

A boot with no flag uses `dev`. Production sets `SPRING_PROFILES_ACTIVE=prod` and the variables;
the committed files never set an active profile.

`DownstreamProperties` binds `app.downstream.base-url` and `app.downstream.timeout` with
`@ConfigurationProperties`. The timeout is a `Duration` (`2s`), not an int multiplied in Java. Both
values are validated, so a profile without them fails at start-up rather than at the first payment.
`ClientConfig` registers the type and `LoggingPaymentClient` receives it. No call is made yet:
Lab 6 adds the HTTP call and enforces the timeout.

## Actuator

Only `health` and `info` are exposed, and health hides its details, so its payload never lists the
datasource URL.

| Call | Result |
|---|---|
| `GET /actuator/health` | **200** `{"status":"UP"}` |
| `GET /actuator/info` | **200** with the product name |
| `GET /actuator/env`, `/heapdump`, `/beans`, `/shutdown` | **404** `NOT_FOUND`, not exposed |

## Tests

| Test class | Kind | What it proves |
|---|---|---|
| `TutoringApplicationTest` | `@SpringBootTest` integration | The application context and JPA entity model start. |
| `DuplicateLessonExceptionTest` | Unit | Duplicate lesson error text and unchecked exception behavior. |
| `LessonIdTest` | Unit | Lesson id value behavior. Its malformed-format test is disabled because format validation is not implemented yet. |
| `LessonPolicyTest` | Unit | Status transition policy using `@CsvSource` and `assertAll`. |
| `TransitionRuleTest` | Unit | Transition rules, grouped with `@Nested`, `@CsvSource`, and `assertAll`. |
| `UnpaidCannotCompleteTest` | Unit | An unpaid lesson cannot be completed. |
| `VendorLessonPayloadTest` | Unit | Vendor payload value behavior. |
| `LessonServicePaymentTest` | Mockito | A mocked `PaymentPort` receives the payment calls. |
| `LessonServiceTest` | `@SpringBootTest` integration | Lesson service wiring and rule behavior. |
| `LessonRegistrationServiceTest` | `@SpringBootTest` integration | Registration, tutor persistence, duplicate translation, and the committed counts 0 and 1. It is not `@Transactional`. |
| `FlywayMigrationTest` | Migration integration | V1, V2 and V3 are the three successful migrations and the old standalone resource is absent. |
| `LessonRepositoryTest` | PostgreSQL `@DataJpaTest` slice | EntityManager `persist`, JPQL count/status access, status CHECK values, parameter binding, and flush-time integrity errors. |
| `LessonMapperTest` | Unit | Every status maps to and from its database word; unknown and null values fail. |
| `SchemaTest` | `@SpringBootTest` integration | Database constraints and the V2 tutor default. |
| `TransactionIsolationTest` | `@SpringBootTest` integration | PostgreSQL's default isolation, invisible uncommitted rows, and rollback visibility. It is not `@Transactional`. |
| `LessonTest`, `LessonNotFoundExceptionTest` | Unit | The model view of a stored lesson and the unchecked not-found exception. |
| `LessonControllerTest` | `@WebMvcTest` web slice | Service mocked, no PostgreSQL: 201 with `Location` and the first status, 200 by UUID, every 400 text, 404, 409, 415, a 500 that leaks nothing, the three-field error body, and the echoed or minted correlation id. |
| `LessonApiTest` | `@SpringBootTest` + MockMvc on PostgreSQL | A second POST of the same business key is 409, the row count stays 1, and `Location` returns the first row. |
| `OpenApiContractTest` | `@SpringBootTest` + MockMvc | `/v3/api-docs` lists 201, 400, 409 on create and 200, 404 on get, the required fields, and no entity schema. |
| `DownstreamPropertiesTest` | `ApplicationContextRunner` | `app.downstream` binds a URI and a `Duration`; a missing value stops the start-up. |
| `ProfileFilesTest` | Unit, reads the yaml text | The shared file holds no URL or password and defaults to `dev`; dev is localhost; prod is only env placeholders; the error leak and actuator exposure settings; no URL literal in `src/main/java`. |
| `DevProfileTest` | `@SpringBootTest` on `dev` | The bound values after a real boot: `http://localhost:9090`, 2 seconds, the localhost `css` database, and the same properties inside the payment adapter. No socket is opened. |
| `ActuatorTest` | `@SpringBootTest` + MockMvc | Health is 200 UP without details, info names the product, and `env`, `heapdump`, `beans`, `shutdown` are 404. |
| `TutorStudentApiTest` | `@SpringBootTest` + MockMvc on PostgreSQL | Tutors and students: create with `Location`, read by UUID, 409 on a taken email (one row stays), 400 on a bad email or rate, 404 on an unknown UUID. Lessons keep `tutorId`/`studentId` and the tutor's name; an unknown tutor or student is 404 and writes no lesson. |

Database tests run `DELETE FROM lesson` in `@BeforeEach` so each method starts with no lesson rows.
They never DROP the table: doing so would leave Flyway history saying the migration ran while the
table was gone. The JPA slice uses the real PostgreSQL database, keeps the existing application
context and exception translation configuration, and lets Flyway migrate before Hibernate
validates.

## Run it

The `dev` profile connects to PostgreSQL 16 at `localhost:5432`, database `css`, user `css`,
password `css`. Start the database and run the application with:

```text
docker compose up -d
mvn -q verify
mvn spring-boot:run
docker compose down
```

The first boot of an empty database logs the profile, the migration, the payment provider address
and then the lesson demo:

```text
No active profile set, falling back to 1 default profile: "dev"
Current version of schema "public": << Empty Schema >>
Migrating schema "public" to version "1 - create lesson"
Migrating schema "public" to version "2 - add tutor"
Successfully applied 2 migrations to schema "public", now at version v2 (execution time 00:00.007s)
Payment provider configured at http://localhost:9090 with a PT2S timeout; no call is made yet
Tomcat started on port 8080 (http) with context path '/'
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

Try the API and the health check on a running app:

```text
curl -i -X POST localhost:8080/api/v1/lessons -H 'Content-Type: application/json' \
     -d '{"businessKey":"LES-2026-0100","subject":"Algebra"}'                         # 201 + Location
curl -i -X POST localhost:8080/api/v1/lessons -H 'Content-Type: application/json' \
     -H 'X-Correlation-Id: demo-1' -d '{"businessKey":"LES-2026-0100","subject":"Algebra"}'  # 409, correlationId demo-1
curl -i localhost:8080/api/v1/lessons/LES-2026-0100                                 # 400 id must be a UUID
curl -i localhost:8080/actuator/health                                              # 200 {"status":"UP"}
curl -i localhost:8080/actuator/env                                                 # 404
```

The contract is at `http://localhost:8080/swagger-ui.html`.

Production uses the `prod` profile and reads everything that varies from the environment:

```text
SPRING_PROFILES_ACTIVE=prod \
CSS_DB_URL=jdbc:postgresql://<host>:5432/<db> CSS_DB_USER=<user> CSS_DB_PASSWORD=<password> \
CSS_DOWNSTREAM_URL=https://<payment-provider> \
java -jar target/tutoring-marketplace-1.0-SNAPSHOT.jar
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
