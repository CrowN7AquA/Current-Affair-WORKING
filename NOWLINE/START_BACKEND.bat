@echo off
cd /d "%~dp0"
if not exist backend\mysql-connector-j.jar (
 echo Place MySQL Connector/J jar at backend\mysql-connector-j.jar first.
 pause
 exit /b 1
)
set /p DB_USER=MySQL username (root): 
if "%DB_USER%"=="" set DB_USER=root
set /p DB_PASSWORD=MySQL password: 
javac -d backend\out backend\NowlineServer.java
if errorlevel 1 exit /b 1
java -cp "backend\out;backend\mysql-connector-j.jar" NowlineServer
pause
