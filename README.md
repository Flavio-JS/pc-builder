# PC Builder CLI

Aplicacao Java de **linha de comando** que ajuda um usuario leigo a montar um PC:
o programa filtra as pecas que encaixam, avisa o que esta limitando o desempenho e
mostra o resumo com os precos. A base tem **17.230 componentes** convertidos do
catalogo publico BuildCores OpenDB para um JSON no formato do nosso modelo de dados
(o catalogo original nao fica no repositorio — ver `docs/base-mockada.md`).

**Status:** apenas a **HU-001** (carregar e validar a base mockada) esta implementada.
O fluxo guiado de montagem comeca na HU-002.

* Sem interface grafica, sem banco de dados, **sem dependencia externa** (so o JDK).
* Dominio imutavel (`Componente` + 6 subclasses) validado por 10 verificacoes de
  cadastro, que classificam cada problema em `ERRO` (registro descartado) ou
  `AVISO` (registro usavel com ressalva).

## Como rodar

Precisa de **JDK 17+** (testado com o 25). Estando nesta pasta (`pc-builder/`):

```bash
./scripts/testar.sh                  # compila tudo e roda os testes da HU-001
./scripts/executar.sh                # carrega a base
./scripts/executar.sh --validar-base # carrega a base e mostra os problemas
```

Windows (Prompt de Comando):

```bat
scripts\testar.bat
scripts\executar.bat --validar-base
```

Se o script nao executar direto, use `bash scripts/testar.sh`.

Saida esperada:

```
Base carregada: 17230 componentes, 0 erro(s), 0 aviso(s).
```

Codigos de saida: `0` sucesso, `1` erro de uso, `2` base mockada invalida.

## Base de componentes

O arquivo `src/main/resources/base/componentes.json` (8,7 MB) ja esta no repositorio.
Para regera-lo a partir do catalogo (pasta `../buildcores-open-db`):

```bash
./scripts/gerar-base.sh     # ou scripts\gerar-base.bat
```

> **Precisa do `buildcores-open-db` para rodar?** Nao. O `componentes.json` esta
> versionado e e copiado para o classpath na compilacao, entao quem clona o projeto
> consegue compilar, rodar e testar sem o catalogo. A pasta `../buildcores-open-db`
> (mais de 20 mil arquivos) so e necessaria para **regenerar** a base.

## Estrutura

```
src/main/java/pcbuilder/     Main.java, enums/, modelo/, servico/, mock/, json/
src/main/resources/base/     componentes.json (base gerada)
src/test/java/pcbuilder/     testes (JDK puro, sem JUnit)
tools/java/pcbuilder/        conversor OpenDB -> componentes.json
scripts/                     compilar / executar / testar / gerar-base (.sh e .bat)
build/                       saida da compilacao (fora do git)
```

## Documentacao

* [`docs/historias-de-usuario.md`](docs/historias-de-usuario.md) — as 11 HUs e as tasks.
* [`docs/modelo-de-dados.md`](docs/modelo-de-dados.md) — campos e regras C01–C15 / L01–L11.
* [`docs/base-mockada.md`](docs/base-mockada.md) — como a base é gerada do OpenDB (regras, contagens, registros ignorados).
* [`docs/analise-tecnica.md`](docs/analise-tecnica.md) e [`docs/exemplo-de-componentes.md`](docs/exemplo-de-componentes.md) — cálculos e configuração de referência.
* [`docs/licensing/`](docs/licensing/) — licença e atribuição dos dados de origem (CC BY-NC-SA 4.0).
