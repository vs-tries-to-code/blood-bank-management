# Blood Bank Management System: Project Report

## 1. Overview

This project is a Java 21 desktop application for blood-bank operations. Its user interface is built with Swing, and its database access uses JDBC with MySQL Connector/J. The main application supports two roles: `ADMIN` for blood-bank staff and `HOSPITAL` for hospital users.

The implemented workflows cover login, viewing and adding blood inventory, hospital blood requests, and administrator review of those requests. Supporting code also models patients, component compatibility, reservations, donors, and donation history. Several of those supporting modules are not yet integrated into the primary application or do not persist their data.

## 2. Technology and Startup

- Java 21, as configured by Maven's `maven.compiler.release` property.
- Swing for the desktop UI.
- MySQL Connector/J 8.3.0 for JDBC persistence.
- jBCrypt 0.4 for password hashing.
- JUnit 4.13.2 for tests.

The Maven entry point is `org.yourcompany.yourproject.BloodBank`. Its `main` method schedules `MainFrame` on Swing's event-dispatch thread. `MainFrame` is the primary `JFrame` and uses a `CardLayout` to switch between login, dashboards, inventory, requests, and donations.

Build and test with:

```bash
mvn clean package
mvn test
```

## 3. Architecture

The source has two Java package roots:

- `com.bloodbank`: authentication, shared database/repository abstractions, roles, and the main dashboards.
- `org.yourcompany.yourproject`: the application entry point and blood-bank feature modules for inventory, hospital requests, reservations, patients, and donations.

The main UI constructs repository and feature objects directly. JDBC repositories obtain connections through `DatabaseConnection`; there is no separate server/API tier. The Swing client connects directly to MySQL.

### Main classes

- `BloodBank`: application entry point; starts `MainFrame` on Swing's event thread.
- `MainFrame`: owns the main window, current logged-in user, role dashboards, and screen navigation.
- `AuthenticationService`: finds a user by username and verifies a supplied password against the stored BCrypt hash.
- `Repository<T, ID>`: generic CRUD interface implemented by the user repositories.
- `DatabaseConnection`: creates MySQL JDBC connections.
- `HospitalRequestRepository`: feature-specific persistence interface, with JDBC and in-memory implementations.

## 4. Functional Areas

### Authentication and roles

The login form accepts a username and password and enables submission when both are non-empty. Password complexity is not enforced during login. `AuthenticationService` looks up the user and verifies the password with BCrypt.

`User` stores a password hash. A normal `User` constructor hashes a raw password, while `User.fromPasswordHash` is used when rebuilding a user from a database value that is already hashed. The in-memory repository creates demo users with hashed passwords. The current main application wiring uses `MySQLUserRepository`, not the in-memory repository.

There is no account-creation or password-reset workflow. Existing database rows that contain plaintext passwords will not pass BCrypt verification; those records need a one-time migration or reset. The database schema's password column must be wide enough for a BCrypt hash (at least 60 characters).

### Admin dashboard

The administrator dashboard provides navigation to inventory, donation history, and hospital requests. Its summary cards and blood-group/component availability matrix are loaded from MySQL. The hospital dashboard displays the same availability matrix and its own request count. Metrics load asynchronously and can be refreshed; dashboard values are refreshed when returning from inventory or requests.

Admins can add a blood unit from the inventory screen and approve or reject hospital requests from the request screen.

### Hospital dashboard and requests

The hospital dashboard provides inventory viewing and a request workflow. The request form takes its hospital identity from the logged-in username, collects blood group, component, units, and urgency, and rejects missing, non-numeric, or non-positive unit counts. A hospital sees requests filtered by its hospital name.

The administrator request screen lists all requests and supports setting their status to `APPROVED` or `REJECTED`. Request data is persisted through `JdbcHospitalRequestRepository` using the `hospital_requests` table.

The JDBC repository catches SQL exceptions and prints stack traces rather than reporting failures through its interface. As a result, the request form can display a success message even if a database write failed.

### Inventory

Inventory records use `BloodGroup`, `BloodComponent`, and `BloodStatus` enums. A blood unit stores an identifier, group, component, donor phone, collection date, expiry date, and status. Shelf life is calculated from the component: whole blood 35 days, PRBC 42 days, platelets 5 days, and FPP 365 days.

`InventoryManager` reads from and inserts into the `blood_inventory` table. The inventory screen lists units and computes displayed expiry dates from the collection date and component. The add form is shown for admins and hidden for hospitals; the add handler also checks that the logged-in user has the `ADMIN` role. The main window restricts inventory navigation to logged-in users and restricts the admin-request screen to admins.

These are desktop-client checks. `InventoryManager.addBloodUnit` itself has no user/role parameter, and users with direct database access can bypass the UI restrictions.

### Patient registration and compatibility

`PatientPanel` is a separate Swing window for registering a patient with name, blood group, contact number, and hospital. `MySQLPatientRepository` implements patient CRUD against a `patients` table. The patient screen has its own `main` method and is not linked from the primary dashboard.

`CompatibilityChecker` contains compatibility rules for whole blood, PRBC, FPP, and platelets. It only considers units whose status is `AVAILABLE`. Whole-blood and platelet compatibility use exact group matching; PRBC and FPP use separate ABO/Rh rules. These rules are application code and should be clinically validated before operational use.

### Reservations

`ReservationPanel` is another standalone Swing window. It accepts a patient ID, recipient blood group, and component, searches inventory for a compatible unit, and attempts to save a row in the `reservations` table.

The reservation flow currently changes a selected `BloodUnit` to `RESERVED` only in memory. It does not atomically update that unit in the database, so another reservation may select the same unit. The panel's cancel action is a placeholder; although the repository can mark a reservation `CANCELLED`, the panel does not call it or release the inventory unit.

### Donations

`DonationHistoryUI` is a panel available from the administrator dashboard. It accepts an existing donor ID, donation date, and quantity, then uses `MySQLDonationRepository` to insert the record into `donation_records`. The donation ID is generated by MySQL; donor name and blood group are loaded by joining the record to `donors`. The UI reloads persisted records after saving. Donor registration is not integrated into this screen, so the donor must already exist in the database.

## 5. Database Expectations

The project includes `src/main/resources/schema.sql`, which creates a local MySQL database named `blood_bank_db` and defines these tables:

- `users`
- `blood_inventory`
- `patients`
- `reservations`
- `hospital_requests`
- `donors`
- `donation_records`

All seven tables are used by or reserved for the application; `donors` and `donation_records` now support persisted donation history. The schema file is a setup script; the application does not execute it automatically, so it must be run against MySQL before using the database-backed features. There are no migration scripts for evolving an existing database.

`DatabaseConnection` currently hard-codes `localhost:3306`, user `root`, and password `password`. These are local development settings and should match the developer's MySQL installation.

## 6. Current Scope and Limitations

- No user-registration or password-reset feature exists; account provisioning is external.
- The schema must be initialized manually from `src/main/resources/schema.sql`; local database credentials must also be configured to match `DatabaseConnection`.
- Authentication uses BCrypt, but legacy plaintext database passwords require migration or reset.
- UI role checks do not protect direct database access or direct calls to data-layer methods.
- Several feature screens still perform database access synchronously from Swing actions and may block the UI; dashboard metric queries run in `SwingWorker` background tasks.
- JDBC repositories generally print exceptions and return empty/null/false results; callers do not always distinguish database failure from absent data or success.
- Donation records require a donor row to exist first; donor registration is not currently available in the main application.
- Patient and reservation windows are separate from the main application navigation.
- Reservation state is not transactionally coupled to inventory state, and cancellation is incomplete.
- The compatibility rules and inventory lifetimes need domain validation before real-world use.
- The test suite currently has a focused authentication test for BCrypt hashing and login success/failure; inventory, request, reservation, and Swing navigation behavior lack automated tests.

## 7. Summary

The project is a functional Swing/MySQL prototype with a connected login flow, role-specific dashboards, admin-only inventory editing in the UI, persisted hospital requests, and donation records linked to existing donors. Its architecture is compact and direct: UI panels call services and repositories, which use JDBC. Patient registration and reservation persistence exist as standalone support windows, but reservation integrity and cancellation are incomplete. Database initialization is documented in the schema file but remains a manual step.
