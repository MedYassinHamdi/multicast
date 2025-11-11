@echo off
REM ================================================================
REM SCRIPT DE LANCEMENT DU SERVEUR UDP
REM ================================================================
REM
REM Ce script lance le serveur UDP avec interface graphique.
REM
REM PREREQUIS:
REM - Java JDK 11 ou superieur installe
REM - Projet compile (mvn compile)
REM
REM UTILISATION:
REM - Double-cliquer sur ce fichier
REM - Ou executer depuis CMD: run-udp-server.bat
REM
REM ================================================================

echo.
echo ========================================
echo    DEMARRAGE DU SERVEUR UDP
echo ========================================
echo.

REM Verification que le dossier target existe
if not exist "target\classes" (
    echo ERREUR: Le projet n'est pas compile.
    echo Veuillez executer: mvn compile
    echo.
    pause
    exit /b 1
)

REM Lancement du serveur UDP
echo Lancement du serveur UDP...
echo.
java -cp "target\classes" udp.ServeurGUI

REM En cas d'erreur
if errorlevel 1 (
    echo.
    echo ERREUR: Le serveur UDP n'a pas pu demarrer.
    echo Verifiez que Java est installe et que le projet est compile.
    echo.
    pause
)

