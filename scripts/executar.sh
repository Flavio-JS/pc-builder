#!/usr/bin/env bash
# Executa a aplicacao. Argumentos sao repassados para pcbuilder.Main.
# Ex.: ./scripts/executar.sh --validar-base
set -e
cd "$(dirname "$0")/.."

if [ ! -d build/classes ]; then
    ./scripts/compilar.sh
fi

java -cp build/classes pcbuilder.Main "$@"
