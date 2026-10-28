@echo off
rem Compila tudo e roda os testes da HU-001.
setlocal
cd /d "%~dp0.."

call scripts\compilar.bat
if errorlevel 1 exit /b 1

java -cp "build\classes;build\classes-test" pcbuilder.teste.ValidadorCadastroTeste
