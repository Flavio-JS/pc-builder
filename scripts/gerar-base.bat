@echo off
rem Gera src\main\resources\base\componentes.json a partir do catalogo OpenDB.
rem Precisa que a pasta buildcores-open-db esteja ao lado de pc-builder.
setlocal
cd /d "%~dp0.."

call scripts\compilar.bat
if errorlevel 1 exit /b 1

if not exist build\tools mkdir build\tools
javac -encoding UTF-8 -cp build\classes -sourcepath tools\java -d build\tools tools\java\pcbuilder\ferramentas\ConversorOpenDb.java
if errorlevel 1 exit /b 1

java -cp "build\classes;build\tools" pcbuilder.ferramentas.ConversorOpenDb --entrada ..\buildcores-open-db\open-db --saida src\main\resources\base\componentes.json
if errorlevel 1 exit /b 1

rem recopia o recurso recem-gerado para o classpath
call scripts\compilar.bat
if errorlevel 1 exit /b 1

echo Base gerada em src\main\resources\base\componentes.json
