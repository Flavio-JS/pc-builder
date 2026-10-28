@echo off
rem Executa a aplicacao. Ex.: scripts\executar.bat --validar-base
setlocal
cd /d "%~dp0.."

if not exist build\classes\pcbuilder\Main.class call scripts\compilar.bat

java -cp build\classes pcbuilder.Main %*
