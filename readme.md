\# ICU Bed Allocation System



\*\*JavaFX Mini Project – Object-Oriented Programming\*\*



A simple JavaFX application to manage ICU bed allocation in a hospital. This project demonstrates object-oriented programming concepts such as classes, objects, encapsulation, and database integration using JDBC.



---



\## Project Structure



```

E:.

│   .gitattributes

│   .gitignore

│   icu.sql

│   pom.xml

│

├───src

│   └───main

│       └───java

│           └───com

│               └───icu

│                       Bed.java

│                       DBManager.java

│                       Main.java

│                       Patient.java

└───target

&nbsp;   └───... (compiled classes and Maven build files)

```



\*\*Key Files:\*\*



\* `icu.sql` – SQL script to create the database and tables with initial ICU beds.

\* `pom.xml` – Maven build file with dependencies and plugins.

\* `src/main/java/com/icu/` – JavaFX source code:



&nbsp; \* `Main.java` – Launches the GUI.

&nbsp; \* `DBManager.java` – Handles MySQL database operations.

&nbsp; \* `Patient.java` – Represents patient objects.

&nbsp; \* `Bed.java` – Represents ICU bed objects.



---



\## Features



\* Add patient details: name, age, and condition priority.

\* Automatically allocate available ICU beds based on patient priority.

\* View waiting patients, allocated patients, and free beds in real-time.

\* Uses MySQL for persistent storage.



---



\## Requirements



\* Java 17 or higher

\* Maven

\* MySQL 8.x or higher

\* JavaFX SDK 22 (configured via Maven dependencies)



---



\## Setup and Running



1\. \*\*Clone the repository:\*\*



```bash

git clone <your-repo-url>

cd icu-bed-mvp

```



2\. \*\*Setup the database:\*\*



\* Open MySQL Workbench (or any MySQL client).

\* Open the `icu.sql` file in Workbench.

\* Run the script to create the `icu` database, tables, and initial beds.



3\. \*\*Update database credentials:\*\*



\* Open `DBManager.java`.

\* Update the username, password, and URL to match your MySQL configuration:



```java

private final String url = "jdbc:mysql://localhost:3306/icu";

private final String user = "root";

private final String password = "your\_password";

```



4\. \*\*Compile and run using Maven:\*\*



```bash

mvn clean compile

mvn javafx:run

```



5\. \*\*Using the Application:\*\*



\* Add new patients with their name, age, and condition priority.

\* The system will automatically assign a free ICU bed based on priority.

\* View the list of waiting and allocated patients in the GUI.



---



\## Notes



\* Only ICU beds are supported in this MVP.

\* Beds are automatically allocated; no manual allocation required.

\* Make sure MySQL service is running before launching the app.



---



