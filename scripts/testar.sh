#!/usr/bin/env bash
# Compila tudo e roda os testes da HU-001 (sem JUnit: o runner tem metodo main).
set -e
cd "$(dirname "$0")/.."

./scripts/compilar.sh

# Windows usa ';' como separador de classpath; Linux/macOS usam ':'.
SEPARADOR=":"
case "$(uname -s)" in
    MINGW* | MSYS* | CYGWIN*) SEPARADOR=";" ;;
esac

java -cp "build/classes${SEPARADOR}build/classes-test" pcbuilder.teste.ValidadorCadastroTeste
java -cp "build/classes${SEPARADOR}build/classes-test" pcbuilder.teste.MontagemGuiadaTeste
