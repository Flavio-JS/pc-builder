@echo off
rem Compila o codigo principal e os testes (Windows: use no Prompt de Comando).
rem Observacao: usamos -sourcepath em vez de um arquivo de argumentos porque o
rem caminho do projeto pode conter caracteres fora do UTF-8 (ex.: "4o periodo").
setlocal
cd /d "%~dp0.."

if exist build\classes rmdir /s /q build\classes
if exist build\classes-test rmdir /s /q build\classes-test
mkdir build\classes
mkdir build\classes-test

javac -encoding UTF-8 -sourcepath src\main\java -d build\classes src\main\java\pcbuilder\Main.java
if errorlevel 1 exit /b 1

rem Os recursos (base/componentes.json) precisam estar no classpath
if exist src\main\resources xcopy /E /I /Y src\main\resources build\classes > nul

javac -encoding UTF-8 -sourcepath "src\main\java;src\test\java" -cp build\classes -d build\classes-test src\test\java\pcbuilder\teste\ValidadorCadastroTeste.java
if errorlevel 1 exit /b 1

echo Compilado: build\classes (aplicacao) e build\classes-test (testes).
