@echo off
REM ================================================================
REM SCRIPT DE LANCEMENT DU SERVEUR RELAI MULTICAST
REM ================================================================
REM
REM Ce script lance le serveur relai multicast avec interface graphique.
REM
REM PREREQUIS:
REM - Java JDK 11 ou superieur installe
REM - Projet compile (mvn compile)
REM
REM UTILISATION:
REM - Double-cliquer sur ce fichier
REM - Ou executer depuis CMD: run-multicast-server.bat
REM
REM NOTE:
REM Le serveur relai est optionnel en multicast.
REM Les clients peuvent communiquer directement sans serveur.
REM Le serveur sert principalement au monitoring du trafic.
REM
REM ================================================================

echo.
echo ========================================
echo  DEMARRAGE DU SERVEUR RELAI MULTICAST
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

REM Lancement du serveur relai multicast
echo Lancement du serveur relai multicast...
echo.
java -cp "target\classes" multicast.ServeurGUI

REM En cas d'erreur
if errorlevel 1 (
    echo.
    echo ERREUR: Le serveur relai multicast n'a pas pu demarrer.
    echo Verifiez que Java est installe et que le projet est compile.
    echo.
    pause
)

