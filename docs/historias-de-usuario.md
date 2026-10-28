# Histórias de Usuário — PC Builder CLI

Requisitos do *PC Builder CLI* em **11 Histórias de Usuário (HU)** ordenadas pela **ordem de execução** (a HU-001 é pré-requisito de todas). Escopo enxuto para o trabalho da disciplina.

Aplicação **Java de linha de comando**, **sem interface gráfica**, com **dados mockados** em memória — inclusive os preços.

Documentos relacionados:

* [`modelo-de-dados.md`](./modelo-de-dados.md) — campos de cada componente e regras **C01–C15** (compatibilidade) / **L01–L11** (limitações).
* [`analise-tecnica.md`](./analise-tecnica.md) — cálculos que justificam as regras.
* [`exemplo-de-componentes.md`](./exemplo-de-componentes.md) — configuração de referência usada nos mocks.

---

## 1. Personas

| Persona | Descrição | Papel |
| --- | --- | --- |
| **Usuário iniciante (leigo)** — *principal* | Quer montar o primeiro PC, **não sabe** o que combina com o quê; não conhece soquete, chipset, DDR nem TDP | Usa **só** a montagem guiada: o sistema esconde o que não serve e **explica** o que está limitando |
| **Desenvolvedor/mantenedor** | Responsável pelo código e pelos mocks | Garante a base mockada válida (HU-001) |

> **Premissa de produto:** o leigo **nunca** digita especificação técnica. Ele só digita **números de opção**. Toda decisão técnica é do programa; toda explicação é em linguagem simples.

---

## 2. Regras de negócio que governam as HUs

### 2.1 A montagem guiada (feature principal)

Ordem **fixa** das etapas e obrigatoriedade:

| Etapa | Categoria | Obrigatória? | Por que nesta posição |
| --- | --- | --- | --- |
| 1 | **Processador** | Sempre | Define o soquete e se haverá vídeo integrado |
| 2 | **Placa-Mãe** | Sempre | Precisa do soquete da CPU (etapa 1) |
| 3 | **Memória** | Sempre | Precisa do tipo DDR e do teto de frequência da placa (etapa 2) |
| 4 | **Placa de Vídeo** | **Obrigatória se** `cpu.possuiVideoIntegrado = false` · **opcional se** `true` | Define quantos conectores a fonte precisará |
| 5 | **Armazenamento** (SSD ou HD) | Sempre | Precisa dos slots M.2 / portas SATA da placa |
| 6 | **Fonte** | Sempre | **Última de propósito**: só com todas as peças é possível somar o consumo e saber quantos conectores a GPU exige |
| — | **Resumo final** | Exibido ao concluir a etapa 6 | — |

**Decisões de escopo (trabalho de faculdade):**

* **Sem perfil de uso**: `L01` e `L07` usam `gpu.resolucaoAlvo` como referência; `L08` (RAM/disco por finalidade) **não é aplicada**.
* **Sem cooler e sem gabinete**: `L03` (cooler ↔ CPU) **não é aplicada** e `C08`/`C11`/`C13` ficam `NAO APLICAVEL`.
* **Sem modo catálogo e sem busca**: o único caminho de entrada é `[1] Montar PC (guiado)`.
* **Cooler, gabinete e monitor fora do escopo**: `C08`, `C11` e `C13` ficam `NAO APLICAVEL`.

### 2.2 Filtro (bloqueia) × Aviso (não bloqueia)

Distinção central do sistema: **nada** é escondido por desempenho; só se esconde o que **não encaixa**.

| Situação | Comportamento | Regras |
| --- | --- | --- |
| **Incompatível (hard)** | O componente **não aparece** na lista da etapa | C01 (soquete), C02 (tipo DDR), C03 (pentes/capacidade), C04 (slot/porta), C05 (slot x16), C06 (iGPU), **C10 (conectores da fonte)**, C12 (ECC), C14 (potência da fonte) |
| **Compatível com limitação (soft)** | O componente **aparece**, marcado com `[!]`, e o usuário **pode escolhê-lo** | L01–L07, L09–L11 + alertas de C02 (EXPO), C03 (single channel), C05 (geração de PCIe), C07 (overclock), C09 (BIOS) |

**Regra específica da fonte (C10):** se a GPU escolhida exige `X` conectores de um tipo, a etapa 6 **só lista fontes com `X` ou mais** daquele tipo:

| Exigência da GPU / placa-mãe | Filtro aplicado na fonte |
| --- | --- |
| GPU `quantidadeConectores16pin = 1` | `fonte.quantidadeConectores16pin12VHPWR >= 1` |
| GPU `quantidadeConectores8pin = 2` | `fonte.quantidadeConectoresPCIe8pin >= 2` |
| GPU `quantidadeConectores6pin = 1` | `fonte.quantidadeConectoresPCIe6pin >= 1` |
| Placa-mãe `quantidadeConectoresCPU8pin = 1` | `fonte.quantidadeConectoresCPU8pin >= 1` |

> **Sem caminho de adaptador:** fonte sem o conector exigido é **ocultada**, mesmo que tenha potência sobrando. É mais rígido que o "adaptador 2x 8-pinos" citado em [`analise-tecnica.md`](./analise-tecnica.md), e é intencional: adaptador é risco para o leigo.

### 2.3 Formato da notificação de limitação

```
[!] <componente ja escolhido> limita <componente novo>:
    <campo>: <valor do novo> > <valor suportado> (<consequencia em linguagem simples>)
    A escolha continua valida.
```

**Exemplo obrigatório (vira teste de aceite):** placa-mãe com `frequenciaMemoriaMaximaMTs = 6000` e memória de `8000 MT/s`:
- a memória **continua na lista** (é DDR5, encaixa no slot — logo é compatível);
- aparece com `[!]` e, ao ser selecionada, o sistema avisa:
  `[!] A placa-mae ASUS Prime B650M-A suporta ate 6000 MT/s. Esta memoria (8000 MT/s) vai rodar a 6000 MT/s: perda de ~25% da frequencia. A escolha continua valida.`

O aviso vale nos **dois sentidos**: escolher uma peça fraca depois também avisa (ex.: placa-mãe com VRM apertado para a CPU já escolhida — `L02`).

---

## 3. Convenções deste documento

| Item | Padrão |
| --- | --- |
| **ID de história** | `HU-000` (3 dígitos, na ordem de execução) |
| **ID de tarefa** | `ST-000.0` (`ST-` + número da HU + sequencial) |
| **Status** | ✅ implementada · 🟨 em andamento · ⬜ pendente · ❌ cancelada |
| **Camadas** | `(Domínio)`, `(Regras)`, `(Dados/Mock)`, `(CLI)`, `(Relatório)`, `(Teste)`, `(Docs)` |
| **Pacote base** | `pcbuilder` — `modelo`, `enums`, `servico`, `analise`, `analise/filtro`, `analise/limitacao`, `relatorio`, `cli`, `mock`, `config` |
| **Só JDK** | `java.util`, `java.io`, `java.math`, `java.time`, `java.text` |
| **Códigos de saída** | `0` sucesso · `1` erro de uso · `2` base mockada inválida |
| **Cabeçalho de etapa** | `=== Etapa N de 6 - <Categoria> (obrigatorio / opcional) ===` |
| **Marcadores na lista** | `[!]` componente compatível **com limitação** |
| **Comandos padrão** | número = escolher · `[0]` voltar/pular · `[M]` ver ocultos · `[L]` avisos |
| **Menu principal** | `[1] Montar PC (guiado)` · `[0] Sair` |

**Definição de pronto (DoD):** compila, roda pelo terminal com o comportamento descrito nos critérios de aceite, não adiciona dependência externa e mantém este documento atualizado.

---

## 4. Ordem de execução e dependências

| Ordem | HU | Título | Depende de | Por quê |
| --- | --- | --- | --- | --- |
| 1 | HU-001 | Carregar e validar a base mockada | — | Sem catálogo válido nenhuma etapa funciona |
| 2 | HU-002 | Iniciar a montagem guiada e escolher o processador (etapa 1) | HU-001 | Cria a `Montagem`, o contêiner do fluxo e a 1ª etapa |
| 3 | HU-003 | Exibir apenas componentes compatíveis em cada etapa | HU-002 | Precisa de pelo menos 1 peça escolhida para filtrar |
| 4 | HU-004 | Avisar quando uma peça já escolhida limita a nova | HU-003 | O aviso é exibido sobre a lista já filtrada |
| 5 | HU-005 | Escolher a placa-mãe (etapa 2) | HU-002/003/004 | 1ª etapa que usa filtro (soquete) e aviso (VRM) |
| 6 | HU-006 | Escolher a memória (etapa 3) | HU-005 | Depende do tipo DDR e do teto de frequência da placa |
| 7 | HU-007 | Escolher a placa de vídeo (etapa 4 — condicional) | HU-006 | Depende de `possuiVideoIntegrado` e do slot PCIe |
| 8 | HU-008 | Escolher o armazenamento — SSD ou HD (etapa 5) | HU-007 | Depende dos slots M.2 / portas SATA da placa |
| 9 | HU-009 | Escolher a fonte (etapa 6) | HU-008 | Depende do consumo total **e** dos conectores exigidos pela GPU |
| 10 | HU-010 | Ver o resumo final com as peças e os preços | HU-009 | Só faz sentido com as 6 etapas concluídas |
| 11 | HU-011 | Trocar uma peça a partir do resumo | HU-010 | Precisa do resumo e das etapas já existentes |

> **Escopo fechado em 11 HUs (001–011)**: base mockada + montagem guiada completa + resumo com preços + troca de peça. Exportação, ficha técnica, persistência e sugestões automáticas ficam fora.

---

## 5. Épico 1 — Base de dados (pré-requisito de tudo)

### HU-001: Carregar e validar a base mockada

**Status:** ✅ implementada — ver [`../README.md`](../README.md) e [`base-mockada.md`](./base-mockada.md).

COMO desenvolvedor responsável pela base de componentes,
EU QUERO carregar os dados mockados e receber a lista de registros inválidos,
PARA QUE nenhuma etapa da montagem ofereça uma peça com cadastro incompleto ou campo "sujo".

**Critérios de aceite:**
- `CatalogoMock.criar()` carrega o **catálogo completo convertido do OpenDB** (`src/main/resources/base/componentes.json`, gerado por `scripts/gerar-base.*`): na versão atual são **17.230 componentes**, muito acima do mínimo de **3 itens por categoria** do fluxo guiado (CPU, placa-mãe, memória, GPU, armazenamento, fonte), e a base mantém **pares propositalmente incompatíveis** (ex.: CPU AM5 + placa-mãe LGA1700; RAM DDR4 + placa DDR5) para exercitar o filtro;
- Todo item tem `id` único, `precoMedioBRL` preenchido (o preço vem sempre do mock) e os campos obrigatórios da seção 2 do modelo de dados;
- A carga aplica as 10 verificações do checklist (seção 18 do modelo de dados) e classifica cada problema em `ERRO` (registro descartado) ou `AVISO` (usável com ressalva);
- Ao final o console imprime `Base carregada: <total> componentes, <n> erro(s), <m> aviso(s).` — na base atual: `Base carregada: 17230 componentes, 0 erro(s), 0 aviso(s).`;
- Com `--validar-base` e pelo menos 1 erro, o programa encerra com código `2`;
- A validação devolve objeto de resultado; **nenhum** `System.exit` no meio da carga;
- A base é carregada **uma única vez** por execução e compartilhada por todas as etapas.

**Tarefas:**
- ✅ **ST-001.1 (Domínio):** hierarquia `Componente` (abstrata) + `CategoriaComponente` (`pcbuilder/modelo/Componente.java`, `pcbuilder/enums/CategoriaComponente.java`) conforme seção 2 do modelo de dados.
- ✅ **ST-001.2 (Domínio):** subclasses `Processador`, `PlacaMae`, `Memoria`, `PlacaVideo`, `Armazenamento`, `Fonte` com os campos atômicos das seções 3–8 do modelo de dados (objetos **imutáveis**).
- ✅ **ST-001.3 (Dados/Mock):** `pcbuilder/mock/LeitorBaseMock.java` (leitura de `base/componentes.json` com o parser `pcbuilder/json/Json.java`) e `CatalogoMock.java` (agregação em `Map<CategoriaComponente, List<Componente>>`), incluindo os pares incompatíveis e as fontes com diferentes combinações de conectores.
- ✅ **ST-001.4 (Regras):** `ValidadorCadastro` + `ProblemaCadastro` + `ResultadoValidacao` (`pcbuilder/servico/`) com as 10 verificações do checklist.
- ✅ **ST-001.5 (CLI):** `--validar-base` em `Main`, imprimindo o resumo e definindo o código de saída.
- ✅ **ST-001.6 (Teste):** 1 caso por verificação do checklist, `id` duplicado, contagem de itens por categoria e presença dos pares incompatíveis.
- ✅ **ST-001.7 (Dados/Mock):** conversor `tools/java/pcbuilder/ferramentas/` + `scripts/gerar-base.*`, que gera o JSON a partir do `buildcores-open-db` com heurísticas documentadas em [`base-mockada.md`](./base-mockada.md). **É ferramenta de desenvolvimento, não parte do produto**: não entra no build do app e deve ser removida na entrega final (ver checklist na [seção 10](#10-checklist-de-encerramento-limpeza-antes-da-entrega)).

---

## 6. Épico 2 — Montagem guiada, resumo e revisão (feature principal)

### HU-002: Iniciar a montagem guiada e escolher o processador (etapa 1)

*Persona: usuário iniciante (leigo)*

COMO usuário iniciante que quer montar meu primeiro PC,
EU QUERO entrar em uma montagem guiada e escolher o processador de uma lista simples,
PARA QUE eu comece a montagem sem precisar saber quais peças existem ou combinam entre si.

**Critérios de aceite:**
- O menu principal imprime `[1] Montar PC (guiado)`, `[0] Sair`;
- `[1]` cria uma `Montagem` vazia e imprime `=== Etapa 1 de 6 - Processador (obrigatorio) ===`;
- A lista traz **todos** os processadores do catálogo (1ª etapa, não há peça anterior para filtrar), com `#`, `Modelo`, `Preco (R$)` e uma linha de apoio (`8 nucleos | AM5 | video integrado: sim`);
- A barra de progresso textual aparece no topo: `[Processador] [Placa-mae] [Memoria] [Video] [Armazenamento] [Fonte]`, destacando a etapa atual;
- A escolha é feita **apenas pelo número**; texto inválido imprime `Digite o numero de uma das opcoes.` e reexibe a lista;
- Após escolher, imprime `Processador selecionado: AMD Ryzen 7 9700X - R$ 2.199,00` e avança para a etapa 2;
- Na etapa 1, `[0] Cancelar montagem` pede confirmação `S/N` e, se confirmado, descarta a montagem e volta ao menu principal;
- A montagem vive em um objeto `Montagem` (`Map<CategoriaComponente, Componente>`) e **não** se perde ao avançar de etapa;
- Categoria vazia no catálogo imprime `Nenhum processador cadastrado.` e retorna ao menu;
- Nenhuma peça é escolhida automaticamente pelo sistema.

**Tarefas:**
- ⬜ **ST-002.1 (Domínio):** `Montagem` (`pcbuilder/modelo/Montagem.java`) com `Map<CategoriaComponente, Componente>`, `adicionar`, `obter`, `estaVazia()` e `custoTotal()` em `BigDecimal`.
- ⬜ **ST-002.2 (Regras):** `MontagemGuiada` (`pcbuilder/servico/MontagemGuiada.java`) — orquestrador do fluxo (`proximaEtapa`, `etapaAtual`, `concluir`) com a ordem fixa das 6 categorias em `pcbuilder/config/OrdemEtapas.java`.
- ⬜ **ST-002.3 (CLI):** `Terminal` + `TabelaComponentes` (`pcbuilder/cli/`) com leitura por número, molduras e largura fixa.
- ⬜ **ST-002.4 (CLI):** `MenuPrincipal` + `Main` (`pcbuilder/Main.java`) com o laço do menu, código de saída `0` e captura de entrada inválida.
- ⬜ **ST-002.5 (CLI):** `TelaEtapa` (`pcbuilder/cli/TelaEtapa.java`) com cabeçalho da etapa, barra de progresso textual e rodapé de comandos.
- ⬜ **ST-002.6 (CLI):** `EtapaProcessador` (`pcbuilder/cli/etapas/EtapaProcessador.java`) com a listagem e o cancelamento.
- ⬜ **ST-002.7 (Teste):** escolher, cancelar e opção inválida na etapa 1; `Montagem` sobrevive ao avanço de etapa.

---

### HU-003: Exibir apenas componentes compatíveis em cada etapa

*Persona: usuário iniciante (leigo)*

COMO usuário iniciante que não sabe o que combina com o quê,
EU QUERO ver, em cada etapa, somente as peças que encaixam no que eu já escolhi,
PARA QUE eu não perca tempo lendo opções que nem vão funcionar juntas.

**Critérios de aceite:**
- Toda etapa a partir da 2ª aplica o **filtro de compatibilidade (hard)** antes de listar;
- Um item é **removido** da lista se, combinado com as peças já escolhidas, alguma regra dura falhar: `C01` (soquete), `C02` (tipo DDR), `C03` (pentes > slots ou capacidade excedida), `C04` (sem slot M.2/porta SATA), `C05` (sem slot x16), `C06` (CPU sem vídeo integrado), `C10` (conectores da fonte), `C12` (ECC), `C14` (potência da fonte);
- **Nenhum item é removido por limitação de desempenho** (frequência, VRM, geração de PCIe, gargalo) — isso é a HU-004;
- O rodapé informa a contagem: `2 componentes compativeis (1 oculto por incompatibilidade)`;
- `[M] Ver ocultos` lista os itens removidos **com o motivo em uma linha** (ex.: `ASUS TUF B650-Plus - incompativel: soquete AM5 x processador LGA1700`), sem permitir selecioná-los;
- Se **nenhum** item for compatível, imprime `Nenhum componente compativel com a sua configuracao ate aqui.` + o motivo predominante + `[0] Voltar` para trocar a peça anterior;
- O filtro é recalculado a cada etapa (sem cache), refletindo qualquer troca feita na HU-011;
- Com `--explicar-filtro`, cada item oculto também é impresso no `stderr` (depuração).

**Tarefas:**
- ⬜ **ST-003.1 (Regras):** interface `RegraFiltro` (`pcbuilder/analise/filtro/RegraFiltro.java`) com `CategoriaComponente categoriaAfetada()` e `Optional<String> motivoIncompatibilidade(Montagem atual, Componente candidato)` — devolve o motivo quando o item deve ser **ocultado**.
- ⬜ **ST-003.2 (Regras):** implementações `FiltroSoquete` (C01), `FiltroTipoMemoria` (C02), `FiltroSlotsMemoria` (C03), `FiltroSlotArmazenamento` (C04), `FiltroSlotPcie` (C05), `FiltroVideoIntegrado` (C06), `FiltroConectoresFonte` (C10) e `FiltroFonteSuficiente` (C14).
- ⬜ **ST-003.3 (Regras):** `FiltroEtapa` (`pcbuilder/analise/filtro/FiltroEtapa.java`) com `filtrar(categoria, montagem)` devolvendo `ListaFiltrada` (`compativeis`, `ocultos` + motivo, `totalOriginal`).
- ⬜ **ST-003.4 (CLI):** integrar `FiltroEtapa` na `TelaEtapa`, com a contagem de ocultos e o comando `[M]`.
- ⬜ **ST-003.5 (CLI):** mensagem de "nenhum compatível" com o motivo predominante (contagem de motivos) e retorno à etapa anterior.
- ⬜ **ST-003.6 (Teste):** CPU AM5 → só placas AM5; placa DDR5 → só memórias DDR5; GPU 1x 16-pinos → fontes sem 16 pinos ocultas; e o caso "nenhum compatível".

---

### HU-004: Avisar quando uma peça já escolhida limita a nova (sem bloquear)

*Persona: usuário iniciante (leigo)*

COMO usuário iniciante escolhendo uma peça mais avançada do que a placa suporta,
EU QUERO ser avisado de que a configuração não vai aproveitar 100% e **qual** peça está limitando,
PARA QUE eu decida com consciência se aceito a perda ou se troco a outra peça — sem ser impedido de escolher.

**Critérios de aceite:**
- Todo item **compatível** que gere limitação aparece na lista marcado com `[!]` antes do número; itens sem limitação aparecem normais;
- O item **continua selecionável** — nenhum aviso bloqueia a escolha;
- Ao selecionar um item com `[!]`, o sistema imprime o bloco de aviso e aguarda `[Enter] para continuar`;
- O aviso nomeia o componente limitante, o campo, os valores e a consequência em linguagem simples:

```
[!] A placa-mae ASUS Prime B650M-A suporta ate 6000 MT/s.
    Esta memoria (8000 MT/s) vai rodar a 6000 MT/s: perda de ~25% da frequencia.
    A escolha continua valida.
```

- O aviso vale nos **dois sentidos**: escolher uma peça que limita algo já escolhido também avisa (ex.: placa-mãe com VRM apertado para a CPU já selecionada — `L02`);
- São avaliadas as regras `L01`, `L02`, `L04`, `L05`, `L06`, `L07`, `L09`, `L10` e `L11` aplicáveis às peças já presentes (`L02` exige placa-mãe; `L06`/`L09` exigem fonte); **`L03` (cooler) e `L08` (perfil) ficam fora do escopo**;
- `L01` e `L07` usam `gpu.resolucaoAlvo` como referência de resolução;
- Cada aviso tem severidade `AVISO` (perda de desempenho) ou `RISCO` (ex.: uso de VRM > 80%, pico de energia acima do trilho 12 V), impressa no início da linha;
- Os avisos são **acumulados** na montagem (`List<Limitacao>`) e reaparecem no resumo (HU-010);
- Regra sem dados suficientes **não** gera aviso falso: aparece `Sem dados para calcular` apenas com `--detalhar-limitacoes`;
- Nenhum aviso altera o resultado do filtro da HU-003 (filtro e aviso são avaliados separadamente).

**Tarefas:**
- ⬜ **ST-004.1 (Domínio):** `Limitacao` (`pcbuilder/modelo/Limitacao.java`) com `regraId`, `componenteLimitante`, `componenteAfetado`, `campoComparado`, `valorDoNovo`, `valorSuportado`, `consequencia`, `severidade` e `percentualPerda`.
- ⬜ **ST-004.2 (Regras):** `AvaliadorLimitacoes` (`pcbuilder/analise/limitacao/AvaliadorLimitacoes.java`) com `avaliar(Montagem atual, Componente candidato)` retornando `List<Limitacao>`.
- ⬜ **ST-004.3 (Regras):** calculadoras reutilizáveis `CalculadoraMemoria` (`L04`: frequência efetiva = `min(RAM, placa-mãe, IMC)`), `CalculadoraVrm` (`L02`), `CalculadoraPcie` (`L05`, `L11`), `CalculadoraConsumo` (`L06`, `L09`) e `RegraGargaloCpuGpu` (`L01`).
- ⬜ **ST-004.4 (Regras):** `TextosLimitacao` (`pcbuilder/analise/limitacao/TextosLimitacao.java`) com a frase em linguagem simples de cada regra avaliada (`L01`, `L02`, `L04`–`L07`, `L09`–`L11`), sem jargão não explicado.
- ⬜ **ST-004.5 (CLI):** marcador `[!]` na `TabelaComponentes` (por item da lista, sem nova rodada de filtro) e bloco de aviso com `[Enter] para continuar`.
- ⬜ **ST-004.6 (CLI):** acúmulo dos avisos na `Montagem` e comando `[L] Avisos` na `TelaEtapa` para reler os avisos até ali.
- ⬜ **ST-004.7 (Teste):** placa 6000 MT/s + memória 8000 MT/s (marcado, selecionável, aviso com "6000 MT/s" e "~25%"); VRM no limite; placa-mãe escolhida depois de CPU forte; e caso sem dados (`picoTransienteWatts` nulo) que **não** pode gerar aviso falso.

---

### HU-005: Escolher a placa-mãe (etapa 2)

*Persona: usuário iniciante (leigo)*

COMO usuário iniciante que já escolheu o processador,
EU QUERO ver apenas as placas-mãe que encaixam no meu processador e saber o que cada uma suporta,
PARA QUE eu escolha uma placa que não deixe o meu processador nem a futura memória limitados.

**Critérios de aceite:**
- A etapa imprime `=== Etapa 2 de 6 - Placa-Mae (obrigatorio) ===` e a barra de progresso com `Processador` concluído;
- A lista é **filtrada** (HU-003): só aparecem placas com `soqueteCPU` igual ao `soquete` da CPU escolhida; as demais ficam em `[M] Ver ocultos` com o motivo;
- Cada linha mostra `#`, `Modelo`, `Preco (R$)` e a linha de apoio `chipset B650 | MATX | 4 slots DDR5 (max 6000 MT/s) | VRM 8x50A`;
- Placas que sustentam o processador mas com VRM apertado (`L02` acima do limite) aparecem com `[!]` e o aviso nomeia o uso estimado (HU-004);
- Placa que exigiria atualização de BIOS (`C09`) aparece com `[!]` e o aviso `pode exigir atualizacao de BIOS` — **nunca** é ocultada;
- `[0] Voltar` retorna à etapa 1 preservando a CPU; trocar a CPU refaz o filtro desta etapa;
- Após escolher, imprime `Placa-mae selecionada: ASUS Prime B650M-A - R$ 949,00` e avança para a etapa 3;
- Nenhuma placa é ocultada por limitação de desempenho — só por incompatibilidade de soquete.

**Tarefas:**
- ⬜ **ST-005.1 (CLI):** `EtapaPlacaMae` (`pcbuilder/cli/etapas/EtapaPlacaMae.java`) com listagem, linha de apoio, `[0]`, `[M]` e `[L]`.
- ⬜ **ST-005.2 (Regras):** `ResumoPlacaMae` (`pcbuilder/servico/resumos/`) montando a linha de apoio a partir de VRM, slots e chipset (nada de texto fixo no código de tela).
- ⬜ **ST-005.3 (Regras):** habilitar `FiltroSoquete` (C01) e os avisos `L02` (VRM) e `C09` (BIOS) nesta etapa.
- ⬜ **ST-005.4 (CLI):** fluxo "voltar" que retorna à etapa 1 sem perder a montagem e reaplica o filtro ao retornar.
- ⬜ **ST-005.5 (Teste):** CPU AM5 → só placas AM5; placa LGA1700 em `[M]` com motivo; VRM apertado marcado `[!]`; voltar/trocar CPU reaplicando o filtro.

---

### HU-006: Escolher a memória (etapa 3)

*Persona: usuário iniciante (leigo)*

COMO usuário iniciante que não entende de frequência de memória,
EU QUERO escolher a memória vendo só as que encaixam na placa e ser avisado quando a placa não aproveitar toda a velocidade,
PARA QUE eu saiba se estou pagando por desempenho que o meu PC não vai entregar.

**Critérios de aceite:**
- A etapa imprime `=== Etapa 3 de 6 - Memoria (obrigatorio) ===`;
- A lista é filtrada (HU-003) por `tipo` igual ao `tipoMemoria` da placa (C02) e por capacidade/pentes aceitos pela placa **e** pela CPU (C03); memórias de outro tipo ficam em `[M]` com o motivo (`DDR4 x placa DDR5`);
- Cada linha mostra `#`, `Modelo`, `Preco (R$)` e a linha de apoio `2x16 GB = 32 GB | 8000 MT/s | CL38 | EXPO: sim`;
- **Caso obrigatório desta HU:** com placa de teto `6000 MT/s`, a memória de `8000 MT/s` **aparece na lista** marcada `[!]` e, ao ser selecionada, avisa que rodará a 6000 MT/s com perda de ~25%, **mantendo a escolha**;
- Avisos também para `perfilEXPO` não suportado pela placa (roda em JEDEC) e para kit de 1 pente em placa dual channel (single channel);
- Memória cuja capacidade total excede `capacidadeMaximaMemoriaGB` (placa **ou** CPU) é **oculta** em `[M]` — isso é incompatibilidade, não limitação;
- Após escolher, imprime `Memoria selecionada: ... - R$ ... (2x16 GB = 32 GB, Dual Channel)` e avança para a etapa 4;
- Os avisos gerados ficam registrados para o resumo final.

**Tarefas:**
- ⬜ **ST-006.1 (CLI):** `EtapaMemoria` (`pcbuilder/cli/etapas/EtapaMemoria.java`) com linha de apoio contendo capacidade total e canal (calculados, nunca fixos).
- ⬜ **ST-006.2 (Regras):** habilitar `FiltroTipoMemoria`, `FiltroSlotsMemoria` e os avisos `L04` (frequência efetiva/latência) e os alertas de `C02`/`C03` (EXPO, single channel).
- ⬜ **ST-006.3 (Regras):** `CalculadoraMemoria.capacidadeTotalGB` e `memoriaEmDualChannel` usados na linha de apoio e no resumo.
- ⬜ **ST-006.4 (Teste):** placa 6000 + RAM 8000 (marcado `[!]`, selecionável, aviso correto); RAM DDR4 oculta com motivo; RAM 2x32 GB acima do limite oculta; 1 pente gera aviso de single channel; EXPO não suportado gera aviso.

---

### HU-007: Escolher a placa de vídeo (etapa 4 — condicional)

*Persona: usuário iniciante (leigo)*

COMO usuário iniciante,
EU QUERO que o sistema me diga se eu **preciso** de placa de vídeo e, se precisar, mostre só as que encaixam,
PARA QUE eu não compre uma placa desnecessária nem termine com um PC que não dá vídeo.

**Critérios de aceite:**
- A obrigatoriedade é decidida por `cpu.possuiVideoIntegrado`:
  - `true` → `=== Etapa 4 de 6 - Placa de Video (opcional) ===` e existe `[0] Pular`;
  - `false` → `=== Etapa 4 de 6 - Placa de Video (obrigatorio) ===`, **sem** opção de pular, e a mensagem `Seu processador nao tem video integrado, por isso a placa de video e obrigatoria.`;
- Ao pular (CPU com iGPU), imprime `Placa de video pulada: o seu processador ja tem video integrado.` e avança para a etapa 5;
- A lista é filtrada (HU-003) por `C05`: só placas cujo `versaoPCIe`/`pistasPCIe` cabem em um slot x16 livre da placa-mãe;
- Cada linha mostra `#`, `Modelo`, `Preco (R$)` e a linha de apoio `12 GB GDDR6X | PCIe 4.0 x16 | 220 W | 1x 16-pinos`;
- Avisos (nunca bloqueiam): `L05` (slot de geração menor → perda de banda), `L01` (desequilíbrio CPU × GPU) e `L07` (VRAM abaixo do mínimo da `resolucaoAlvo` da GPU);
- Ao escolher, imprime `Placa de video selecionada: ... - R$ ...` e avança para a etapa 5;
- Se nenhuma placa for compatível e a etapa for obrigatória, imprime `Nenhuma placa de video compativel com a sua placa-mae.` e oferece `[0] Voltar`.

**Tarefas:**
- ⬜ **ST-007.1 (Regras):** obrigatoriedade da etapa em `pcbuilder/config/OrdemEtapas.java`, calculada a partir de `cpu.possuiVideoIntegrado` (regra `C06`).
- ⬜ **ST-007.2 (CLI):** `EtapaPlacaVideo` (`pcbuilder/cli/etapas/EtapaPlacaVideo.java`) com cabeçalho dinâmico, `[0] Pular` condicional e mensagem explicativa.
- ⬜ **ST-007.3 (Regras):** habilitar `FiltroSlotPcie` (C05) e os avisos `L05`, `L01` e `L07` (usando `gpu.resolucaoAlvo`).
- ⬜ **ST-007.4 (CLI):** tratamento de "nenhuma GPU compatível" com retorno à etapa anterior.
- ⬜ **ST-007.5 (Teste):** CPU com iGPU (etapa pulável, resumo marca `(nao selecionado)`); CPU sem iGPU (etapa obrigatória, sem `[0] Pular`); GPU PCIe 5.0 em slot 4.0 gera aviso de banda; GPU de 8 GB gera aviso de VRAM.

---

### HU-008: Escolher o armazenamento — SSD ou HD (etapa 5)

*Persona: usuário iniciante (leigo)*

COMO usuário iniciante que não diferencia SSD de HD,
EU QUERO escolher o armazenamento vendo quais encaixam na minha placa e o que eu ganho ou perco em cada tipo,
PARA QUE eu escolha sem errar o encaixe e sabendo a diferença de velocidade.

**Critérios de aceite:**
- A etapa imprime `=== Etapa 5 de 6 - Armazenamento (obrigatorio) ===`;
- A lista une **SSD e HD** (não há etapas separadas) e é filtrada (HU-003) por `C04`: NVMe exige slot M.2 disponível (`placaMae.quantidadeSlotsM2`); SATA exige porta SATA livre (`quantidadePortasSATA`);
- Cada linha mostra `#`, `Modelo`, `Preco (R$)` e a linha de apoio `SSD NVMe M.2 2280 | 2 TB | 7000 MB/s` ou `HD SATA 3.5" | 2 TB | 200 MB/s`;
- Avisos (nunca bloqueiam): `L05`/`L10` quando o SSD é de geração superior ao slot (perda de banda) e informativo de mídia lenta (`HD`/`SATA`) em relação ao NVMe;
- Escolher um SSD NVMe ocupa 1 slot M.2 na contagem da montagem;
- Após escolher, imprime `Armazenamento selecionado: ... - R$ ...` e avança para a etapa 6;
- Se não houver nenhum item compatível, imprime o motivo predominante (`sem slot M.2 livre` / `sem porta SATA livre`) e oferece `[0] Voltar`.

**Tarefas:**
- ⬜ **ST-008.1 (CLI):** `EtapaArmazenamento` (`pcbuilder/cli/etapas/EtapaArmazenamento.java`) listando as duas mídias juntas com linha de apoio.
- ⬜ **ST-008.2 (Regras):** `FiltroSlotArmazenamento` (C04) considerando slots/portas já ocupados.
- ⬜ **ST-008.3 (Regras):** aviso `L10` (interface × slot) e informativo de mídia, com texto em linguagem simples.
- ⬜ **ST-008.4 (Teste):** placa com 2 slots M.2 → 2 NVMe permitidos; SATA em placa sem portas → oculto; NVMe PCIe 4.0 em placa com slot 3.0 → aviso; lista vazia com motivo.

---

### HU-009: Escolher a fonte (etapa 6)

*Persona: usuário iniciante (leigo)*

COMO usuário iniciante que não sabe calcular quantos watts precisa nem quais cabos existem,
EU QUERO que o sistema calcule o consumo da minha configuração e mostre só as fontes que dão conta **e** têm os conectores que a minha placa de vídeo exige,
PARA QUE eu não compre uma fonte fraca, sem o cabo certo, nem uma exagerada.

**Critérios de aceite:**
- A etapa imprime `=== Etapa 6 de 6 - Fonte (obrigatorio) ===`;
- Antes da lista, o sistema imprime **o que a configuração exige**:

```
Consumo estimado da sua configuracao: 421 W (com margem de 40%: 590 W)
Conectores exigidos: 1x 16-pinos (placa de video) + 1x 8-pinos (placa-mae)
```

- A lista é filtrada (HU-003) por **quatro** condições rígidas:
  1. `fonte.potenciaTotalWatts >= consumoComMargemWatts` (C14);
  2. `fonte.quantidadeConectores16pin12VHPWR >= gpu.quantidadeConectores16pin`;
  3. `fonte.quantidadeConectoresPCIe8pin >= gpu.quantidadeConectores8pin` (e o mesmo para 6 pinos);
  4. `fonte.quantidadeConectoresCPU8pin >= placaMae.quantidadeConectoresCPU8pin`;
- Se a etapa 4 foi **pulada** (CPU com iGPU), as condições 2 e 3 não se aplicam e o cabeçalho mostra `Conectores exigidos: 1x 8-pinos (placa-mae)`;
- **Não existe caminho de adaptador:** fonte sem o conector exigido é ocultada, mesmo com potência sobrando; `[M]` mostra o motivo (`incompativel: sem conector de 16 pinos (a placa de video exige 1)`);
- Cada linha mostra `#`, `Modelo`, `Preco (R$)` e a linha de apoio `750 W | 80 Plus Gold | 12V: 744 W | 1x 16-pinos | 2x 8-pinos`;
- Avisos (nunca bloqueiam): `L09` quando a carga fica fora de 40–60% (ex.: fonte de 1000 W para consumo de 421 W) e `RISCO` quando `picoTransienteWatts` da GPU excede `potenciaTrilho12VWatts`;
- Se nenhuma fonte for compatível: `Nenhuma fonte compativel. Sua configuracao precisa de 590 W e de 1 conector de 16 pinos.` + `[0] Voltar`;
- Ao escolher, imprime `Fonte selecionada: ... - R$ ...` e o fluxo **avança automaticamente para o resumo** (HU-010);
- `[C] Ver calculo do consumo` detalha a soma componente por componente.

**Tarefas:**
- ⬜ **ST-009.1 (Regras):** `CalculadoraConsumo` (`pcbuilder/analise/CalculadoraConsumo.java`) com `consumoTotalSistemaWatts` (CPU + GPU + placa-mãe + RAM + SSD + reserva de fans), `consumoComMargemWatts` (× `MARGEM_SEGURANCA_FONTE`) e detalhamento por componente.
- ⬜ **ST-009.2 (Regras):** `RequisitosFonte` (`pcbuilder/analise/RequisitosFonte.java`) que deriva de `gpu.quantidadeConectores*` e `placaMae.quantidadeConectoresCPU8pin` a lista de conectores exigidos (usada no cabeçalho **e** no filtro, sem duplicar regra).
- ⬜ **ST-009.3 (Regras):** `FiltroConectoresFonte` (C10) + `FiltroFonteSuficiente` (C14) consumindo `RequisitosFonte` e `CalculadoraConsumo`; `CalculadoraFonte` para os avisos `L06`/`L09`.
- ⬜ **ST-009.4 (CLI):** `EtapaFonte` (`pcbuilder/cli/etapas/EtapaFonte.java`) com o bloco de requisitos antes da lista, `[C]` e `[M]`.
- ⬜ **ST-009.5 (CLI):** transição automática para o resumo após a escolha e mensagem de "nenhuma fonte compatível" com os requisitos calculados.
- ⬜ **ST-009.6 (Teste):** GPU `1x 16-pinos` → fontes sem 16 pinos ocultas (motivo correto); GPU `2x 8-pinos` → fonte com 1x 8-pinos oculta; GPU pulada → nenhuma exigência de conector PCIe; consumo 700 W → fonte de 750 W oculta e de 850 W listada; fonte de 1200 W gera aviso de eficiência.

---

### HU-010: Ver o resumo final com as peças e os preços

*Persona: usuário iniciante (leigo)*

COMO usuário iniciante que terminou de escolher as 6 peças,
EU QUERO ver um resumo final com cada peça escolhida, o preço de cada uma e o total,
PARA QUE eu saiba exatamente o que vou comprar e quanto vai custar.

**Critérios de aceite:**
- Ao concluir a etapa 6, o sistema imprime `=== Resumo da sua montagem ===` (somente com as 6 etapas concluídas; caso contrário, `Complete as etapas pendentes: <lista>.`);
- A tabela tem as colunas `Etapa`, `Categoria`, `Peca escolhida`, `Preco (R$)`, na ordem fixa do fluxo (1 Processador → 6 Fonte);
- A peça pulada da etapa 4 aparece como `(nao selecionado)` e **não** soma no total;
- O total é impresso como `TOTAL: R$ 5.345,00`, formatado em pt-BR e somado em `BigDecimal` a partir do `precoMedioBRL` do mock;
- Preço ausente no mock imprime `R$ --` e acrescenta `(*) inclui peca sem preco no catalogo` no rodapé;
- O resumo repete os **avisos de limitação acumulados** (HU-004) em um bloco curto `Avisos: 1`, uma linha por aviso (`[!] Memoria roda a 6000 MT/s (a placa-mae limita a 6000)`);
- O rodapé oferece `[T] Trocar uma peca` (HU-011), `[N] Nova montagem` e `[0] Sair`;
- O resumo usa as **instâncias já escolhidas** (nunca reconsulta o catálogo), garantindo que o preço exibido é o da peça efetivamente selecionada;
- O resumo não mostra campos técnicos crus (`tdpWatts`, `vrmFasesCPU`): só o necessário para o leigo decidir.

**Tarefas:**
- ⬜ **ST-010.1 (Relatório):** `ResumoMontagem` (`pcbuilder/relatorio/ResumoMontagem.java`) com as linhas (etapa, categoria, peça, preço), total e avisos.
- ⬜ **ST-010.2 (Relatório):** `FormatadorMoeda` (`java.text.NumberFormat` com `Locale("pt","BR")`) e `TabelaResumo` com largura fixa de 100 colunas.
- ⬜ **ST-010.3 (CLI):** `TelaResumo` (`pcbuilder/cli/TelaResumo.java`) com a tabela, o bloco de avisos e o rodapé de comandos.
- ⬜ **ST-010.4 (Regras):** validação de "montagem completa" (6 etapas, respeitando a obrigatoriedade condicional da GPU) antes de exibir o resumo.
- ⬜ **ST-010.5 (Teste):** resumo com GPU pulada; resumo com peça sem preço; total conferido com os preços do mock; montagem incompleta (mensagem de pendências).

---

### HU-011: Trocar uma peça a partir do resumo

*Persona: usuário iniciante (leigo)*

COMO usuário iniciante que viu o resumo e não gostou de uma peça (ou do preço),
EU QUERO voltar direto naquela etapa e trocar só aquela peça,
PARA QUE eu ajuste a configuração sem refazer as outras cinco escolhas.

**Critérios de aceite:**
- `[T] Trocar uma peca` no resumo lista as 6 etapas com a peça atual e o preço (`1 Processador - AMD Ryzen 7 9700X - R$ 2.199,00`);
- Escolher uma etapa reabre **aquela** etapa com o filtro (HU-003) e os avisos (HU-004) recalculados para o estado atual;
- As demais peças são preservadas;
- Os avisos da peça substituída são **descartados**; os da nova peça passam a valer;
- Após a troca, o sistema **revalida a montagem inteira** e, se a nova peça invalidar outra já escolhida, o resumo não é liberado:
  - `Configuracao invalida: a etapa 2 (Placa-Mae) precisa ser revista - soquete AM5 x LGA1700`;
  - `Configuracao invalida: a etapa 6 (Fonte) precisa ser revista - a fonte nao tem conector de 16 pinos (a placa de video exige 1)`;
- O sistema leva o usuário direto à etapa apontada;
- O total do resumo é recalculado (`BigDecimal`) após qualquer troca;
- Em etapa opcional já preenchida existe `[R] Remover peca` (volta a `(nao selecionado)`);
- Nenhuma troca é aplicada sem confirmação `S/N` quando a peça já existe;
- `[0] Voltar` retorna ao resumo sem alterar nada.

**Tarefas:**
- ⬜ **ST-011.1 (Regras):** `MontagemGuiada.reabrirEtapa(int etapa)` e `substituir(categoria, componente)` preservando as demais categorias.
- ⬜ **ST-011.2 (Regras):** `RevalidadorMontagem` (`pcbuilder/analise/filtro/RevalidadorMontagem.java`) rodando as regras duras (inclusive C10/C14) sobre as peças **já escolhidas** e devolvendo as etapas inválidas com o motivo.
- ⬜ **ST-011.3 (CLI):** ação `[T]` na `TelaResumo`, `[R] Remover` nas etapas opcionais e navegação de volta ao resumo.
- ⬜ **ST-011.4 (Regras):** `Montagem.limparLimitacoesDe(categoria)` para descartar os avisos da peça substituída.
- ⬜ **ST-011.5 (Teste):** trocar só a RAM mantendo o resto; trocar CPU para outro soquete (etapa 2 apontada); trocar GPU por uma que exige 16 pinos com fonte de 8 pinos (etapa 6 apontada); remover GPU em CPU com iGPU; total recalculado.

---

## 7. Rastreabilidade HU → regras do modelo de dados

| HU | Regras / seções usadas | Papel na feature principal |
| --- | --- | --- |
| HU-001 | Seção 18 (checklist) + seções 2–8 (campos) | Pré-requisito |
| HU-002 | C15 (montagem obrigatória) | **Etapa 1** |
| HU-003 | C01, C02, C03, C04, C05, C06, C10, C12, C14 (filtro **hard**) | Motor de todas as etapas |
| HU-004 | L01, L02, L04–L07, L09–L11 + alertas de C02, C03, C05, C07, C09 (avisos **soft**) | Motor de todas as etapas |
| HU-005 | C01, C09, L02 | **Etapa 2** |
| HU-006 | C02, C03, L04 | **Etapa 3** |
| HU-007 | C05, C06, L01, L05, L07 | **Etapa 4** (condicional) |
| HU-008 | C04, L05, L10 | **Etapa 5** |
| HU-009 | C10, C14, L06, L09 | **Etapa 6** |
| HU-010 | Todas (consolidação) + `precoMedioBRL` | **Resumo final** |
| HU-011 | C01–C14 (revalidação) | Revisão pós-resumo |

**Fora de escopo do trabalho:** `C08` (gabinete), `C11` (cooler), `C13` (cooler × RAM), `L03` (cooler ↔ CPU) e `L08` (RAM/disco por finalidade) — dependem de peças ou de perfil de uso que o fluxo não coleta.

---

## 8. Fases de implementação

| Fase | HUs | Entrega verificável |
| --- | --- | --- |
| **1 — Esqueleto** | HU-001, HU-002 | Base mockada validada + etapa 1 funcionando no terminal |
| **2 — Inteligência** | HU-003, HU-004 | Listas filtradas e avisos de limitação (caso 8000 × 6000 fechado) |
| **3 — Meio do fluxo** | HU-005, HU-006 | Etapas 2 e 3 com filtro por soquete e por tipo/capacidade de memória |
| **4 — Fim do fluxo** | HU-007, HU-008, HU-009 | Etapas 4 (condicional), 5 e 6 (fonte por consumo **e** conectores) |
| **5 — Resumo final** | HU-010 | Resumo final com peças, preços do mock e total |
| **6 — Revisão** | HU-011 | Trocar peça a partir do resumo, com revalidação da montagem |
| **7 — Encerramento** | — | Remover o conversor do OpenDB e os scripts `gerar-base.*` (checklist da [seção 10](#10-checklist-de-encerramento-limpeza-antes-da-entrega)) |

---

## 9. Critério de aceite — roteiro de sessão

Sessão esperada no terminal (usuário leigo, sem digitar especificação técnica):

```
[1] Montar PC (guiado)   [0] Sair
> 1

=== Etapa 1 de 6 - Processador (obrigatorio) ===
[Processador] [Placa-mae] [Memoria] [Video] [Armazenamento] [Fonte]
 1) AMD Ryzen 7 9700X        - R$ 2.199,00   8 nucleos | AM5 | video integrado: sim
 2) AMD Ryzen 5 7600X        - R$ 1.199,00   6 nucleos | AM5 | video integrado: sim
> 1
Processador selecionado: AMD Ryzen 7 9700X - R$ 2.199,00

=== Etapa 2 de 6 - Placa-Mae (obrigatorio) ===
 1) ASUS Prime B650M-A         - R$   949,00  chipset B650 | MATX | 4 slots DDR5 (max 6000 MT/s) | VRM 8x50A
 2) ASUS TUF Gaming B650-Plus  - R$ 1.349,00  chipset B650 | ATX  | 4 slots DDR5 (max 7600 MT/s) | VRM 12x60A
2 componentes compativeis (1 oculto por incompatibilidade)   [M] ver ocultos
> 1
Placa-mae selecionada: ASUS Prime B650M-A - R$ 949,00

=== Etapa 3 de 6 - Memoria (obrigatorio) ===
 1) Corsair Vengeance 2x16 6000 CL30 - R$ 799,00
 2) Corsair Vengeance 2x16 8000 CL38 - R$ 999,00  [!]
> 2
[!] A placa-mae ASUS Prime B650M-A suporta ate 6000 MT/s.
    Esta memoria (8000 MT/s) vai rodar a 6000 MT/s: perda de ~25% da frequencia.
    A escolha continua valida.
[Enter] para continuar
>
Memoria selecionada: Corsair Vengeance 2x16 8000 - R$ 999,00 (2x16 GB = 32 GB, Dual Channel)

=== Etapa 4 de 6 - Placa de Video (opcional) ===
 1) NVIDIA RTX 4070 Super 12GB       - R$ 3.999,00
 2) NVIDIA RTX 4060 8GB              - R$ 1.899,00  [!]
[0] Pular (seu processador tem video integrado)
> 0
Placa de video pulada: o seu processador ja tem video integrado.

=== Etapa 5 de 6 - Armazenamento (obrigatorio) ===
 1) Kingston KC3000 2TB NVMe - R$ 899,00
 2) Seagate Barracuda 2TB HD - R$ 399,00
> 1
Armazenamento selecionado: Kingston KC3000 2TB - R$ 899,00

=== Etapa 6 de 6 - Fonte (obrigatorio) ===
Consumo estimado da sua configuracao: 201 W (com margem de 40%: 281 W)
Conectores exigidos: 1x 8-pinos (placa-mae)
 1) Corsair CX450 450W Bronze   - R$ 299,00  450 W | 12V: 450 W | 2x 8-pinos | 1x 8-pinos CPU
 2) MSI MAG A750GL 750W Gold    - R$ 549,00  750 W | 12V: 744 W | 1x 16-pinos | 2x 8-pinos | 1x 8-pinos CPU  [!]
> 1
Fonte selecionada: Corsair CX450 450W - R$ 299,00

=== Resumo da sua montagem ===
Etapa | Categoria      | Peca escolhida                   | Preco (R$)
  1   | Processador    | AMD Ryzen 7 9700X                |  2.199,00
  2   | Placa-Mae      | ASUS Prime B650M-A               |    949,00
  3   | Memoria        | Corsair Vengeance 2x16 8000 MT/s |    999,00
  4   | Placa de Video | (nao selecionado)                |       --
  5   | Armazenamento  | Kingston KC3000 2TB NVMe         |    899,00
  6   | Fonte          | Corsair CX450 450W               |    299,00
TOTAL: R$ 5.345,00

Avisos: 1
[!] Memoria roda a 6000 MT/s (a placa-mae limita a 6000)

[T] Trocar uma peca   [N] Nova montagem   [0] Sair
```

**O que esse roteiro prova:**

1. A memória de 8000 MT/s **aparece** e **foi selecionada** — o aviso não bloqueia (requisito central).
2. A GPU foi **pulada** porque a CPU tem vídeo integrado, e o resumo mostra `(nao selecionado)`.
3. **Nenhuma** especificação técnica foi digitada: só números de opção.
4. O total soma apenas os preços do mock e ignora a etapa pulada.
5. Componentes incompatíveis nunca aparecem na lista principal (só em `[M]`).
6. A fonte de 750 W aparece com `[!]` (carga de 27%, fora da faixa de eficiência) e a de 450 W não — **aviso não bloqueia** também na última etapa.

### Variação obrigatória: com placa de vídeo (filtro por conectores)

Se a etapa 4 fosse preenchida com a **RTX 4070 Super (1x 16-pinos)** em vez de pulada:

```
=== Etapa 6 de 6 - Fonte (obrigatorio) ===
Consumo estimado da sua configuracao: 421 W (com margem de 40%: 590 W)
Conectores exigidos: 1x 16-pinos (placa de video) + 1x 8-pinos (placa-mae)
 1) MSI MAG A750GL 750W Gold  - R$ 549,00  750 W | 1x 16-pinos | 2x 8-pinos | 1x 8-pinos CPU
1 componente compativel (1 oculto por incompatibilidade)   [M] ver ocultos
```

E em `[M] ver ocultos`: `Corsair CX450 - incompativel: sem conector de 16 pinos (a placa de video exige 1)`.

> É este comportamento que garante o requisito: **a GPU exige `X` conectores → o sistema só exibe fontes com `X` ou mais**, e a fonte continua sendo a última etapa justamente para que o consumo e os conectores sejam calculados com todas as peças já escolhidas.

---

## 10. Checklist de encerramento (limpeza antes da entrega)

**Status:** ⬜ **pendente** — executar na última fase do trabalho, antes de entregar.

O conversor do OpenDB é útil **enquanto os dados ainda podem mudar** (as HUs 003/004 em
diante costumam revelar campo faltando ou heurística errada). Na entrega ele **não
deve mais existir**: o programa não o usa e ele só carrega código de apoio. **Esta
seção existe para não esquecermos disso.**

### 10.1 O que existe hoje por ser ferramenta (não por ser produto)

| Item | Tamanho | O app usa? |
| --- | --- | --- |
| `tools/java/pcbuilder/ferramentas/` — 10 classes (`ConversorOpenDb`, 6 `Mapeador*`, `Heuristicas`, `Estatisticas`, `Registro`) | ~1.600 linhas / 88 KB | **Não**: `compilar.*`, `testar.*` e `executar.*` não referenciam `tools/` (verificado: 0 ocorrências) |
| `scripts/gerar-base.sh` e `.bat` | ~2 KB | **Não**: servem apenas para regenerar a base |
| `src/main/resources/base/componentes.json` | 8,7 MB | **Sim** — é a base do programa (**manter**) |
| `docs/licensing/` (`LICENSE.txt`, `NOTICE.md`) | ~23 KB | Atribuição **CC BY-NC-SA 4.0** do dado de origem (**manter**) |
| `buildcores-open-db/` (~298 MB) | — | Já removido: só servia para regenerar |

### 10.2 Passos da remoção

1. `rm -rf tools`
2. `rm -f scripts/gerar-base.sh scripts/gerar-base.bat`
3. `docs/base-mockada.md`: substituir as seções **1 (Como a base é gerada)**, **7
   (Regenerar a base)** e **8 (Origem/licença)** por um parágrafo curto — o
   `componentes.json` passa a ser a **fonte da verdade** e o conversor foi usado uma
   única vez, ficando fora do repositório (manter só a atribuição CC BY-NC-SA);
4. `README.md`: tirar `tools/java/...` da árvore de pastas e o comando `gerar-base`,
   deixando a seção "Base de componentes" apenas informando que o JSON já está
   versionado;
5. `README.md`: o mesmo (sem `tools/`, sem `gerar-base`, sem menção ao
   catálogo);
6. Conferir que nada mais aponta para o que saiu:
   `grep -rn 'gerar-base\|ferramentas\|buildcores-open-db' README.md docs/*.md`
7. Rodar tudo do zero: `rm -rf build && scripts/testar.sh &&
   scripts/executar.sh --validar-base` → esperado `OK: 34 verificacoes,
   0 falha(s).` e `Base carregada: 17230 componentes, 0 erro(s), 0 aviso(s).`
8. Marcar esta seção como ✅ e anotar a data da remoção.

### 10.3 Quando **não** remover

Se o grupo decidir continuar mexendo nos dados (heurística nova, catálogo mais novo,
correção de campo), mantenha o conversor: ele é a única forma de reconstruir o
`componentes.json` sem editar 17 mil registros à mão. Nesse caso, deixe o status desta
seção como ⬜ e reabra a decisão no fim da fase correspondente.

