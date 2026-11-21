@echo off
rem Compila tudo e roda os testes da HU-001.
setlocal
cd /d "%~dp0.."

call scripts\compilar.bat
if errorlevel 1 exit /b 1

java -cp "build\classes;build\classes-test" pcbuilder.teste.ValidadorCadastroTeste
if errorlevel 1 exit /b 1

java -cp "build\classes;build\classes-test" pcbuilder.teste.MontagemGuiadaTeste
if errorlevel 1 exit /b 1

java -cp "build\classes;build\classes-test" pcbuilder.teste.FiltroEtapaTeste
if errorlevel 1 exit /b 1

java -cp "build\classes;build\classes-test" pcbuilder.teste.LimitacoesTeste
if errorlevel 1 exit /b 1

java -cp "build\classes;build\classes-test" pcbuilder.teste.EtapaPlacaMaeTeste
if errorlevel 1 exit /b 1

java -cp "build\classes;build\classes-test" pcbuilder.teste.EtapaMemoriaTeste
if errorlevel 1 exit /b 1

java -cp "build\classes;build\classes-test" pcbuilder.teste.EtapaPlacaVideoTeste
if errorlevel 1 exit /b 1

java -cp "build\classes;build\classes-test" pcbuilder.teste.EtapaArmazenamentoTeste
if errorlevel 1 exit /b 1
