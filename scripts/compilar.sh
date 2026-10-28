#!/usr/bin/env bash
# Compila o codigo principal e os testes para build/classes e build/classes-test.
set -e
cd "$(dirname "$0")/.."

rm -rf build/classes build/classes-test
mkdir -p build/classes build/classes-test

find src/main/java -name '*.java' > build/fontes-main.txt
javac -encoding UTF-8 -d build/classes @build/fontes-main.txt

# Os recursos (base/componentes.json) precisam estar no classpath
if [ -d src/main/resources ]; then
    cp -r src/main/resources/. build/classes/
fi

find src/test/java -name '*.java' > build/fontes-test.txt
javac -encoding UTF-8 -cp build/classes -d build/classes-test @build/fontes-test.txt

echo "Compilado: build/classes (aplicacao) e build/classes-test (testes)."
