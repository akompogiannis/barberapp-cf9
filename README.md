# Barber Appointment App

My final project for Coding Factory 9 (AUEB).

It is a booking app for a freelance barber. Customers can look at the shop page,
see the services and any discount running that week, pick a free time and book it.
The barber logs in to the same app and manages his services, his weekly schedule,
his days off, his promotions, and the diary of appointments.

Spring Boot REST API on the back, React on the front, MySQL underneath.

## Tech

- Java 21, Spring Boot 3.5, Gradle
- Spring Data JPA + MySQL 8, with Flyway owning the schema
- Spring Security 6 with JWT, BCrypt for passwords
- springdoc-openapi for the Swagger documentation
- React 18 + Vite + Axios on the front end
- JUnit 5, Mockito and AssertJ for the tests

## How to run it

You need JDK 21, Node 18+, and MySQL 8.

### 1. The database

Easiest way is Docker, since the container is already set up with the same
database name and password the app expects:

```bash
docker compose up -d
```

If you would rather use a MySQL you already have installed, create the database
and the user once:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS barberapp CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE USER IF NOT EXISTS 'barberuser'@'localhost' IDENTIFIED BY '12345'; GRANT ALL PRIVILEGES ON barberapp.* TO 'barberuser'@'localhost'; FLUSH PRIVILEGES;"
```

Do not create any tables yourself. Flyway builds the whole schema the first time
the app starts, and it also inserts the demo data (the barber, one customer, four
services, the opening hours and one promotion).

### 2. The back end

```bash
cd backend && ./gradlew bootRun
```

It runs on <http://localhost:8080>. On Windows use `gradlew.bat bootRun`.

### 3. The front end

```bash
cd frontend && npm install && npm run dev
```

It runs on <http://localhost:5173>, which is already allowed in the backend's CORS
settings, so there is nothing else to configure.

### Accounts to log in with

| Username | Password | Who |
|---|---|---|
| `barber` | `barber12345` | the barber (admin) |
| `customer` | `customer12345` | a customer |

These are only seeded for the `dev` profile. They would obviously be removed before
putting this anywhere real.

## API documentation

With the backend running, the Swagger UI is at
<http://localhost:8080/swagger-ui.html>.

Every endpoint is documented there. To try the secured ones, call
`POST /api/v1/auth/authenticate` first, copy the token, press **Authorize** at the
top right and paste it in.

There is also a Postman collection in `postman/` if you prefer that. Import it and
run the "Authenticate" request first — it saves the token automatically, so all the
other requests just work. It has assertions on the happy paths and on the error
cases too (403 when a customer tries to reach the diary, 409 when a slot is already
taken, and so on).

## How the availability works

This was the part that took the most thinking, so it is worth a short explanation.

Free slots are never stored anywhere. They are calculated every time from three
things: the barber's weekly working hours, the appointments already booked, and any
time off he has registered. So if he changes his hours or a customer cancels, the
next search already shows the right thing — there is no list of slots to keep in
sync.

The calculation itself lives in `SlotCalculator`, which is a plain class with static
methods and no Spring or database in it. I did it that way so I could unit test the
arithmetic properly, and it is where most of the tests point.

A few rules that are easy to get wrong and that I handled explicitly:

- An appointment that ends at 10:00 leaves 10:00 free for the next one.
- A service has to fit fully inside one working block. If the barber works
  09:00–14:00 and then 17:00–21:00, a 50 minute service is not offered at 13:30,
  because it would run past closing.
- The app tells the difference between "closed today" and "open but fully booked",
  since those are two different messages for the customer.
- Two people can be looking at the same free slot at the same time. So when a
  booking is saved, the app checks again for a clash inside the same transaction,
  right before the insert. Whoever is second gets a 409 and is asked to pick
  another time, instead of both of them getting the same chair.

The price is also decided by the server, not sent by the client, and it is stored on
the appointment. That way a promotion expiring next week does not silently change
the price of something already booked.

## Login and permissions

Login returns a JWT, and every request after that sends it in the `Authorization`
header.

Permissions are not just "admin or not". Each role holds a set of capabilities
(`BOOK_APPOINTMENT`, `MANAGE_SERVICES`, `VIEW_APPOINTMENTS`, and so on) and those
are what get checked. The barber has all of them; a customer has the four he needs
to book and manage his own appointments.

They are checked in three places:

1. On the URLs, in `SecurityConfiguration`.
2. On the service methods, with `@PreAuthorize`.
3. On ownership, so a customer holding `CANCEL_OWN_APPOINTMENT` still cannot cancel
   somebody else's booking by guessing a uuid — he gets a 403.

The React side reads the same capability names to decide which menu items and
buttons to show, but that is only about what is displayed. The server checks
everything again on every request.

## Tests

```bash
cd backend && ./gradlew test
```

60 unit tests. Most of them are on the slot calculation and the availability
service, because that is where a bug would actually cost somebody an appointment —
things like the lunch break boundary, slots that have already passed today, and the
case where two people race for the last slot. The rest cover the appointment status
transitions and the discount arithmetic.

They do not need a database or a running server. The report ends up in
`backend/build/reports/tests/test/index.html`.

## Some notes on the design

A few decisions that are visible everywhere in the code:

- **Nothing is ever deleted.** Every entity extends `AbstractEntity`, which adds
  created/updated timestamps and a `deleted` flag. Retiring a service or closing an
  account must not take the appointment history with it.
- **URLs use UUIDs, not database ids**, so nothing can be enumerated by counting up.
- **Flyway owns the schema** and Hibernate is set to `validate`. If an entity and
  the SQL ever disagree, the app refuses to start instead of quietly changing a
  table.
- **Controllers never see entities.** Everything crossing the API is a DTO record,
  and there is one `Mapper` class doing the translation.
- **No try/catch in the controllers.** The services throw checked exceptions and a
  `@ControllerAdvice` turns them into the right status code.

## Building and deploying

To build both parts:

```bash
cd backend  && ./gradlew clean bootJar    # produces build/libs/barberapp.jar
cd frontend && npm run build              # produces dist/
```

The jar is self-contained, so you only need a JVM and a MySQL to run it. Nothing
sensitive is written into the files — the settings all come from environment
variables, with the dev defaults only used when nothing is set:

| Variable | What it is | Dev default |
|---|---|---|
| `MYSQL_HOST` / `MYSQL_PORT` | database host and port | `localhost` / `3306` |
| `MYSQL_DB` | database name | `barberapp` |
| `MYSQL_USER` / `MYSQL_PASSWORD` | database credentials | `barberuser` / `12345` |
| `JWT_SECRET` | signing key for the tokens | a dev value |
| `ALLOWED_ORIGINS` | origins allowed by CORS | `http://localhost:5173` |
| `BOOKING_MAX_DAYS_AHEAD` | how far ahead people can book | 60 days |

So a real deployment looks like this:

```bash
export MYSQL_HOST=db.example.com
export MYSQL_DB=barberapp
export MYSQL_USER=barberapp
export MYSQL_PASSWORD='...'
export JWT_SECRET='...'                   # base64, at least 256 bits
export ALLOWED_ORIGINS=https://barber.example.com

java -jar barberapp.jar --spring.profiles.active=prod
```

Flyway runs the migrations on startup, so a fresh database is set up by just
starting the app. The `frontend/dist/` folder is static files and can be served by
nginx or any static host, with `VITE_API_BASE_URL` pointing at wherever the API is.

Two things the `prod` profile does on purpose:

- `JWT_SECRET` has no default, so the app will not start on the development secret
  that is committed in this repository.
- Swagger is switched off unless you set `SWAGGER_ENABLED=true`.

## Project structure

```
barberapp/
├── compose.yaml          MySQL for local development
├── postman/              the API collection
├── backend/
│   └── src/main/java/gr/aueb/cf/barberapp/
│       ├── api/          the REST controllers
│       ├── service/      the business logic
│       ├── repository/   Spring Data JPA
│       ├── model/        the JPA entities
│       ├── dto/          the records the API speaks in
│       ├── mapper/       entity to DTO
│       ├── security/     JWT filter, security config, ownership checks
│       ├── authentication/  login and token handling
│       ├── core/         error handling, Swagger config, SlotCalculator
│       ├── specification/   the filters for the diary
│       └── validator/    the cross-field validation
└── frontend/src/
    ├── api/              axios client and the endpoint calls
    ├── auth/             the login context and route guards
    ├── pages/            the screens, with admin/ for the barber's ones
    └── components/       the shared bits
```
