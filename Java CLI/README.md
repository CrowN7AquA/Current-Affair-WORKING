# NOWLINE – Java CLI Current Affairs Portal

A console-based current-affairs portal built with Java 17, MySQL and JDBC.
It includes account registration/login, news categories, search, bookmarks,
personal news interests, daily briefing, category quizzes and a small
producer-consumer demonstration.

## Requirements
- JDK 17 or newer
- Maven
- MySQL Server
- VS Code with Extension Pack for Java (recommended)

## 1. Create the database
Open MySQL Workbench or the MySQL terminal and run:

```sql
SOURCE /full/path/to/NOWLINE/sql/nowline_db.sql;
```

Or open `sql/nowline_db.sql` and execute its contents.

## 2. Configure database credentials
The default connection is:
- URL: jdbc:mysql://localhost:3306/nowline_db?serverTimezone=UTC
- User: root
- Password: empty

Set environment variables if your MySQL setup differs.

Windows PowerShell (current terminal):
```powershell
$env:NOWLINE_DB_URL="jdbc:mysql://localhost:3306/nowline_db?serverTimezone=UTC"
$env:NOWLINE_DB_USER="root"
$env:NOWLINE_DB_PASSWORD="your_mysql_password"
```

## 3. Configure live news (optional)
Create a NewsAPI account and obtain an API key. Then set:

```powershell
$env:NEWS_API_KEY="your_api_key"
```

Without a key, the application shows clearly-labelled demo articles.
Live API access depends on the provider's current plan and restrictions.
The free NewsAPI developer plan may not be suitable for a deployed public app.

## 4. Run from VS Code terminal
Open the NOWLINE folder in VS Code, then run:

```powershell
mvn clean compile
mvn exec:java -Dexec.mainClass="com.nowline.Main"
```

If Maven says the exec plugin is unavailable, use:
```powershell
mvn org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass="com.nowline.Main"
```

## Current implementation notes
- Start -> Login/Register/Exit -> logged-in dashboard.
- User-specific bookmarks, preferences and quiz attempts are stored using user IDs.
- News is shared; user-specific records are separated by user ID.
- Source, publication time and original URL are shown with fetched articles.
- News importance labels are simple keyword-based rules, not AI verification.
- Quiz questions are starter/sample questions. They are not automatically generated from the live news feed yet.
- The `NewsProcessor` class demonstrates a bounded producer-consumer queue and ExecutorService. It is a milestone demonstration, not yet wired to the live API refresh pipeline.
- Milestone 3 synchronization is represented by safe blocking-queue coordination in this starter. Expand the concurrency demonstration and test it with your faculty's exact checklist before submission.
- This starter implements the requested Milestones 1–5 direction in a compact form, but it is not a complete replacement for milestone evidence, testing, class diagrams or your own explanation.

## Security note
This is a learning project. SHA-256 is used as a simple password hash for demonstration.
For a real commercial application, use a password-specific algorithm such as BCrypt or Argon2,
secure secrets outside source code, add account recovery and review API/provider terms.

## Suggested next work
1. Add more quiz questions and maintain dated current-affairs question sets.
2. Connect the producer-consumer queue to actual fetched article processing.
3. Add update/delete operations for article/admin management if required.
4. Add unit tests and record real test results.
5. Capture screenshots and update the CA3 PPT from the actual running project.

## VS Code Run button (updated)
Open the folder containing pom.xml, install Extension Pack for Java, wait for Maven dependency import, open src/main/java/com/nowline/Main.java and click Run above main (or F5 and Run NOWLINE). Enter your MySQL password in the integrated terminal. Run sql/nowline_db.sql in Workbench first; it no longer drops existing data. The password prompt is visible in the terminal, so do not share screenshots containing it. Live news is optional; demo articles can now be bookmarked. Menu 15 demonstrates queue processing. Maven dependencies require internet on first import.
