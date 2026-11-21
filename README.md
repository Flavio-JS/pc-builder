# PC Builder CLI

Aplicacao Java de **linha de comando** que ajuda um usuario leigo a montar um PC:
o programa filtra as pecas que encaixam, avisa o que esta limitando o desempenho e
mostra o resumo com os precos. A base tem **17.230 componentes** convertidos do
catalogo publico BuildCores OpenDB para um JSON no formato do nosso modelo de dados
(o catalogo original nao fica no repositorio — ver `docs/base-mockada.md`).

**Status:** **HU-001** (carregar e validar a base mockada), **HU-002** (menu
principal + etapa 1, escolha do processador), **HU-003** (filtro de compatibilidade
dura), **HU-004** (avisos de limitacao, marcador `[!]`), **HU-005** (etapa 2, placa-mae),
**HU-006** (etapa 3, memoria), **HU-007** (etapa 4, placa de video condicional),
**HU-008** (etapa 5, armazenamento), **HU-009** (etapa 6, fonte) e **HU-010** (resumo
final) implementadas. A troca de peca a partir do resumo chega na HU-011.

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
* **Etapa 3 (memoria)** com filtro por tipo DDR/slots/capacidade, linha de apoio
  calculada (`2x16 GB = 32 GB | 8000 MT/s | CL38 | EXPO: sim`), `[M] Ver ocultos` e os
  avisos L04 (frequencia/single channel) e C02 (EXPO/XMP nao suportado) marcados com
  `[!]`. O caso 8000 MT/s x placa de 6000 MT/s avisa a perda de ~25% e mantem a escolha.
* **Etapa 4 (placa de video)** condicional ao video integrado da CPU: `(opcional)` com
  `[0] Pular` quando a CPU tem iGPU e `(obrigatorio)` sem pular quando nao tem (C06).
  Filtro C05 e linha de apoio `12 GB GDDR6X | PCIe 4.0 x16 | 220 W | 1x 16-pinos`.
* **Etapa 5 (armazenamento)** une SSD e HD na mesma lista, filtra por C04 (slot M.2 /
  porta SATA) e mostra `SSD NVMe M.2 2280 | 2 TB | 7000 MB/s` ou
  `HD SATA 3.5" | 2 TB | 200 MB/s`; HD/SSD SATA recebem aviso informativo de midia lenta.
* **Etapa 6 (fonte)** mostra consumo com margem de 40% e conectores exigidos, detalha a
  conta com `[C]`, filtra por C10/C14 (potencia e conectores) e marca L06 (pico
  transiente, `RISCO`) e L09 (eficiencia) com `[!]`.
* **Resumo final** com tabela `Etapa | Categoria | Peca escolhida | Preco (R$)`, total em
  `BigDecimal`, `(nao selecionado)` na GPU pulada, nota para peca sem preco, bloco
  `Avisos: N` e rodape `[T] Trocar uma peca`, `[N] Nova montagem` e `[0] Sair`.
* Com `--explicar-filtro`, cada item oculto tambem e escrito no `stderr` (depuracao).
* Com `--detalhar-limitacoes`, ao selecionar uma peca o programa imprime os dados que
  faltam para calcular alguma regra (`Sem dados para calcular: ...`).
* O usuario leigo so digita **numeros de opcao**: nenhuma especificacao tecnica e
  pedida e nenhuma peca e escolhida automaticamente.

## Como rodar

Precisa de **JDK 17+** (testado com o 25). Estando nesta pasta (`pc-builder/`):

```bash
./scripts/testar.sh                  # compila tudo e roda os testes das HUs 001 a 010
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

Ao escolher a placa, a etapa 3 lista so as memorias do tipo DDR da placa e que cabem nos
slots/capacidade (C02/C03). Kits de outro tipo ou grandes demais ficam em
`[M] Ver ocultos`; kits que rendem menos do que o anunciado aparecem com `[!]`:

```
=== Etapa 3 de 6 - Memoria (obrigatorio) ===
[+Processador]   [+Placa-mae]   >>[Memoria]<<   [Video]   [Armazenamento]   [Fonte]
   1) CORSAIR Vengeance                  R$   799,00  2x16 GB = 32 GB | 6000 MT/s | CL30 | EXPO: sim
[!]   4) CORSAIR Vengeance                  R$   999,00  2x32 GB = 64 GB | 8000 MT/s | CL38 | EXPO: sim
...
1510 componentes compativeis (2996 ocultos por incompatibilidade)   863 com aviso [!]   [M] Ver ocultos   [L] Avisos
----------------------------------------------------------------------------------------------------
[0] Voltar
```

Um kit de 8000 MT/s em uma placa de teto 6000 MT/s continua selecionavel e explica a
perda; o kit de 1 pente avisa o single channel; e um kit com perfil que a placa nao
suporta avisa que vai rodar em JEDEC:

```
[!] AVISO - A placa-mae ... limita CORSAIR Vengeance:
    frequenciaMTs: 8000 MT/s > 6000 MT/s (A placa-mae ... suporta ate 6000 MT/s. Esta memoria (8000 MT/s) vai rodar a 6000 MT/s: perda de ~25% da frequencia.)
    A escolha continua valida.
[Enter] para continuar Memoria selecionada: ... - R$ ... (2x32 GB = 64 GB, Dual Channel)
```

`[0] Voltar` retorna a etapa 2 preservando CPU e placa-mae.

A etapa 4 e condicional (C06): com CPU que tem video integrado o cabecalho e
`(opcional)` e o rodape oferece `[0] Pular`; sem video integrado, e `(obrigatorio)`,
explica o motivo e nao permite pular:

```
=== Etapa 4 de 6 - Placa de Video (opcional) ===
[+Processador]   [+Placa-mae]   [+Memoria]   >>[Video]<<   [Armazenamento]   [Fonte]
...
3831 componentes compativeis (nenhum oculto por incompatibilidade)   1442 com aviso [!]   [L] Avisos
----------------------------------------------------------------------------------------------------
[0] Pular
```

`[0]` imprime `Placa de video pulada: o seu processador ja tem video integrado.` e segue
para a etapa 5. A linha de apoio e
`12 GB GDDR6X | PCIe 4.0 x16 | 220 W | 1x 16-pinos`, e L05 (banda), L01 (gargalo) e L07
(VRAM) aparecem com `[!]` sem bloquear a escolha. Sem GPU compativel e etapa
obrigatoria, o programa imprime `Nenhuma placa de video compativel com a sua placa-mae.`
e oferece `[0] Voltar`.

Depois da GPU (ou do pulo), a etapa 5 une SSD e HD na mesma lista e filtra por C04
(slot M.2 / porta SATA). O que nao encaixa fica em `[M]` com o motivo; a linha de apoio
mostra midia, capacidade e velocidade:

```
=== Etapa 5 de 6 - Armazenamento (obrigatorio) ===
[+Processador]   [+Placa-mae]   [+Memoria]   [+Video]   >>[Armazenamento]<<   [Fonte]
   1) Inland Premium PCIe                  R$   149,90  SSD NVMe M.2 2280 | 512 GB | 3400 MB/s
[!]   4) Seagate Barracuda Compute          R$   319,90  HD SATA 3.5" | 2 TB | 190 MB/s
...
3322 componentes compativeis (nenhum oculto por incompatibilidade)   2004 com aviso [!]   [L] Avisos
----------------------------------------------------------------------------------------------------
[0] Voltar
```

O `[!]` do HD/SSD SATA e o informativo de midia lenta (mais lento que o NVMe); o L10 de
banda aparece quando o SSD e de geracao maior que o slot. `[0] Voltar` retorna a etapa 4.

Depois do armazenamento, a etapa 6 mostra o que a configuracao exige e filtra as fontes
por potencia (C14) e conectores (C10):

```
=== Etapa 6 de 6 - Fonte (obrigatorio) ===
[+Processador]   [+Placa-mae]   [+Memoria]   [+Video]   [+Armazenamento]   >>[Fonte]<<
Consumo estimado da sua configuracao: 401 W (com margem de 40%: 562 W)
Conectores exigidos: 1x 16-pinos (placa de video) + 1x 8-pinos (placa-mae)
   1) MSI MAG A750GL                    R$ 549,90  750 W | 80 Plus Gold | 12V: 744 W | 1x 16-pinos | 2x 8-pinos
...
[C] Ver calculo do consumo   [0] Voltar
```

`[C]` detalha a soma (`Processador ... - 140 W`, `Placa de video ... - 220 W`, ...,
`Total - 401 W (com margem de 40%: 562 W)`). Fonte sem o conector exigido fica em `[M]`
(`sem conector de 16 pinos (a placa de video exige 1)`) - **nao existe caminho de
adaptador**. L09 avisa fonte fora de 40-60% de carga e L06 marca `RISCO` quando o pico da
GPU passa do trilho de 12 V. Ao escolher, imprime `Fonte selecionada: ...` e mostra o
resumo:

```
=== Resumo da sua montagem ===
 Etapa | Categoria        | Peca escolhida                                 |     Preco (R$)
   1   | Processador      | Intel Core i9 13900T                           |       2.529,90
   2   | Placa-Mae        | ASUS B760M-AYW D4                              |         949,90
   3   | Memoria          | TEAMGROUP T-Force Delta                        |         189,90
   4   | Placa de Video   | (nao selecionado)                              |          R$ --
   5   | Armazenamento    | Inland Premium PCIe                            |         149,90
   6   | Fonte            | Antec Earthwatts                               |         299,90
TOTAL: R$ 4.119,50
Avisos: 2
[!] TEAMGROUP T-Force Delta: quantidadePentes 1 pente(s) > 2 canais
[!] Antec Earthwatts: percentualCargaFonte 26% > 40-60%
[T] Trocar uma peca   [N] Nova montagem   [0] Sair
```

`[N]` recomeca o fluxo com uma montagem vazia e `[0]` encerra. O `[T]` fica como marcador
ate a HU-011.

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
