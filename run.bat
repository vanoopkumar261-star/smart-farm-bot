@echo off
REM Compile the whole project and start the web dashboard. One command, no Maven/Gradle.
cd /d "%~dp0"

set SQLITE=lib\sqlite-jdbc-3.46.1.3.jar

if not exist out mkdir out
echo Compiling (Java 17)...
dir /s /b src\*.java > "%TEMP%\sfb_sources.txt"
javac --release 17 -cp "%SQLITE%" -d out @"%TEMP%\sfb_sources.txt"
del "%TEMP%\sfb_sources.txt"
if errorlevel 1 exit /b 1

echo Starting Smart Farm Bot on http://localhost:8080/login.html ...
java --enable-native-access=ALL-UNNAMED -cp "out;%SQLITE%" farmbot.web.ApiServer
