@echo off
REM Compile and run the JUnit 5 test suite.
cd /d "%~dp0"

set SQLITE=lib\sqlite-jdbc-3.46.1.3.jar
set JUNIT=lib\junit-platform-console-standalone-1.11.3.jar

if not exist out mkdir out
if not exist out-test mkdir out-test

echo Compiling sources...
dir /s /b src\*.java > "%TEMP%\sfb_src.txt"
javac --release 17 -cp "%SQLITE%" -d out @"%TEMP%\sfb_src.txt"
del "%TEMP%\sfb_src.txt"
if errorlevel 1 exit /b 1

echo Compiling tests...
dir /s /b test\*.java > "%TEMP%\sfb_test.txt"
javac --release 17 -cp "out;%JUNIT%;%SQLITE%" -d out-test @"%TEMP%\sfb_test.txt"
del "%TEMP%\sfb_test.txt"
if errorlevel 1 exit /b 1

echo Running tests...
java --enable-native-access=ALL-UNNAMED -jar "%JUNIT%" execute --class-path "out;out-test;%SQLITE%" --scan-class-path
