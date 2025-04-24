@echo off
echo Compiling Java files...

REM Create the bin directory if it doesn't exist
if not exist "bin" mkdir bin

REM Compile all Java files with SQLite JDBC in the classpath
javac -d bin -cp "src\lib\sqlite-jdbc-3.49.1.0.jar" src\common\*.java src\server\*.java

echo Starting Find Number Game Server...
java -cp "bin;src\lib\sqlite-jdbc-3.49.1.0.jar" server.GameServer
pause