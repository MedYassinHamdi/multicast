@echo off
echo ========================================
echo    Running Multicast Receiver
echo ========================================
echo You can run this multiple times!
echo Each instance creates a new receiver.
echo.
cd /d "%~dp0"
mvn exec:java -Dexec.mainClass="MulticastReceiver"
pause
@echo off
echo ========================================
echo    Running Multicast Sender
echo ========================================
echo.
cd /d "%~dp0"
mvn exec:java -Dexec.mainClass="MulticastSender"
pause

