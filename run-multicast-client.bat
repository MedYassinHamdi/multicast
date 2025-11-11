@echo off
REM ================================================================
REM SCRIPT DE LANCEMENT DU CLIENT MULTICAST
REM ================================================================
REM
REM Ce script lance le client multicast avec interface graphique.
REM
REM PREREQUIS:
REM - Java JDK 11 ou superieur installe
REM - Projet compile (mvn compile)
REM
REM UTILISATION:
REM - Double-cliquer sur ce fichier
REM - Ou executer depuis CMD: run-multicast-client.bat
REM - Vous pouvez lancer plusieurs instances pour simuler plusieurs clients
REM
REM ================================================================

echo.
echo ========================================
echo    DEMARRAGE DU CLIENT MULTICAST
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

REM Lancement du client multicast
echo Lancement du client multicast...
echo.
java -cp "target\classes" multicast.Client

REM En cas d'erreur
if errorlevel 1 (
    echo.
    echo ERREUR: Le client multicast n'a pas pu demarrer.
    echo Verifiez que Java est installe et que le projet est compile.
    echo.
    pause
)

