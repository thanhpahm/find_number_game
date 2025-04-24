@echo off
echo Find the Number - Remote Client Launcher

REM Get server IP address from user
set /p SERVER_IP="Enter server IP address (default: localhost): "
if "%SERVER_IP%"=="" set SERVER_IP=localhost

REM Create the bin directory if it doesn't exist
if not exist "bin" mkdir bin

REM Compile all Java files with SQLite JDBC in the classpath
echo Compiling Java files...
javac -d bin -cp "src\lib\sqlite-jdbc-3.49.1.0.jar" src\common\*.java src\client\*.java

REM Launch the client with the specified server address
echo Starting Find Number Game Client...
java -cp "bin;src\lib\sqlite-jdbc-3.49.1.0.jar" client.GameClient %SERVER_IP% 12345
pause