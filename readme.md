## Bed Allocation System

JavaFX Mini Project – Object-Oriented Programming

This is a simple JavaFX application to manage bed allocation in a hospital. It demonstrates OOP concepts and database integration via JDBC.

Project structure (important files):

```
icu-bed-mvp/
├─ bed.sql                # SQL script to create the `bed` database and initial beds
├─ pom.xml                # Maven build
└─ src/main/java/com/icu/ # Java source (UI + DB manager)
   ├─ Main.java
   ├─ DBManager.java
   ├─ Patient.java
   └─ Bed.java
```

Key files:
- `bed.sql` – Creates the `bed` database, `patients` and `beds` tables and inserts initial beds.
- `DBManager.java` – JDBC code for DB operations.
- `Main.java` – JavaFX application.

Setup
1. Clone the repository:

```powershell
git clone https://github.com/msnikash/icu-bed-mvp.git
cd icu-bed-mvp
```

2. Create the database (use MySQL Workbench or CLI):

- Open `bed.sql` and run the script to create the `bed` database and tables.

3. Configure DB credentials: open `DBManager.java` and set the `DB_URL`, `DB_USER`, and `DB_PASS` to match your MySQL setup (default URL uses `jdbc:mysql://localhost:3306/bed`).

4. Build and run:

```powershell
mvn clean compile
mvn javafx:run
```

Features
- Add patients (name, age, priority) with start/end dates.
- Automatic bed allocation (priority-aware).
- Discharge patients and generate a discharge receipt.

Notes
- The project uses the `bed` database name and `BED-` prefixed bed numbers in the sample SQL.
- If you previously used `icu.sql`, the new file is `bed.sql`.

