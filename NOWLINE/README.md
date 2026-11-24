# NOWLINE — Java/MySQL integration prototype
Original React/CSS/assets retained. This is a development handoff, not a verified final submission.

1. Open MySQL Workbench or CLI and execute database/schema.sql (CLI: source C:/path/to/NOWLINE/database/schema.sql).
2. Download MySQL Connector/J from https://dev.mysql.com/downloads/connector/j/ and extract its JAR. Rename the JAR mysql-connector-j.jar and place in backend/.
3. Run START_BACKEND.bat and enter local database credentials. Keep terminal open.
4. Run START_FRONTEND.bat; open the URL printed by Vite. Node.js compatible with the original Vite version is required.
5. Register a new account with a password of at least eight characters. Old Firebase accounts are not migrated. Google/Apple login and email recovery are not configured.
6. Test vote, newsletter and simulated donation. No money is collected. News/dispatches and campaign cards still use original demonstration data; do not represent them as verified live news.

## Evidence and limitations
Java source compiled with JDK 21 in the build environment. MySQL integration and browser runtime have not been tested here. CSS files are unchanged. JDBC, prepared statements, transaction rollback, synchronized donation processing, HashMap sessions, custom exception and fixed thread pool are implemented. Full DAO separation, producer-consumer, Callable/Future, article CRUD and full syllabus coverage remain student implementation tasks. Record actual test results and meaningful contributions before submitting; the CA3 notice requires them. Do not claim untested results.

Suggested tests: valid registration, duplicate email, wrong password, duplicate vote, restart and log in again, newsletter duplicate, invalid donation, successful donation and compare SUM(amount) with donation_total.total. Test simultaneous donations from two accounts. Record actual outcomes and screenshots.
