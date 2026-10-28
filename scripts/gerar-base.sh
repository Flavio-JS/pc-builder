#!/usr/bin/env bash
# Gera src/main/resources/base/componentes.json a partir do catalogo OpenDB.
# Precisa que a pasta buildcores-open-db esteja ao lado de pc-builder.
set -e
cd "$(dirname "$0")/.."

./scripts/compilar.sh

mkdir -p build/tools
find tools/java -name '*.java' > build/fontes-tools.txt
javac -encoding UTF-8 -cp build/classes -d build/tools @build/fontes-tools.txt

# Windows usa ';' como separador de classpath; Linux/macOS usam ':'.
SEPARADOR=":"
case "$(uname -s)" in
    MINGW* | MSYS* | CYGWIN*) SEPARADOR=";" ;;
esac

java -cp "build/classes${SEPARADOR}build/tools" pcbuilder.ferramentas.ConversorOpenDb \
     --entrada ../buildcores-open-db/open-db \
     --saida src/main/resources/base/componentes.json

# recopia o recurso recem-gerado para o classpath
./scripts/compilar.sh

echo "Base gerada em src/main/resources/base/componentes.json"
