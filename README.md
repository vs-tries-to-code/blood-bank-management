# Blood Bank Management System

A Java Swing desktop application for coordinating core blood-bank and hospital workflows. It provides role-based screens for blood-bank administrators and hospital users, with MySQL persistence for accounts, inventory, hospital requests, patients, and reservations.

## Benefits

- Gives staff a shared view of blood units, blood groups, components, expiry dates, and availability.
- Lets hospitals submit blood requests and review their request status.
- Gives blood-bank administrators tools to add inventory and approve or reject hospital requests.
- Keeps common blood groups and blood components represented consistently through enums and domain models.
- Runs locally as a desktop application; it does not require a web server.

The project is a prototype. Donation records are persisted for existing donors, but donor registration is not yet integrated into the application, and reservation handling is not yet transactionally integrated with inventory. Compatibility rules require clinical review before operational use.

## Technology

- Java 21
- Swing
- MySQL and MySQL Connector/J
- Maven
- BCrypt password hashing (jBCrypt)
- JUnit 4

## Prerequisites

Install:

- JDK 21 or newer
- Maven 3.8 or newer
- MySQL 8 or a compatible MySQL server

Check Java and Maven are available:

```bash
java -version
mvn -version
```

## Installation and Setup

1. Clone or download this repository, then open a terminal in the project root (the directory containing `pom.xml`).

2. Start the local MySQL server. Review `src/main/java/com/bloodbank/common/DatabaseConnection.java`; the current settings are `localhost:3306`, database `blood_bank_db`, username `root`, and password `password`. Change those constants to match your local MySQL account if needed.

3. Create the database and tables by running the included schema:

   ```bash
   mysql -u root -p < src/main/resources/schema.sql
   ```

   Use a MySQL account with permission to create the database and tables. The script creates the `blood_bank_db` database and tables for users, inventory, patients, reservations, hospital requests, donors, and donation records.

4. Add at least one user to the `users` table. The application has no account-creation screen, and the demo users in `InMemoryUserRepository` are not used by the normal SQL-backed application. The SQL-backed login expects a BCrypt hash in the `password` column; inserting a plaintext password will not work.

   Compile the app and generate a BCrypt hash from JShell:

   ```bash
   mvn -q -DskipTests compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
   jshell --class-path "target/classes:$(cat target/classpath.txt)"
   ```

   At the JShell prompt, run this with a password you choose locally:

   ```java
   import com.bloodbank.auth.PasswordHasher;
   PasswordHasher.hash("choose-a-local-password");
   ```

   Copy the returned hash into an insert such as this, replacing the placeholder with that hash:

   ```sql
   INSERT INTO users (username, password, role)
   VALUES ('admin', '<bcrypt-hash>', 'ADMIN');
   ```

   Create hospital accounts with role `HOSPITAL`. A hospital's username is also used as its hospital name when submitting and listing requests. Generate a separate BCrypt hash for each account.

5. Build and run tests:

   ```bash
   mvn clean package
   mvn test
   ```

6. Launch the desktop application:

   ```bash
   mvn exec:java
   ```

   The application opens at the login screen. It connects to the MySQL database when its database-backed screens are initialized or used.

## Roles and Workflows

### Administrator (`ADMIN`)

- View the admin dashboard.
- View inventory and add blood units.
- View hospital requests and approve or reject them.
- Open donation history and record donations for existing donors. Records are saved to MySQL and reloaded from the database.

### Hospital (`HOSPITAL`)

- View the inventory in read-only mode.
- Submit blood requests for a group, component, unit count, and urgency.
- View requests associated with the hospital account username.

The current role checks are enforced in the desktop UI. This is appropriate for a local prototype, but it is not a security boundary against someone who can modify the application or connect directly to the database.

## Database Notes

`src/main/resources/schema.sql` is a setup script, not an automatic migration: run it manually before first use. It uses `CREATE TABLE IF NOT EXISTS`, so it does not update existing tables. To add a donation, first create its donor row in `donors` and use that donor's ID; the application generates the donation ID and saves the donor reference, date, and quantity in `donation_records`.

Database credentials are currently hard-coded in `DatabaseConnection.java`. For a shared or deployed environment, move credentials to external configuration and avoid committing real secrets.

## Project Layout

```text
src/main/java/com/bloodbank/
  auth/       Login, users, authentication, user repositories, BCrypt helper
  common/     Database connection, generic repository, role enum
  dashboard/  Main window, dashboards, theme, placeholder panel

src/main/java/org/yourcompany/yourproject/
  hospital/                  Hospital requests and admin review
  inventory/                 Blood units, types, statuses, inventory UI and manager
  compatibility_reservation/ Patients, compatibility rules, reservations
  donation/                  Donor and donation models, donation history UI
  BloodBank.java             Application entry point

src/main/resources/
  schema.sql                 MySQL database and table creation script

src/test/java/
  com/bloodbank/auth/        Authentication tests
```

## Donate Blood, Save Lives!
If you are eligible, please consider donating blood through a licensed local blood center. Donated blood can make a meaningful difference to patients who need it.
