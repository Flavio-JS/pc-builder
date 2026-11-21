# PC Builder CLI

Aplicacao Java de **linha de comando** que ajuda um usuario leigo a montar um PC:
o programa filtra as pecas que encaixam, avisa o que esta limitando o desempenho e
mostra o resumo com os precos. A base tem **17.230 componentes** convertidos do
catalogo publico BuildCores OpenDB para um JSON no formato do nosso modelo de dados
(o catalogo original nao fica no repositorio — ver `docs/base-mockada.md`).

**Status:** **HU-001** (carregar e validar a base mockada), **HU-002** (menu
principal + etapa 1, escolha do processador), **HU-003** (filtro de compatibilidade
dura), **HU-004** (avisos de limitacao, marcador `[!]`) e **HU-005** (etapa 2, escolha
da placa-mae) implementadas. As telas das etapas 3 a 6 chegam nas HUs 006 a 009.

* Sem interface grafica, sem banco de dados, **sem dependencia externa** (so o JDK).
* Dominio imutavel (`Componente` + 6 subclasses) validado por 10 verificacoes de
  cadastro, que classificam cada problema em `ERRO` (registro descartado) ou
  `AVISO` (registro usavel com ressalva).
* A montagem guiada guarda as pecas em `Montagem` (`Map<CategoriaComponente,
  Componente>`) e a ordem fixa das 6 etapas fica em `config/OrdemEtapas`.
* O **filtro de compatibilidade** (`analise/filtro/`) aplica as regras duras C01, C02,
  C03, C04, C05, C06, C10, C12 e C14 antes de listar: o que nao encaixa fica em
  `[M] Ver ocultos`, com o motivo em uma linha (`soquete AM5 x processador LGA1700`).
  Nenhum item e escondido por desempenho.
* O **avaliador de limitacoes** (`analise/limitacao/`) aplica C09 (BIOS minima, por
  familia de chipset) e L01, L02, L04-L07 e L09-L11 sobre a lista ja filtrada: a peca
  continua selecionavel, aparece com `[!]` antes do numero e, ao ser escolhida, explica
  a perda em linguagem simples (`A placa-mae ... suporta ate 6000 MT/s ... perda de
  ~25% ... A escolha continua valida.`). Os avisos ficam acumulados na montagem e o
  comando `[L] Avisos` os relê.
* **Etapa 2 (placa-mae)** com filtro por soquete, linha de apoio montada a partir dos
  campos (`chipset B650 | MATX | 4 slots DDR5 (max 6000 MT/s) | VRM 8x50A`),
  `[M] Ver ocultos`, `[0] Voltar` (preserva a CPU e recalcula o filtro) e os avisos de
  VRM (L02) e BIOS (C09) marcados com `[!]`.
* Com `--explicar-filtro`, cada item oculto tambem e escrito no `stderr` (depuracao).
* Com `--detalhar-limitacoes`, a previa da proxima etapa imprime o bloco completo de
  cada aviso e os dados que faltam para calcular (`Sem dados para calcular`).
* O usuario leigo so digita **numeros de opcao**: nenhuma especificacao tecnica e
  pedida e nenhuma peca e escolhida automaticamente.

## Como rodar

Precisa de **JDK 17+** (testado com o 25). Estando nesta pasta (`pc-builder/`):

```bash
./scripts/testar.sh                  # compila tudo e roda os testes das HUs 001 a 005
./scripts/executar.sh                # carrega a base e abre o menu principal
./scripts/executar.sh --validar-base # carrega a base e mostra os problemas
./scripts/executar.sh --explicar-filtro # escreve cada item oculto no stderr
./scripts/executar.sh --detalhar-limitacoes # mostra os avisos [!] completos
```

Windows (Prompt de Comando):

```bat
scripts\testar.bat
scripts\executar.bat
scripts\executar.bat --validar-base
```

Se o script nao executar direto, use `bash scripts/testar.sh`.

Saida esperada ao executar sem argumentos:

```
Base carregada: 17230 componentes, 0 erro(s), 0 aviso(s).

[1] Montar PC (guiado)   [0] Sair
>
```

Apos o `[1]`, a etapa 1 lista os **265 processadores** do catalogo com `#`, `Modelo`,
`Preco (R$)` e uma linha de apoio (`8 nucleos | AM5 | video integrado: sim`). A
escolha e feita pelo numero da opcao e o programa imprime
`Processador selecionado: AMD Ryzen 7 9700X - R$ 1.919,90`.

Ao avancar, a etapa 2 aplica o filtro por soquete (HU-005) e lista as placas compativeis
com a linha de apoio montada a partir dos campos; as placas de outro soquete ficam em
`[M] Ver ocultos` com o motivo:

```
=== Etapa 2 de 6 - Placa-Mae (obrigatorio) ===
[+Processador]   >>[Placa-mae]<<   [Memoria]   [Video]   [Armazenamento]   [Fonte]
   1) ASUS B760M-AYW D4                R$ 949,90  chipset B760 | MATX | 2 slots DDR4 (max 3200 MT/s) | VRM 8x50A
[!]   5) ASUS H610 PRIME H610M-K         R$ 599,90  chipset H610 | MATX | 2 slots DDR5 (max 6400 MT/s) | VRM 6x40A
...
598 componentes compativeis (1405 ocultos por incompatibilidade)   251 com aviso [!]   [M] Ver ocultos   [L] Avisos
----------------------------------------------------------------------------------------------------
[0] Voltar
```

Escolher um item com `[!]` mostra o bloco de aviso e espera `[Enter] para continuar`;
`[0]` volta para a etapa 1 preservando a CPU, e trocar a CPU recalcula o filtro da
etapa 2. O aviso de BIOS da H610 (familia de 2022) com o Core i9 13900T (2023) fica:

```
[!] AVISO - ASUS H610 PRIME H610M-K limita Intel Core i9 13900T:
    requisitoBIOS: CPU de 2023 > chipset H610 (2022) (esta placa-mae pode exigir atualizacao de BIOS ...)
    A escolha continua valida.
```

> A C09 e uma **estimativa por familia de chipset** porque a tabela `RequisitoBIOS`
> (par placa/CPU) nao existe na base; nenhuma placa e ocultada por isso.

Depois da placa-mae, a previa da etapa 3 mostra quantas memorias teriam `[!]` por causa
do teto de frequencia da placa (`A placa-mae ... suporta ate ... perda de ~N%`). Com
`--detalhar-limitacoes` cada aviso aparece completo, inclusive os dados que faltam para
calcular (`Sem dados para calcular`). Nenhum aviso remove a peca da lista: escolher a
memoria de 8000 MT/s em uma placa de 6000 MT/s continua valido, com o aviso de ~25% de
perda de frequencia.

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
src/main/java/pcbuilder/     Main.java, analise/, analise/filtro/, analise/limitacao/,
                             cli/, cli/etapas/, config/, enums/, json/, mock/, modelo/,
                             relatorio/, servico/, servico/resumos/
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
