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

Three more files followed for accounts and booking, each a new version and none an edit:

| File | Change |
|---|---|
| `V4__create_app_user.sql` | The `app_user` table: unique username, BCrypt hash, role CHECK, link to a student or tutor row |
| `V5__add_tutor_bio.sql` | `tutor.bio text NOT NULL DEFAULT ''`, so existing tutors stay valid |
| `V6__add_lesson_starts_at.sql` | `lesson.starts_at timestamptz`, optional, so lessons made before booking stay valid |

After all six migrations have run, the history query is:

```text
SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;

 installed_rank | version |       description        | success
----------------+---------+--------------------------+---------
              1 | 1       | create lesson            | t
              2 | 2       | add tutor                | t
              3 | 3       | create tutor and student | t
              4 | 4       | create app user          | t
              5 | 5       | add tutor bio            | t
              6 | 6       | add lesson starts at     | t
(6 rows)
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
| `POST /api/v1/auth/register` | Sign up as a student or tutor; answers **201** with a token, like login |
| `GET /api/v1/account` | My profile: name, email, role, and a tutor's subject, price and bio |
| `GET /api/v1/tutors` | The public tutor catalogue, no token needed and no emails |
| `POST /api/v1/lessons/book` | A student books a tutor for one hour (`tutorId`, `subject`, `startsAt`) |
| `GET /api/v1/lessons/mine` | My lessons: a tutor's teaching or a student's bookings, with names and price |
| `POST /api/v1/lessons/{id}/confirm` | Tutor: `REQUESTED` → `CONFIRMED`, payment hold placed (the locked action) |
| `POST /api/v1/lessons/{id}/complete` | Tutor: `CONFIRMED` → `COMPLETED`, hold released |
| `POST /api/v1/lessons/{id}/cancel` | The lesson's tutor or student: → `CANCELLED`, a held payment is refunded |

A lesson may name an existing tutor and student with the optional `tutorId` and `studentId`
fields. Old clients that send only `businessKey` and `subject` keep working on v1. An unknown
`tutorId` or `studentId` is **404** and writes nothing; a second tutor or student with the same
email is **409** `DUPLICATE`.

```json
{ "businessKey": "LES-2026-0500", "subject": "Algebra",
  "tutorId": "<tutor uuid>", "studentId": "<student uuid>" }
```

The version sits in the path. The path id is the row's UUID from Lab 3; the business key stays in
the body. There is no `PUT`, `PATCH` or `DELETE`; statuses move only through the three verbs above,
and a `GET` never registers, moves or flushes anything. The assignment's `title` is this product's `subject`.

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

springdoc publishes the contract at `/v3/api-docs` and the UI at `/swagger-ui.html`; both open
without a token, and the UI's Authorize button sends `Bearer <token>`. Login lists 200, 400 and
401; confirm lists 200, 400, 401, 403, 404 and 409; `/api/me` lists 200 and 401. Create lists
201, 400 and 409; get lists 200 and 404. `businessKey` and `subject` are required, and the response
schema is `LessonResponse`, not the entity.

| Still `/api/v1` | Needs `/api/v2`, v1 left alone |
|---|---|
| A new optional field old clients can ignore | Renaming `subject` |
| Clearer message text | 201 becoming 200 without `Location` |
| | A status token becoming a number |

## Security

Two questions stand in front of every controller. **Who are you?** fails as **401**. **May you do
this?** fails as **403**. A caller logs in once at `POST /api/v1/auth/login`, receives a signed JWT,
and sends it as `Authorization: Bearer <token>` on every other call.

### Roles

| Role | Who | May |
|---|---|---|
| `STUDENT` (junior) | A student who books lessons | Create and read lessons, tutors and students |
| `TUTOR` (senior) | A tutor who teaches them | Everything a student may, and confirm a requested lesson |

The locked action is `POST /api/v1/lessons/{id}/confirm`: it moves a lesson from `REQUESTED` to
`CONFIRMED` and places the payment hold, so only a tutor may do it. It is a status change, not a
delete; the row stays.

### Rules

The filter chain follows this table from top to bottom. The first matching row wins. The numbers are
the numbered comments in `SecurityConfig`.

| # | Action | `STUDENT` | `TUTOR` | No token |
|---|---|---|---|---|
| 1 | `POST /api/v1/auth/login`, `POST /api/v1/auth/register` | anyone | anyone | allowed |
| 2 | The page: `GET /`, `/index.html`, `/app.css`, `/app.js` | anyone | anyone | allowed |
| 3 | `GET /actuator/health`, `GET /actuator/info` | anyone | anyone | allowed |
| 4 | springdoc: `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**` | anyone | anyone | allowed |
| 5 | The public catalogue: `GET /api/v1/tutors` (this exact path only) | anyone | anyone | allowed |
| 6 | Confirm or complete a lesson: `POST /api/v1/lessons/{id}/confirm`, `/complete` | **403** | yes | 401 |
| 7 | Book a tutor: `POST /api/v1/lessons/book` | yes | **403** | 401 |
| 8 | `GET /api/me` | yes | yes | 401 |
| 9 | Everything else in `/api/v1/**`: create and read lessons, tutors and students, `mine`, `cancel`, `account` | yes | yes | 401 |
| 10 | Anything else, including `/actuator/env` and `/actuator/heapdump` | 403 | 403 | 401 |

`SecurityConfig` is this table in code. Rows 6 and 7 sit above row 9: with the wide rule first, a
student would match it and confirm. Row 5 names one exact path, so `/api/v1/tutors/{id}` still needs a
token, and the list it opens carries no email. `anyRequest().denyAll()` closes every path a teammate
adds later and forgets to list. CSRF is off and sessions are `STATELESS` because the credential is a
header, never a cookie; there is no form login, no HTTP Basic and no server-side logout.

**Whose lesson is it?** The role is checked first, by the URL rule and again by `@PreAuthorize`.
`LessonWorkflowService` then checks ownership: a lesson booked with a tutor may only be confirmed,
completed or cancelled by **that** tutor, and cancelled by its own student. Another tutor gets
**403** `FORBIDDEN`, `not your lesson`, and the row does not change. A lesson with no tutor yet (made by
`POST /api/v1/lessons`, as in Lab 5) is decided by the role alone.

### Login and the token

`POST /api/v1/auth/login` is the only call that sees a password.

```json
{ "username": "aliya", "password": "student-dev" }
```

| Login | Status | Body |
|---|---|---|
| Known user, right password | 200 | `{"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":900}` |
| Unknown user | 401 | `UNAUTHENTICATED`, `bad username or password` |
| Known user, wrong password | 401 | the same message |
| Blank username or password | 400 | `VALIDATION`, `username must not be blank` |

The two 401 rows are one message, so the answer does not tell an attacker which names exist.
`AuthenticationManager` compares the typed password with the BCrypt hash; `TokenService` signs an
HS256 token that lives 15 minutes (`expiresIn` is seconds). The decoded payload of Aliya's token:

```json
{ "iss": "tutoring", "sub": "aliya", "roles": ["STUDENT"], "iat": 1795246800, "exp": 1795247700 }
```

`roles` carries the name without `ROLE_`; the decoder's converter adds the prefix back, so
`hasRole("TUTOR")` finds `ROLE_TUTOR`. Anyone can read the payload, so it holds no email, phone or
password. Changing `STUDENT` to `TUTOR` breaks the signature and the call is 401.

### Accounts and sign-up

Accounts live in the `app_user` table (`V4__create_app_user.sql`): username (unique), a BCrypt
`password_hash`, the role (`STUDENT` or `TUTOR`, a CHECK constraint), and a link to the person's
`student` or `tutor` row. A second CHECK makes a student account point at a student row and a tutor
account at a tutor row.

Login asks `AccountUsers` (a `UserDetailsService`) for the row by name; Spring compares the typed
password with the hash. An unknown name and a wrong password are the same 401.

`POST /api/v1/auth/register` is one transaction in `AccountService.register`: it writes the student or
tutor row, then the account row with `passwordEncoder.encode(password)`. A taken username or email is
**409** `DUPLICATE` and neither row stays. A tutor without a subject or price is **400**. The role in
the sign-up body picks the kind of account once; every later call takes the role from the signed
token, never from JSON. After sign-up the controller logs in with the same name and password, so a
new account gets exactly the token a returning one gets.

```json
{ "username": "madina", "password": "correct-horse", "role": "TUTOR", "name": "Madina Sultan",
  "email": "madina@example.com", "subject": "Biology", "hourlyRate": 20.00, "bio": "Cells and genes." }
```

| Sign-up | Status | Body |
|---|---|---|
| New name and email | 201 | the token, `Location: /api/v1/account` |
| Username taken (also `aliya` and `dana`) | 409 | `DUPLICATE`, `username already taken: madina` |
| Email taken | 409 | `DUPLICATE`, `email already exists: madina@example.com` |
| Password under 8 or over 72 characters | 400 | `VALIDATION`, `password size must be between 8 and 72` |
| Role not `STUDENT` or `TUTOR` | 400 | `VALIDATION`, `role must be STUDENT or TUTOR` |
| Tutor without a subject | 400 | `VALIDATION`, `subject is required for a tutor` |

### Dev users

`DevUsersConfig` is `@Profile("dev")`. At start-up it registers each missing classroom account
through `AccountService`, the same path as the sign-up form, so the passwords are BCrypt hashes in
the table and never text. Prod registers nobody.

| Username | Password | Role |
|---|---|---|
| `aliya` | `student-dev` | `STUDENT` |
| `dana` | `tutor-dev` | `TUTOR` (Algebra, $25) |
| `timur`, `mira`, `arman`, `sofia`, `erik`, `aigerim` | `tutor-dev` | `TUTOR`, so the catalogue is not empty |

### The secret

| File | `app.jwt.secret` |
|---|---|
| `application.yaml` | none (only `app.jwt.issuer: tutoring`) |
| `application-dev.yaml` | a classroom value of 32+ characters |
| `application-prod.yaml` | `${CSS_JWT_SECRET}`, no default |

`JwtProperties` is validated: a secret shorter than 32 characters stops the boot, and so does a
missing `CSS_JWT_SECRET` in prod. `src/main/java` holds no secret literal; `ProfileFilesTest` fails
if the dev secret or a plain-text password prefix appears there.

### 401 and 403 bodies

401 and 403 are decided in the filters, before any controller, so `@RestControllerAdvice` never
sees them. `SecurityErrors` is the entry point and the access-denied handler and writes the Lab 4
error record, with the same `X-Correlation-Id` echo-or-mint rule as the advice.

| Check, in order | Fails as |
|---|---|
| No `Authorization: Bearer` (Basic does not count) | 401 `UNAUTHENTICATED`, `valid token required` |
| Not three base64url pieces | 401 |
| Signature is not our secret | 401 |
| Expired, or `iss` is not `tutoring` | 401 |
| Role is not allowed | 403 `FORBIDDEN`, `not allowed for your role` |

```json
{ "code": "FORBIDDEN", "message": "not allowed for your role", "correlationId": "demo-7" }
```

### The locked action

`POST /api/v1/lessons/{id}/confirm` takes the row's UUID and no body. The controller passes
`Authentication.getName()` to `LessonWorkflowService.confirm`, so a JSON `actor` or `role` field is
ignored. The method carries `@PreAuthorize("hasRole('TUTOR')")` (enabled by `@EnableMethodSecurity`):
the same role as the URL rule, a second lock on the same door. The rules in `rule/` stay plain Java.

| Call | Result |
|---|---|
| No token | 401; the service method is not called |
| `STUDENT` | 403; the service method is not called and the row stays `REQUESTED` |
| `TUTOR`, lesson `REQUESTED` | 200 with `"status":"CONFIRMED"`; the payment hold is placed; the log says `lesson LES-2026-0042 confirmed by dana` |
| `TUTOR`, the rules refuse (already `CONFIRMED`, `COMPLETED` or `CANCELLED`) | 409 `INVALID_TRANSITION`; the row does not change |
| `TUTOR`, unknown UUID | 404 `NOT_FOUND` |

### Who am I

`GET /api/me` returns what the filter put into the `SecurityContext` for this request:

```json
{ "username": "aliya", "authorities": ["ROLE_STUDENT"] }
```

### The page

`http://localhost:8080/` is our own page, served by the same Spring Boot process from
`src/main/resources/static/`: `index.html`, `app.css` and `app.js`, the only three files rule 2 opens.
Same origin, so there is no CORS bean. `formLogin` stays off, so Spring's "Please sign in" page never
appears.

The page is a React 19 + TypeScript app in `frontend/`, built with Vite. `npm run build` writes exactly
those three files (no hashed chunks); the built files are committed, so `mvn spring-boot:run` needs no
Node. Motion does the animations and lucide-react the icons.

```text
cd frontend
npm install
npm run dev      # http://localhost:5173, API calls forwarded to :8080 (one origin, no CORS)
npm run build    # writes ../src/main/resources/static/{index.html,app.js,app.css}
```

| Signed out | Student | Tutor |
|---|---|---|
| Landing page, live tutor catalogue with search, subject chips and sort, the two roles, dev users | Book a tutor: pick a day and an hour, see the price | Teaching desk: requests to confirm or decline, confirmed schedule to complete or cancel |
| Log in or sign up as a student or tutor | My lessons: upcoming and history, cancel, and a Confirm button that shows the server's 403 | Earnings and money on hold |

- The token is kept in one variable inside `frontend/src/lib/api.ts`: not in `localStorage`,
  `sessionStorage`, a cookie, the URL or the page text. React components only ever see the username,
  the role from `GET /api/me` and the expiry time.
- `dropToken` is the one place the token is forgotten: log out, the 15-minute expiry, or a 401 on a
  signed-in call. The session ring in the top bar counts the 15 minutes down.
- Every call sends `X-Correlation-Id`. Each answer shows as a receipt: a sentence, then the HTTP
  status, and on an error `code`, `message` and `correlationId`. No stack trace.
- The request log (bottom left) lists what the page sent: method, path, status, code and correlation
  id. Never a body, never the token.

| Step in the browser | Result on the page |
|---|---|
| Open `/` | The tutoring marketplace page, not a Spring login |
| Sign in as `aliya` with a wrong password | 401 `UNAUTHENTICATED`, `bad username or password`; the form stays |
| Sign in as `aliya` / `student-dev` | `aliya`, role `STUDENT`, 15:00; no token on the page |
| Book Dana for an hour | 201, status `REQUESTED` |
| Confirm lesson | 403 `FORBIDDEN`; "the row did not change: status is still REQUESTED" |
| Log out, sign in as `dana` / `tutor-dev`, confirm | 200, status `CONFIRMED` |
| Log out, read lesson | 401 `UNAUTHENTICATED`, `valid token required` |

## Configuration and profiles

Nothing that varies between a laptop and a server is a Java string. `src/main/java` contains no
`jdbc:postgresql` and no `http://` literal; `ProfileFilesTest` fails if one appears.

| File | Holds |
|---|---|
| `application.yaml` | What every run shares: `ddl-auto: validate`, `open-in-view: false`, Flyway, the error leak settings, actuator exposure, and `spring.profiles.default: dev`. No URL and no password. |
| `application-dev.yaml` | The laptop: PostgreSQL `localhost:5432/css`, user and password `css` (the classroom default), payment provider at `http://localhost:9090` with a `2s` timeout. |
| `application-prod.yaml` | Reads `CSS_DB_URL`, `CSS_DB_USER`, `CSS_DB_PASSWORD`, `CSS_DOWNSTREAM_URL` and `CSS_JWT_SECRET` from the environment. No default secret. |

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
| `GET /actuator/env`, `/heapdump`, `/beans`, `/shutdown` | **401** without a token, **403** with one; never the payload |

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
| `FlywayMigrationTest` | Migration integration | V1 to V6 are the six successful migrations in order and the old standalone resource is absent. |
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
| `SecurityRulesTest` | `@WebMvcTest` + `SecurityConfig`, services mocked, `jwt()` | The README rules table: no token 401 `UNAUTHENTICATED`, a student's confirm 403 `FORBIDDEN`, a tutor's confirm 200 with the token's subject as actor, a refused move 409, `/api/me`, health open; refused calls never reach the mocks. |
| `AuthControllerTest` | `@WebMvcTest` + `SecurityConfig` | Login 200 with `Bearer` and 900, both bad logins the same 401, blank fields 400 without a password check. A refused login logs `login refused for <name> [<correlationId>]`: never the password, one line even if the name holds a line break, and the same id as the error body. |
| `TokenServiceTest` | Unit, no web layer | A token issued with a test secret decodes with the same secret: subject, `roles` without `ROLE_`, `iss`, 15 minutes; another issuer or secret rejects it. |
| `DevUsersTest` | `@SpringBootTest` on PostgreSQL | The dev accounts in `app_user`: BCrypt hashes, two hashes of one password differ, `ROLE_` prefixes, each account linked to its profile row, one exception for wrong password and unknown user. |
| `SignUpApiTest` | `@SpringBootTest` + MockMvc on PostgreSQL | Sign-up answers 201 with a token that works on `/api/me`; the hash is BCrypt; a new tutor shows in the public catalogue without an email; a taken username or email is 409 and writes no half; a tutor without a subject and bad fields are 400. |
| `BookingFlowTest` | `@SpringBootTest` + MockMvc on PostgreSQL | aliya books dana with real tokens; each side sees its own lessons; a tutor cannot book; a clashing hour is 409 `SLOT_TAKEN`, a past one 400; dana confirms and completes; another tutor gets 403 `not your lesson`; a student may cancel but not complete. |
| `TokenCheckTest` | `@SpringBootTest` + MockMvc, real decoder | Each row of the token check: no header, `Bearer hello`, another secret, expired, `iss` helpdesk, Basic, an unknown role, a path outside the table. |
| `LoginApiTest` | `@SpringBootTest` + MockMvc | The real dev users and signer: dana's token decodes to `TUTOR`; the two bad logins are byte-for-byte the same body. |
| `LessonWorkflowServiceTest` | `@SpringBootTest` on PostgreSQL | `@PreAuthorize` without HTTP: a student and an anonymous caller are denied and the row stays; a tutor confirms; a second confirm or a cancelled lesson is refused. |
| `AuthFlowTest` | `@SpringBootTest` + MockMvc on PostgreSQL | The marker's path with real tokens: 403 and the row unchanged, 200 `CONFIRMED`, 409, 401 after logout; the log names dana and holds no token or password. |
| `AuthPageTest` | `@SpringBootTest` + MockMvc | `/` is our page shell and `app.js` carries the product, roles, dev users and the password field, with no Spring login form; the page files need no token, the bundle stores nothing; `/api` stays 401. |
| `AuthPageScriptTest` | Unit, reads `frontend/src` | One `dropToken` clears the token; logout, expiry and a rejected token all go through it; no file in `frontend/src` touches storage, cookies or the URL. |
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

Try the API and the health check on a running app. Every API call except login needs a token:

```text
STUDENT=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
     -d '{"username":"aliya","password":"student-dev"}' | jq -r .accessToken)
TUTOR=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
     -d '{"username":"dana","password":"tutor-dev"}' | jq -r .accessToken)

curl -i -X POST localhost:8080/api/v1/lessons -H 'Content-Type: application/json' \
     -d '{"businessKey":"LES-2026-0100","subject":"Algebra"}'                         # 401, no token
curl -i -X POST localhost:8080/api/v1/lessons -H "Authorization: Bearer $STUDENT" \
     -H 'Content-Type: application/json' -d '{"businessKey":"LES-2026-0100","subject":"Algebra"}'  # 201 + Location
curl -i -X POST localhost:8080/api/v1/lessons -H "Authorization: Bearer $STUDENT" -H 'X-Correlation-Id: demo-1' \
     -H 'Content-Type: application/json' -d '{"businessKey":"LES-2026-0100","subject":"Algebra"}'  # 409, correlationId demo-1
curl -i localhost:8080/api/me -H "Authorization: Bearer $STUDENT"                   # 200 aliya, ROLE_STUDENT
curl -i -X POST localhost:8080/api/v1/lessons/<uuid>/confirm -H "Authorization: Bearer $STUDENT"  # 403, still REQUESTED
curl -i -X POST localhost:8080/api/v1/lessons/<uuid>/confirm -H "Authorization: Bearer $TUTOR"    # 200 CONFIRMED
curl -i localhost:8080/actuator/health                                              # 200 {"status":"UP"}
curl -i localhost:8080/actuator/env                                                 # 401
```

The contract is at `http://localhost:8080/swagger-ui.html`.

Production uses the `prod` profile and reads everything that varies from the environment:

```text
SPRING_PROFILES_ACTIVE=prod \
CSS_DB_URL=jdbc:postgresql://<host>:5432/<db> CSS_DB_USER=<user> CSS_DB_PASSWORD=<password> \
CSS_DOWNSTREAM_URL=https://<payment-provider> CSS_JWT_SECRET=<32+ random characters> \
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
