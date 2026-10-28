# Modelo de Dados dos Componentes

Especificação dos dados que o programa precisa armazenar de **cada componente** para ser capaz de:

1. **Verificar compatibilidade** entre as peças escolhidas (encaixe físico, suporte elétrico, padrões).
2. **Detectar limitações** (*gargalos*) — quando um componente impede outro de entregar 100% do seu desempenho.

Documentos relacionados:

* [`exemplo-de-componentes.md`](./exemplo-de-componentes.md) — ficha comercial/informativa das peças de referência.
* [`analise-tecnica.md`](./analise-tecnica.md) — justificativa dos cálculos e das relações entre pares de componentes.
* [`historias-de-usuario.md`](./historias-de-usuario.md) — **11 HUs** (escopo fechado do trabalho) e tasks que implementam estas regras na CLI.

---

## 1. Convenções deste documento

| Regra | Descrição |
| --- | --- |
| **1 campo = 1 informação** | Nunca juntar valor + unidade + observação em um único campo. `"4x slots DDR5"` → `quantidadeSlotsMemoria: 4` + `tipoMemoria: DDR5`. |
| **Unidade fixa e embutida no nome** | O número é armazenado puro (`int`/`double`) e a unidade vai no nome do campo: `frequenciaBoostMHz`, `tdpWatts`, `comprimentoMm`, `capacidadePorPenteGB`. Assim nenhuma comparação precisa converter texto. |
| **Nada de texto livre para dados comparáveis** | Soquete, chipset, formato, certificação etc. são `enum`, nunca `String`. `String` só para dados que nunca serão comparados (`modelo`, `fabricante`, `observacoes`). |
| **`null` ≠ `0`** | Campo que não se aplica ao componente fica `null` (`Integer`, `Double`, `Boolean` — tipos wrapper, não primitivos). `0` significa "existe e vale zero". Ex.: RAM DDR5 não tem `interfaceSATAGbps` → `null`. |
| **Armazenar o dado, nunca o resultado da conta** | `consumoTotalSistemaWatts`, `percentualCargaFonte`, `bandaEfetivaPCIeGBs` são **calculados em tempo de execução** (seção 16). Guardá-los gera dado duplicado e desatualizado. |
| **Modelo sem especificação embutida** | `modelo: "Vengeance RGB"` e não `"Vengeance RGB 32GB (2x16GB)"` — a capacidade já vive em `capacidadePorPenteGB` × `quantidadePentes`. |
| **Lista em vez de sufixos** | Quando o componente aceita vários valores do mesmo tipo (soquetes do cooler, formatos do gabinete), usar `List<enum>`, não `soquete1`, `soquete2`. |

### 1.1 Exemplos de campos ruins → campos bons

| ❌ Campo único (ruim) | ✅ Campos separados (bom) |
| --- | --- |
| `"Slots de Memória": "4x slots DDR5"` | `quantidadeSlotsMemoria: 4` + `tipoMemoria: DDR5` |
| `"Frequência (Clock)": "3.8 GHz (Base) até 5.5 GHz (Boost)"` | `frequenciaBaseMHz: 3800` + `frequenciaBoostMHz: 5500` |
| `"TDP (Consumo)": "65W (Base) / ~140W (PPT/Turbo)"` | `tdpWatts: 65` + `consumoMaximoWatts: 140` |
| `"Interface": "M.2 2280 / NVMe PCIe 4.0 x4"` | `formato: M2_2280` + `interface: NVME` + `versaoPCIe: 4` + `pistasPCIe: 4` |
| `"Conector de Energia": "1x 16-pinos ou adaptador de 2x 8-pinos"` | `quantidadeConectores16pin: 1` + `quantidadeConectores8pin: 0` |
| `"Certificação": "80 Plus Gold"` | `certificacao80Plus: GOLD` |

### 1.2 Legenda das colunas das fichas

| Coluna | Significado |
| --- | --- |
| **Campo** | Nome sugerido (camelCase, em português, unidade no final). |
| **Tipo** | Tipo Java sugerido. `Integer`/`Double`/`Boolean` (wrapper) quando o campo pode ser nulo. |
| **Unidade** | Unidade de medida do número armazenado (`—` para textos, enums e booleanos). |
| **Exemplo** | Valor da configuração de referência ([`exemplo-de-componentes.md`](./exemplo-de-componentes.md)). |
| **Obrig.** | `Sim` (sem ele nenhuma análise roda), `Não` (só informativo) ou `Cond.` (obrigatório em certos casos). |
| **Regras** | IDs das regras (seções 13 e 14) que **usam** esse campo. `—` = campo puramente informativo. |

### 1.3 Escopo do trabalho (o que o fluxo guiado realmente usa)

O modelo abaixo descreve **todos** os componentes, mas o programa entrega um fluxo guiado de **6 etapas** ([`historias-de-usuario.md`](./historias-de-usuario.md)). Só os itens marcados como **ativos** são coletados e avaliados:

| Item | No fluxo? | Consequência no modelo |
| --- | --- | --- |
| Processador, Placa-Mãe, Memória, Placa de Vídeo, Armazenamento, Fonte | ✅ **Ativos** | Campos `Sim` são obrigatórios no mock |
| Cooler, Gabinete, Monitor | ❌ Fora | Tabelas documentadas para fases futuras; regras dependentes ficam `NAO APLICAVEL` |
| Perfil de Uso (finalidade, orçamento) | ❌ Fora | `L01`/`L07` usam `gpu.resolucaoAlvo`; `L08` **não** é avaliada |
| Preço (`precoMedioBRL`) | ✅ **Ativo** | Obrigatório no mock: o resumo final soma os preços |

---

## 2. Campos comuns a todos os componentes

Superclasse `Componente` (herdada por CPU, Placa-Mãe, RAM, GPU, Armazenamento, Fonte, Cooler e Gabinete).

| Campo | Tipo | Unidade | Exemplo | Obrig. | Regras |
| --- | --- | --- | --- | --- | --- |
| `id` | `String` | — | `"cpu-amd-ryzen-7-9700x"` | Sim | — |
| `modelo` | `String` | — | `"Ryzen 7 9700X"` | Sim | — |
| `fabricante` | `String` | — | `"AMD"` | Sim | — |
| `categoria` | `enum CategoriaComponente` | — | `CPU` | Sim | C15 |
| `consumoEstimadoWatts` | `Integer` | W | `6` | Cond. | L06 |
| `precoMedioBRL` | `BigDecimal` | R$ | `2199.00` | Sim | — |
| `garantiaMeses` | `Integer` | meses | `36` | Não | — |
| `anoLancamento` | `Integer` | ano | `2024` | Não | — |
| `observacoes` | `String` | — | `"Requer BIOS 2611 ou superior"` | Não | — |

> `consumoEstimadoWatts` existe em **todos** os componentes porque a fonte precisa somar o sistema inteiro (placa-mãe, RAM, SSD, coolers, fans). Ele é `Cond.` porque **CPU e GPU não o usam**: nelas vale o consumo máximo real (`consumoMaximoWatts` / `tdpWatts`).
>
> `precoMedioBRL` é `Sim` porque o **resumo final** (HU-010) soma os preços de cada peça e exibe o total; peça sem preço no mock apareceria como `R$ --` e contaminaria o total.

---

## 3. Processador (CPU)

**Classe:** `Processador extends Componente`

| Campo | Tipo | Unidade | Exemplo | Obrig. | Regras |
| --- | --- | --- | --- | --- | --- |
| `soquete` | `enum Soquete` | — | `AM5` | Sim | C01, C11 |
| `nucleosFisicos` | `int` | núcleos | `8` | Sim | L01 |
| `threads` | `int` | threads | `16` | Sim | L01 |
| `frequenciaBaseMHz` | `int` | MHz | `3800` | Sim | L01 |
| `frequenciaBoostMHz` | `int` | MHz | `5500` | Sim | L01 |
| `tdpWatts` | `int` | W | `65` | Sim | L03, L06 |
| `consumoMaximoWatts` | `int` | W | `140` | Sim | L02, L03, L06 |
| `tensaoMaximaVolts` | `Double` | V | `1.25` | Cond. | L02 |
| `tiposMemoriaSuportados` | `List<enum TipoMemoria>` | — | `[DDR5]` | Sim | C02 |
| `canaisMemoria` | `int` | canais | `2` | Sim | C03, L04 |
| `frequenciaMemoriaNativaMTs` | `int` | MT/s | `5600` | Sim | L04 |
| `frequenciaMaximaSincronaMTs` | `Integer` | MT/s | `6000` | Cond. | L04 |
| `capacidadeMaximaMemoriaGB` | `int` | GB | `192` | Sim | C03 |
| `suportaECC` | `boolean` | — | `true` | Sim | C12 |
| `versaoPCIeMaxima` | `int` | geração | `5` | Sim | C05, L11 |
| `pistasPCIeLivres` | `int` | lanes | `24` | Sim | L11 |
| `possuiVideoIntegrado` | `boolean` | — | `true` | Sim | C06 |
| `desbloqueadoOverclock` | `boolean` | — | `true` | Sim | C07 |
| `indiceDesempenhoSingleThread` | `int` | pontos (0–100) | `92` | Sim | L01 |
| `indiceDesempenhoMultithread` | `int` | pontos (0–100) | `88` | Sim | L01 |

**Observações de modelagem:**

* `tensaoMaximaVolts` — usada apenas para estimar a corrente exigida pela CPU (`I = P / V`). Se ausente, a regra **L02** assume o parâmetro `TENSAO_REFERENCIA_CPU` (1.25 V).
* `frequenciaMaximaSincronaMTs` — teto em que o controlador de memória (IMC) ainda trabalha em proporção **1:1** (*sweet spot*). É diferente do suporte JEDEC nativo (`frequenciaMemoriaNativaMTs`) e é esse valor que limita a RAM na regra **L04**.
* `pistasPCIeLivres` — pistas que a CPU entrega diretamente para GPU + NVMe, **sem contar** as do chipset.
* `indiceDesempenho*` — nota relativa normalizada (0–100) obtida de benchmarks; é a única base viável para comparar CPUs de famílias diferentes na regra de gargalo **L01**.

---

## 4. Placa-Mãe (Motherboard)

**Classe:** `PlacaMae extends Componente`

### 4.1 Soquete, chipset e formato

| Campo | Tipo | Unidade | Exemplo | Obrig. | Regras |
| --- | --- | --- | --- | --- | --- |
| `soqueteCPU` | `enum Soquete` | — | `AM5` | Sim | C01 |
| `chipset` | `enum Chipset` | — | `B650` | Sim | C07 |
| `formato` | `enum FormatoPlacaMae` | — | `ATX` | Sim | C08 |
| `comprimentoMm` | `int` | mm | `305` | Sim | C08 |
| `larguraMm` | `int` | mm | `244` | Sim | C08 |
| `suportaAtualizacaoBIOSSemCPU` | `boolean` | — | `true` | Sim | C09 |

### 4.2 VRM (fornecimento de energia para a CPU)

| Campo | Tipo | Unidade | Exemplo | Obrig. | Regras |
| --- | --- | --- | --- | --- | --- |
| `vrmFasesCPU` | `int` | fases | `12` | Sim | L02 |
| `vrmCorrentePorFaseA` | `int` | A | `60` | Sim | L02 |
| `vrmPossuiDissipador` | `boolean` | — | `true` | Não | L02 |

### 4.3 Memória RAM

| Campo | Tipo | Unidade | Exemplo | Obrig. | Regras |
| --- | --- | --- | --- | --- | --- |
| `quantidadeSlotsMemoria` | `int` | slots | `4` | Sim | C03 |
| `tipoMemoria` | `enum TipoMemoria` | — | `DDR5` | Sim | C02 |
| `canaisMemoria` | `int` | canais | `2` | Sim | C03, L04 |
| `capacidadeMaximaMemoriaGB` | `int` | GB | `192` | Sim | C03 |
| `frequenciaMemoriaMaximaMTs` | `int` | MT/s | `7600` | Sim | L04 |
| `suportaPerfilEXPO` | `boolean` | — | `true` | Sim | C02 |
| `suportaPerfilXMP` | `boolean` | — | `true` | Sim | C02 |
| `suportaECC` | `boolean` | — | `false` | Sim | C12 |

### 4.4 Slots de expansão e armazenamento

| Campo | Tipo | Unidade | Exemplo | Obrig. | Regras |
| --- | --- | --- | --- | --- | --- |
| `quantidadeSlotsM2` | `int` | slots | `2` | Sim | C04 |
| `geracaoMaximaSlotM2` | `int` | geração PCIe | `5` | Sim | C04, L05 |
| `geracaoMinimaSlotM2` | `int` | geração PCIe | `4` | Sim | C04, L05 |
| `quantidadeSlotsPCIe16` | `int` | slots | `1` | Sim | C05 |
| `versaoSlotPCIe16` | `int` | geração PCIe | `4` | Sim | C05, L05 |
| `pistasSlotPCIe16` | `int` | lanes | `16` | Sim | L05 |
| `quantidadePortasSATA` | `int` | portas | `4` | Sim | C04 |
| `velocidadeSATAGbps` | `int` | Gb/s | `6` | Sim | L05 |

### 4.5 Conectores de energia (para casar com a fonte)

| Campo | Tipo | Unidade | Exemplo | Obrig. | Regras |
| --- | --- | --- | --- | --- | --- |
| `quantidadeConectoresCPU8pin` | `int` | conectores | `1` | Sim | C10 |
| `quantidadeConectoresCPU4pin` | `int` | conectores | `0` | Não | C10 |
| `quantidadeConectoresATX24pin` | `int` | conectores | `1` | Sim | C10 |

**Observações de modelagem:**

* **Slots M.2 diferentes entre si:** os campos `geracaoMaximaSlotM2` / `geracaoMinimaSlotM2` resolvem o caso comum (todos os slots iguais ou um conjunto de gerações). Se a placa tiver slots com **pistas diferentes** (`x4` e `x2`, por exemplo), substituir por uma lista de `SlotM2 { quantidade, geracaoPCIe, pistas, suportaSATA }` — a lista nunca junta duas informações em um campo.
* **Placas híbridas (DDR4 + DDR5 coexistentes):** raras, mas exigem `List<GrupoSlotsMemoria> { tipoMemoria, quantidadeSlots, canais }>`. Nas demais, os campos planos de 4.3 bastam.
* `capacidadeMaximaMemoriaGB` deve ser comparada com a **mesma informação da CPU**: vale o menor valor entre as duas (regra **C03**).
* A versão de BIOS necessária para cada CPU é uma tabela à parte (`RequisitoBIOS`), com chave composta `(idPlacaMae, idProcessador)` — ver regra **C09**.

---

## 5. Memória RAM

**Classe:** `Memoria extends Componente`

| Campo | Tipo | Unidade | Exemplo | Obrig. | Regras |
| --- | --- | --- | --- | --- | --- |
| `tipo` | `enum TipoMemoria` | — | `DDR5` | Sim | C02 |
| `capacidadePorPenteGB` | `int` | GB | `16` | Sim | C03, L08 |
| `quantidadePentes` | `int` | pentes | `2` | Sim | C03 |
| `frequenciaMTs` | `int` | MT/s | `6000` | Sim | L04, L08 |
| `frequenciaJEDECBaseMTs` | `Integer` | MT/s | `4800` | Não | L04 |
| `latenciaCAS` | `int` | ciclos (CL) | `30` | Sim | L04 |
| `tensaoVolts` | `Double` | V | `1.35` | Não | — |
| `perfilEXPO` | `boolean` | — | `true` | Sim | C02 |
| `perfilXMP` | `boolean` | — | `true` | Sim | C02 |
| `possuiECC` | `boolean` | — | `false` | Sim | C12 |
| `alturaMm` | `Integer` | mm | `44` | Cond. | C13 |

**Observações de modelagem:**

* **"6000 MHz" é na prática 6000 MT/s** (transferências por segundo). Armazenar como `frequenciaMTs` evita erro de unidade nas contas de banda e de latência.
* `capacidadeTotalGB` **não é armazenado**: é derivado (`capacidadePorPenteGB × quantidadePentes`).
* `quantidadePentes` é necessário porque define o **Dual Channel** e quantos slots da placa-mãe serão ocupados (regra **C03**).
* `alturaMm` só é obrigatório em pentes altos (com dissipador grande), pois é o que pode colidir com o cooler (regra **C13**).
* `frequenciaJEDECBaseMTs` — velocidade sem EXPO/XMP. É o valor que a máquina assume se a placa-mãe não suportar o perfil, útil para avisar o usuário do desempenho que ele perderia.

---

## 6. Placa de Vídeo (GPU)

**Classe:** `PlacaVideo extends Componente`

| Campo | Tipo | Unidade | Exemplo | Obrig. | Regras |
| --- | --- | --- | --- | --- | --- |
| `fabricanteChip` | `enum FabricanteChip` | — | `NVIDIA` | Sim | — |
| `memoriaVRAMGB` | `int` | GB | `12` | Sim | L07 |
| `tipoMemoriaVRAM` | `enum TipoMemoriaVRAM` | — | `GDDR6X` | Não | — |
| `versaoPCIe` | `int` | geração PCIe | `4` | Sim | C05, L05 |
| `pistasPCIe` | `int` | lanes | `16` | Sim | C05, L05 |
| `comprimentoMm` | `int` | mm | `305` | Sim | C08 |
| `espessuraSlots` | `int` | slots | `3` | Sim | C08 |
| `tdpWatts` | `int` | W | `220` | Sim | L06 |
| `picoTransienteWatts` | `int` | W | `350` | Cond. | L06 |
| `fonteRecomendadaWatts` | `Integer` | W | `650` | Não | L06 |
| `quantidadeConectores16pin` | `int` | conectores | `1` | Sim | C10 |
| `quantidadeConectores8pin` | `int` | conectores | `0` | Sim | C10 |
| `quantidadeConectores6pin` | `int` | conectores | `0` | Sim | C10 |
| `resolucaoAlvo` | `enum Resolucao` | — | `QHD_1440P` | Sim | L01, L07 |
| `indiceDesempenhoGPU` | `int` | pontos (0–100) | `85` | Sim | L01 |
| `possuiRayTracing` | `boolean` | — | `true` | Não | — |

**Observações de modelagem:**

* `quantidadeConectores16pin` / `8pin` / `6pin` são **campos separados** porque a fonte é comparada conector por conector (regra **C10**). Esses campos são a **exigência** da GPU: a fonte só é compatível se tiver a mesma quantidade — **o modelo não contempla adaptadores**.
* A GPU é a única peça que impõe exigência de conector PCIe; é por isso que a fonte é a **última etapa** do fluxo guiado, depois de GPU e placa-mãe conhecidas.
* `picoTransienteWatts` — picos de milissegundos que desligam a máquina se a fonte não aguentar; entra na conta de folga da fonte (**L06**).
* `espessuraSlots` — quantidade de slots de expansão do gabinete consumidos; `comprimentoMm` — limite de comprimento do gabinete (**C08**).
* `resolucaoAlvo` é a resolução em que a GPU é projetada para trabalhar; é o que define se há gargalo de CPU x GPU (**L01**) e se a VRAM é suficiente (**L07**).

---

## 7. Armazenamento (SSD / HD)

**Classe:** `Armazenamento extends Componente`

| Campo | Tipo | Unidade | Exemplo | Obrig. | Regras |
| --- | --- | --- | --- | --- | --- |
| `tipoMidia` | `enum TipoMidia` | — | `SSD` | Sim | C04 |
| `interface` | `enum InterfaceArmazenamento` | — | `NVME` | Sim | C04 |
| `formato` | `enum FormatoArmazenamento` | — | `M2_2280` | Sim | C04, C08 |
| `capacidadeGB` | `int` | GB | `2000` | Sim | — |
| `versaoPCIe` | `Integer` | geração PCIe | `4` | Cond. | C04, L05 |
| `pistasPCIe` | `Integer` | lanes | `4` | Cond. | L05 |
| `velocidadeSATAGbps` | `Integer` | Gb/s | `null` | Cond. | L05 |
| `leituraSequencialMBs` | `int` | MB/s | `7000` | Sim | L05 |
| `escritaSequencialMBs` | `int` | MB/s | `7000` | Sim | L05 |
| `tbw` | `Integer` | TB | `1600` | Não | — |
| `possuiDRAMCache` | `boolean` | — | `true` | Não | — |
| `consumoMedioWatts` | `Double` | W | `5.0` | Sim | L06 |
| `consumoPicoWatts` | `Double` | W | `8.0` | Não | L06 |

**Observações de modelagem:**

* `versaoPCIe` / `pistasPCIe` só existem para NVMe; em um SSD/HD SATA os dois ficam `null` e `velocidadeSATAGbps` recebe o valor (3 ou 6). Isso é o uso correto de `null`: o campo **não se aplica**, e não vale zero.
* `comprimentoMm` não é armazenado no SSD: o que importa é o **formato** (`M2_2280`, `SATA_2_5`, `SATA_3_5`), que já determina o encaixe no slot M.2 ou na baia do gabinete.
* `velocidadeSATAGbps` aparece aqui **e** na placa-mãe: são dados diferentes (porta × unidade). Quem limita é o menor dos dois (**L05**).
* O tamanho do SSD (`capacidadeGB`) não interfere em compatibilidade, mas serve para a regra de espaço total (**L08**).

---

## 8. Fonte de Alimentação (PSU)

**Classe:** `Fonte extends Componente`

| Campo | Tipo | Unidade | Exemplo | Obrig. | Regras |
| --- | --- | --- | --- | --- | --- |
| `potenciaTotalWatts` | `int` | W | `750` | Sim | L06, L09 |
| `potenciaTrilho12VWatts` | `int` | W | `744` | Sim | L06 |
| `certificacao80Plus` | `enum Certificado80Plus` | — | `GOLD` | Sim | L09 |
| `eficienciaPercentualCarga50` | `Double` | % | `90.0` | Não | L09 |
| `modularidade` | `enum Modularidade` | — | `TOTALMENTE_MODULAR` | Sim | C10 |
| `padraoATX` | `enum PadraoATX` | — | `ATX_3_0` | Sim | C10 |
| `quantidadeConectores16pin12VHPWR` | `int` | conectores | `1` | Sim | C10 |
| `quantidadeConectoresPCIe8pin` | `int` | conectores | `2` | Sim | C10 |
| `quantidadeConectoresPCIe6pin` | `int` | conectores | `0` | Sim | C10 |
| `quantidadeConectoresCPU8pin` | `int` | conectores | `1` | Sim | C10 |
| `quantidadeConectoresCPU4pin` | `int` | conectores | `0` | Sim | C10 |
| `quantidadeConectoresSATA` | `int` | conectores | `4` | Sim | — |
| `quantidadeConectoresMolex` | `int` | conectores | `2` | Não | — |
| `comprimentoMm` | `int` | mm | `160` | Sim | C08 |
| `alturaMm` | `int` | mm | `86` | Não | C08 |
| `possuiPFCAtivo` | `boolean` | — | `true` | Não | — |

**Observações de modelagem:**

* `potenciaTotalWatts` **não** é dividido em raias no cadastro básico; `potenciaTrilho12VWatts` existe porque é dela que saem CPU e GPU — uma fonte de 750 W com apenas 500 W em 12 V não serve para a mesma configuração.
* Cada tipo de conector é um **campo inteiro próprio**: comparar "tem 12VHPWR?" com um campo de texto `"1x 16-pinos ou 2x 8-pinos"` seria impossível sem *parsing*.
* `padraoATX` fica **informativo** no escopo atual: como a regra **C10** exige o **conector físico** presente na fonte (`quantidadeConectores16pin12VHPWR`, `quantidadeConectoresPCIe8pin/6pin`, `quantidadeConectoresCPU8pin`), não existe caminho de adaptador. O enum permanece no cadastro para evolução futura.
* `consumoEstimadoWatts` (campo comum) não se aplica à fonte: ela fornece energia, não consome da própria tomada para o sistema — usar `null`.

---

## 9. Cooler (dissipador da CPU) — fora do escopo do trabalho

Sem esse componente a regra de limitação térmica (**L03**), que é uma das mais importantes, não pode ser calculada.

**Classe:** `Cooler extends Componente`

| Campo | Tipo | Unidade | Exemplo | Obrig. | Regras |
| --- | --- | --- | --- | --- | --- |
| `tipo` | `enum TipoCooler` | — | `AIR` | Sim | C08 |
| `soquetesSuportados` | `List<enum Soquete>` | — | `[AM5, AM4]` | Sim | C01, C11 |
| `tdpSuportadoWatts` | `int` | W | `180` | Sim | L03 |
| `alturaMm` | `Integer` | mm | `158` | Cond. | C08 |
| `dimensaoRadiadorMm` | `Integer` | mm | `null` | Cond. | C08, C13 |
| `espacoLivreMemoriaMm` | `Integer` | mm | `38` | Não | C13 |
| `quantidadeVentiladores` | `int` | fans | `2` | Não | — |
| `ruidoMaximoDba` | `Double` | dBA | `28.0` | Não | — |

**Observações de modelagem:**

* `tipo` define qual campo de dimensão é obrigatório: `AIR` → `alturaMm`; `AIO` → `dimensaoRadiadorMm` (120/240/360), que precisa caber no gabinete.
* `tdpSuportadoWatts` é a **capacidade de dissipação** declarada pelo fabricante — é ela que se compara com o `consumoMaximoWatts` da CPU, nunca com o `tdpWatts` base.
* `espacoLivreMemoriaMm` é a folga horizontal/vertical sobre os slots de RAM; pentes altos (**C13**) podem ser bloqueados por coolers torre largos.

---

## 10. Gabinete (Case) — fora do escopo do trabalho

**Classe:** `Gabinete extends Componente`

| Campo | Tipo | Unidade | Exemplo | Obrig. | Regras |
| --- | --- | --- | --- | --- | --- |
| `formatosPlacaMaeSuportados` | `List<enum FormatoPlacaMae>` | — | `[ATX, MATX, ITX]` | Sim | C08 |
| `comprimentoMaximoGPUmm` | `int` | mm | `360` | Sim | C08 |
| `espessuraMaximaGPUSlots` | `int` | slots | `4` | Sim | C08 |
| `alturaMaximaCoolerMm` | `int` | mm | `165` | Sim | C08 |
| `suportaRadiadorMm` | `List<Integer>` | mm | `[120, 240, 360]` | Não | C08 |
| `quantidadeBaias25` | `int` | baias | `2` | Sim | C04 |
| `quantidadeBaias35` | `int` | baias | `2` | Sim | C04 |
| `quantidadeSlotsExpansao` | `int` | slots | `7` | Sim | C08 |
| `comprimentoMaximoFonteMm` | `int` | mm | `200` | Sim | C08 |
| `quantidadeVentiladoresInstalados` | `int` | fans | `4` | Não | L06 |

---

## 11. Monitor e Perfil de Uso (fora do escopo do trabalho)

**Não são coletados pelo fluxo guiado.** Ficam documentados para fases futuras: o mesmo par CPU+GPU é equilibrado em 1440p e desequilibrado em 1080p, então esses dados enriqueceriam a análise — mas o escopo atual usa `gpu.resolucaoAlvo` como referência de resolução (`L01`, `L07`) e **não avalia `L08`**.

### 11.1 Monitor

**Classe:** `Monitor extends Componente`

| Campo | Tipo | Unidade | Exemplo | Obrig. | Regras |
| --- | --- | --- | --- | --- | --- |
| `resolucaoLarguraPx` | `int` | px | `2560` | Sim | L01, L07 |
| `resolucaoAlturaPx` | `int` | px | `1440` | Sim | L01, L07 |
| `taxaAtualizacaoHz` | `int` | Hz | `165` | Sim | L01 |
| `tamanhoPolegadas` | `Double` | pol | `27.0` | Não | — |
| `conexaoPrincipal` | `enum ConexaoVideo` | — | `DISPLAYPORT_1_4` | Não | — |

### 11.2 Perfil de Uso (entrada do usuário)

| Campo | Tipo | Unidade | Exemplo | Obrig. | Regras |
| --- | --- | --- | --- | --- | --- |
| `finalidade` | `enum Finalidade` | — | `JOGOS` | Sim | L07, L08, L01 |
| `resolucaoAlvo` | `enum Resolucao` | — | `QHD_1440P` | Sim | L01, L07 |
| `fpsAlvo` | `Integer` | FPS | `144` | Não | L01 |
| `orcamentoMaximoBRL` | `BigDecimal` | R$ | `9000.00` | Não | — |
| `priorizaSilencio` | `boolean` | — | `false` | Não | L03 |
| `quantidadeArmazenamentos` | `int` | unidades | `1` | Sim | C04, L08 |
| `capacidadeTotalDesejadaGB` | `Integer` | GB | `2000` | Não | L08 |

---

## 12. Enums necessários (domínios fechados)

Todo campo de enum precisa ter **valores fixos no código**, nunca texto livre digitado pelo usuário.

| Enum | Valores | Exemplo real |
| --- | --- | --- |
| `CategoriaComponente` | `CPU`, `PLACA_MAE`, `MEMORIA`, `PLACA_VIDEO`, `ARMAZENAMENTO`, `FONTE`, `COOLER`, `GABINETE`, `MONITOR` | `CPU` |
| `Soquete` | `AM4`, `AM5`, `LGA1700`, `LGA1851`, `AM3`, `LGA1200` | `AM5` |
| `Chipset` | `A320`, `B350`, `B450`, `A520`, `B550`, `X570`, `A620`, `B650`, `B650E`, `X670`, `X670E`, `B850`, `X870`, `X870E`, `H410`, `B460`, `H510`, `B560`, `Z490`, `Z590`, `H610`, `B660`, `B760`, `Z690`, `Z790`, `B860`, `Z890` | `B650` |
| `FormatoPlacaMae` | `E_ATX`, `ATX`, `MATX`, `MINI_ITX` | `ATX` |
| `TipoMemoria` | `DDR4`, `DDR5` | `DDR5` |
| `FabricanteChip` | `NVIDIA`, `AMD`, `INTEL` | `NVIDIA` |
| `TipoMemoriaVRAM` | `GDDR6`, `GDDR6X`, `GDDR7`, `HBM2` | `GDDR6X` |
| `TipoMidia` | `SSD`, `HDD` | `SSD` |
| `InterfaceArmazenamento` | `NVME`, `SATA` | `NVME` |
| `FormatoArmazenamento` | `M2_2280`, `M2_2242`, `M2_22110`, `SATA_2_5`, `SATA_3_5` | `M2_2280` |
| `Certificado80Plus` | `NENHUM`, `WHITE`, `BRONZE`, `SILVER`, `GOLD`, `PLATINUM`, `TITANIUM` | `GOLD` |
| `Modularidade` | `NAO_MODULAR`, `SEMI_MODULAR`, `TOTALMENTE_MODULAR` | `TOTALMENTE_MODULAR` |
| `PadraoATX` | `ATX_2_4`, `ATX_3_0`, `ATX_3_1` | `ATX_3_0` |
| `TipoCooler` | `AIR`, `AIO` | `AIR` |
| `Resolucao` | `FHD_1080P`, `QHD_1440P`, `UHD_4K` | `QHD_1440P` |
| `ConexaoVideo` | `HDMI_2_1`, `DISPLAYPORT_1_4` | `DISPLAYPORT_1_4` |
| `Finalidade` | `JOGOS`, `ESCRITORIO`, `EDICAO_VIDEO`, `MODELAGEM_3D`, `MACHINE_LEARNING` | `JOGOS` |

> **Regra de ouro dos enums:** se o valor aparece em uma comparação (`==`), ele é enum. Comparações por texto (`"AM5".equalsIgnoreCase(...)`) quebram com um acento ou espaço a mais.
>
> **Domínio de `Chipset` ampliado:** a lista original (`A620`, `B650`, `B650E`, `X670`,
> `X670E`, `H610`, `B760`, `Z790`) cobria só as gerações mais recentes. Ao carregar o
> catálogo OpenDB (ver [`base-mockada.md`](./base-mockada.md)), os chipsets AM4
> (`A320`–`X570`) e Intel `H410`–`Z690`/`B860`–`Z890` apareceram em milhares de
> placas — sem eles, 265 processadores ficariam sem placa-mãe compatível. Os valores
> acrescentados continuam sendo domínio fechado; nenhum texto livre entrou no enum.

---

## 13. Regras de compatibilidade (o que impede a montagem)

| ID | Regra | Campos utilizados | Resultado |
| --- | --- | --- | --- |
| **C01** | Soquete da CPU × soquete da placa-mãe × soquetes do cooler | `cpu.soquete`, `placaMae.soqueteCPU`, `cooler.soquetesSuportados` | ❌ incompatível se diferentes; ❌ se o cooler não lista o soquete |
| **C02** | Tecnologia e perfil de memória | `memoria.tipo`, `placaMae.tipoMemoria`, `cpu.tiposMemoriaSuportados`, `memoria.perfilEXPO/XMP`, `placaMae.suportaPerfilEXPO/XMP` | ❌ incompatível se o tipo não bate; ⚠️ se o perfil não é suportado (roda em JEDEC, perde frequência) |
| **C03** | Quantidade e capacidade de memória | `memoria.quantidadePentes`, `capacidadePorPenteGB`, `placaMae.quantidadeSlotsMemoria`, `capacidadeMaximaMemoriaGB` (placa-mãe **e** CPU), `canaisMemoria` | ❌ se pentes > slots; ❌ se total > menor capacidade máxima; ⚠️ se pentes < canais (single channel) ou slots ficam parcialmente vazios |
| **C04** | Armazenamento × placa-mãe | `armazenamento.interface`, `formato`, `placaMae.quantidadeSlotsM2`, `placaMae.quantidadePortasSATA` | ❌ se não houver slot M.2 (NVMe) ou porta SATA livre (baia de gabinete fica fora do escopo) |
| **C05** | GPU × slot PCIe | `gpu.versaoPCIe`, `gpu.pistasPCIe`, `placaMae.quantidadeSlotsPCIe16`, `placaMae.versaoSlotPCIe16`, `pistasSlotPCIe16` | ❌ se não houver slot x16 livre; ✅ compatível para trás (PCIe 4.0 em 3.0), com ⚠️ de perda de banda |
| **C06** | CPU sem vídeo integrado | `cpu.possuiVideoIntegrado`, existência de GPU | ❌ se a CPU não tem iGPU e não há GPU selecionada |
| **C07** | Overclock | `cpu.desbloqueadoOverclock`, `placaMae.chipset` | ⚠️ CPU desbloqueada em chipset que não permite OC |
| **C08** | Encaixe físico no gabinete | `placaMae.formato`, `comprimentoMm`, `larguraMm`; `gpu.comprimentoMm`, `espessuraSlots`; `cooler.alturaMm`, `dimensaoRadiadorMm`; `fonte.comprimentoMm`; `gabinete.*` | ❌ se qualquer dimensão exceder o limite do gabinete |
| **C09** | BIOS mínima para a CPU | `placaMae.id`, `cpu.id`, `placaMae.suportaAtualizacaoBIOSSemCPU`, tabela `RequisitoBIOS` | ⚠️ "pode exigir atualização de BIOS"; ❌ se exigir e a placa não atualizar sem CPU |
| **C10** | Conectores de energia da fonte (filtro **rígido**) | `gpu.quantidadeConectores16pin/8pin/6pin`, `placaMae.quantidadeConectoresCPU8pin`, `fonte.quantidadeConectores16pin12VHPWR/PCIe8pin/PCIe6pin/CPU8pin/ATX24pin` | ❌ se a fonte tiver **menos** conectores do que a GPU/placa-mãe exige. **Sem caminho de adaptador**: a fonte precisa ter o conector físico (ver `requisitosFonte`, seção 16) |
| **C11** | Cooler × consumo da CPU | `cooler.tdpSuportadoWatts`, `cpu.consumoMaximoWatts` | ❌ se a capacidade de dissipação for menor que o consumo máximo |
| **C12** | ECC | `memoria.possuiECC`, `placaMae.suportaECC`, `cpu.suportaECC` | ❌ se ECC for exigido e algum dos dois não suportar |
| **C13** | Cooler × altura da RAM | `ram.alturaMm`, `cooler.espacoLivreMemoriaMm` | ⚠️ conflito físico entre pente e cooler |
| **C14** | Fonte suficiente para o conjunto | `fonte.potenciaTotalWatts`, `potenciaTrilho12VWatts`, somatório de consumo | ❌ se o consumo com margem exceder a entrega (ver **L06**) |
| **C15** | Montagem obrigatória (6 etapas) | `categoria` de cada componente selecionado, `cpu.possuiVideoIntegrado` | ❌ se faltar CPU, placa-mãe, memória, armazenamento ou fonte; ❌ se a CPU não tem iGPU e não houver GPU (mesmo caso de **C06**) |

> **Regras fora do escopo atual** (o fluxo não coleta cooler/gabinete nem o perfil de uso): `C08` (gabinete), `C11` (cooler) e `C13` (cooler × RAM) ficam `NAO APLICAVEL`; `L03` (cooler ↔ CPU) e `L08` (capacidade por finalidade) não são avaliadas.

---

## 14. Regras de limitação (gargalos)

Diferente das regras da seção 13 (que dizem "não encaixa"), estas dizem **"encaixa, mas um componente vai segurar o outro"**. Todas as fórmulas usam apenas os campos cadastrados.

| ID | O que limita o que | Campos utilizados | Cálculo / critério |
| --- | --- | --- | --- |
| **L01** | CPU ↔ GPU (gargalo principal) | `cpu.indiceDesempenhoSingleThread`, `cpu.indiceDesempenhoMultithread`, `gpu.indiceDesempenhoGPU`, `gpu.resolucaoAlvo` | `desequilibrio = (indiceCPU − indiceGPU) / indiceGPU`. Se `> +15%` → **GPU é o limite** (cenário saudável: a CPU sobra). Se `< −15%` → **gargalo de CPU**: a GPU não é usada por completo. Sem perfil de uso, usa-se o índice de *single thread* da CPU e a tolerância vem de `gpu.resolucaoAlvo` (em 4K a GPU pesa mais). |
| **L02** | VRM da placa-mãe ↔ CPU | `cpu.consumoMaximoWatts`, `cpu.tensaoMaximaVolts`, `placaMae.vrmFasesCPU`, `vrmCorrentePorFaseA`, `vrmPossuiDissipador` | `correnteCPU = consumoMaximoWatts / tensaoMaximaVolts` (140 W / 1.25 V = **112 A**); `capacidadeVRM = vrmFasesCPU × vrmCorrentePorFaseA` (12 × 60 = **720 A**); `usoVRM = correnteCPU / capacidadeVRM`. `> 80%` → ⚠️ risco de *thermal throttling*; sem dissipador, o limite cai para 60%. |
| **L03** | Cooler ↔ CPU — **não avaliada no escopo atual** (exige cooler) | `cooler.tdpSuportadoWatts`, `cpu.consumoMaximoWatts`, `cpu.tdpWatts` | `folgaTermica = (tdpSuportadoWatts − consumoMaximoWatts) / consumoMaximoWatts`. `< 0` → ❌; `0–20%` → ⚠️ troca térmica no limite. Documentada para a fase futura com cooler. |
| **L04** | Memória ↔ CPU (IMC) ↔ placa-mãe | `ram.frequenciaMTs`, `ram.latenciaCAS`, `cpu.frequenciaMaximaSincronaMTs`, `cpu.frequenciaMemoriaNativaMTs`, `cpu.canaisMemoria`, `placaMae.frequenciaMemoriaMaximaMTs`, `ram.quantidadePentes` | `frequenciaEfetiva = min(ram.frequenciaMTs, placaMae.frequenciaMemoriaMaximaMTs, cpu.frequenciaMaximaSincronaMTs)`. Se `frequenciaEfetiva < ram.frequenciaMTs` → ⚠️ a RAM não roda no valor anunciado. `latenciaNs = latenciaCAS / frequenciaEfetiva × 2000` (CL30 @ 6000 MT/s = **10 ns**). Se `pentes < canaisMemoria` → ⚠️ perde-se cerca de metade da banda (single channel). |
| **L05** | Barramento PCIe / M.2 ↔ GPU e SSD | `gpu.versaoPCIe`, `gpu.pistasPCIe`, `placaMae.versaoSlotPCIe16`, `pistasSlotPCIe16`; `armazenamento.versaoPCIe`, `pistasPCIe`, `leituraSequencialMBs`; `placaMae.geracaoMaximaSlotM2`, `geracaoMinimaSlotM2` | `versaoEfetiva = min(versaoGPU, versaoSlot)`; banda por pista (GB/s): 3.0 ≈ 0,985 · 4.0 ≈ 1,969 · 5.0 ≈ 3,938. `bandaSlot = pistas × bandaPorPista`. Para o SSD: `tetoSlotM2 = bandaPorPista(geracao) × 1000` MB/s (PCIe 4.0 x4 ≈ 7.800 MB/s), comparado com `leituraSequencialMBs`. Geração de slot menor → ⚠️ % de desempenho perdido. |
| **L06** | Fonte ↔ conjunto | `fonte.potenciaTotalWatts`, `potenciaTrilho12VWatts`, `cpu.consumoMaximoWatts`, `gpu.tdpWatts`, `gpu.picoTransienteWatts`, `consumoEstimadoWatts` de placa-mãe/RAM/SSD/cooler/fans | `consumoTotalSistemaWatts = cpu.consumoMaximoWatts + gpu.tdpWatts + Σ(demais consumos)` (≈ **410 W** no exemplo); `consumoComMargemWatts = consumoTotalSistemaWatts × 1,40` (≈ **574 W**). Se `consumoComMargemWatts > potenciaTotalWatts` → ❌; se `gpu.picoTransienteWatts > potenciaTrilho12VWatts` → ⚠️ risco de desligamento. |
| **L07** | VRAM ↔ resolução-alvo da GPU | `gpu.memoriaVRAMGB`, `gpu.resolucaoAlvo` | Compara com `VRAM_MINIMA_GB` da resolução: 1080p = 8 GB, 1440p = 12 GB, 4K = 16 GB. Abaixo do mínimo → ⚠️ *stutter* e texturas reduzidas. |
| **L08** | Capacidade de RAM e de disco ↔ uso — **não avaliada no escopo atual** (depende de perfil de uso) | `ram.capacidadePorPenteGB`, `ram.quantidadePentes`, `armazenamento.capacidadeGB`, `perfilUso.capacidadeTotalDesejadaGB`, `perfilUso.finalidade` | `ramTotal = capacidadePorPenteGB × quantidadePentes` vs `RAM_MINIMA_GB` da finalidade (jogos 16, edição/IA 32, modelagem 64). Documentada para a fase futura com perfil. |
| **L09** | Eficiência da fonte | `fonte.potenciaTotalWatts`, `certificacao80Plus`, `eficienciaPercentualCarga50`, `consumoTotalSistemaWatts` (de L06) | `percentualCargaFonte = consumoTotalSistemaWatts / potenciaTotalWatts` (410/750 ≈ **54%**). Fora da faixa 40–60% → ⚠️ operação longe do ponto de maior eficiência (mais calor e consumo). |
| **L10** | Armazenamento ↔ slot | `armazenamento.interface`, `versaoPCIe`, `velocidadeSATAGbps`, `placaMae.geracaoMinimaSlotM2`, `placaMae.velocidadeSATAGbps` | NVMe instalado em slot de geração inferior, ou SATA em porta de 3 Gb/s → ⚠️ velocidade cortada; quantificar com a banda efetiva de **L05**. |
| **L11** | Pistas PCIe da CPU ↔ GPU + NVMe | `cpu.pistasPCIeLivres`, `gpu.pistasPCIe`, `armazenamento.pistasPCIe` | `pistasNecessarias = gpu.pistasPCIe + Σ(armazenamento.pistasPCIe)`. Se exceder `cpu.pistasPCIeLivres` → os slots restantes passam a dividir pistas do chipset (⚠️ o segundo M.2 pode desativar uma porta SATA ou reduzir o slot x16 para x8). |

> **Ordem de avaliação sugerida:** primeiro as regras **C** (sem compatibilidade não faz sentido falar de desempenho), depois **L02 → L04 → L05 → L06 → L07 → L09 → L10 → L11** e, por último, **L01** (que depende do conjunto já validado). `L03` e `L08` só entram em fases futuras, com cooler e perfil de uso.

---

## 15. Parâmetros de cálculo (constantes do sistema)

Não são dados de componente: ficam em uma classe de configuração, para poderem ser ajustados sem alterar as regras.

| Parâmetro | Valor sugerido | Justificativa / uso |
| --- | --- | --- |
| `TENSAO_REFERENCIA_CPU` | `1.25 V` | Usada em **L02** quando `cpu.tensaoMaximaVolts` é `null`. |
| `MARGEM_SEGURANCA_FONTE` | `1.40` | 40% de folga sobre o consumo total (**L06**), citada em [`exemplo-de-componentes.md`](./exemplo-de-componentes.md). |
| `LIMITE_ALERTA_USO_VRM` | `80%` | Em **L02**, acima disso a placa-mãe estrangula a CPU por temperatura. |
| `LIMITE_ALERTA_USO_VRM_SEM_DISSIPADOR` | `60%` | Mesmo cálculo quando `vrmPossuiDissipador = false`. |
| `FOLGA_MINIMA_COOLER` | `20%` | Em **L03**, abaixo disso o cooler trabalha no limite. |
| `TOLERANCIA_DESEQUILIBRIO_CPU_GPU` | `15%` | Em **L01**, diferença aceitável entre os índices de desempenho. |
| `FAIXA_EFICIENCIA_FONTE_MIN` / `MAX` | `40%` / `60%` | Faixa de carga em que a fonte atinge a melhor eficiência (**L09**). |
| `CONSUMO_BASE_SISTEMA_WATTS` | `50 W` | Reserva para placa-mãe + RAM + SSD + fans quando esses componentes não estão cadastrados (**L06**). |
| `BANDA_PCIE_GBS_POR_PISTA` | 3.0 → `0,985` · 4.0 → `1,969` · 5.0 → `3,938` | Conversão usada em **L05**. |
| `VRAM_MINIMA_GB` | 1080p → `8` · 1440p → `12` · 4K → `16` | Base da regra **L07**. |
| `RAM_MINIMA_GB` | `ESCRITORIO` 8 · `JOGOS` 16 · `EDICAO_VIDEO`/`MACHINE_LEARNING` 32 · `MODELAGEM_3D` 64 | Base da regra **L08** — **fora do escopo atual** (sem perfil de uso) |

---

## 16. Campos derivados (calculados, **não** armazenados)

Estes valores aparecem no relatório final, mas nascem de contas sobre os campos cadastrados. Se forem gravados no banco, qualquer correção de cadastro deixa o dado inconsistente.

| Valor derivado | Fórmula | Regra de origem |
| --- | --- | --- |
| `capacidadeTotalMemoriaGB` | `capacidadePorPenteGB × quantidadePentes` | C03, L08 |
| `memoriaEmDualChannel` | `quantidadePentes ≥ canaisMemoria` | C03, L04 |
| `correnteExigidaCPUAmperes` | `consumoMaximoWatts / tensaoMaximaVolts` | L02 |
| `capacidadeVRMAmperes` | `vrmFasesCPU × vrmCorrentePorFaseA` | L02 |
| `percentualUsoVRM` | `correnteExigidaCPUAmperes / capacidadeVRMAmperes` | L02 |
| `folgaTermicaCooler` | `(tdpSuportadoWatts − consumoMaximoWatts) / consumoMaximoWatts` | L03 |
| `frequenciaEfetivaMemoriaMTs` | `min(ram, placaMãe, cpu.frequenciaMaximaSincronaMTs)` | L04 |
| `latenciaEfetivaNs` | `latenciaCAS / frequenciaEfetivaMTs × 2000` | L04 |
| `bandaEfetivaPCIeGBs` | `pistasEfetivas × bandaPorPista(versaoEfetiva)` | L05 |
| `perdaBandaPCIePercentual` | `1 − (bandaEfetiva / bandaNominalGPU)` | L05 |
| `consumoTotalSistemaWatts` | `Σ` consumos de todos os componentes selecionados | L06 |
| `consumoComMargemWatts` | `consumoTotalSistemaWatts × MARGEM_SEGURANCA_FONTE` | L06, C14 |
| `requisitosFonte` | W mínimos (`consumoComMargemWatts`) + lista de conectores exigidos: `gpu.quantidadeConectores16pin/8pin/6pin` e `placaMae.quantidadeConectoresCPU8pin` | C10, C14 |
| `percentualCargaFonte` | `consumoTotalSistemaWatts / potenciaTotalWatts` | L09 |
| `capacidadeTotalArmazenamentoGB` | `Σ` `capacidadeGB` | L08 (**fora do escopo atual**) |
| `desequilibrioCpuGpu` | `(indiceCPU − indiceGPU) / indiceGPU` | L01 |

---

## 17. Exemplo de registro completo (JSON)

Processador e memória de referência, demonstrando a atomicidade exigida:

```json
{
  "id": "cpu-amd-ryzen-7-9700x",
  "modelo": "Ryzen 7 9700X",
  "fabricante": "AMD",
  "categoria": "CPU",
  "consumoEstimadoWatts": null,
  "soquete": "AM5",
  "nucleosFisicos": 8,
  "threads": 16,
  "frequenciaBaseMHz": 3800,
  "frequenciaBoostMHz": 5500,
  "tdpWatts": 65,
  "consumoMaximoWatts": 140,
  "tensaoMaximaVolts": 1.25,
  "tiposMemoriaSuportados": ["DDR5"],
  "canaisMemoria": 2,
  "frequenciaMemoriaNativaMTs": 5600,
  "frequenciaMaximaSincronaMTs": 6000,
  "capacidadeMaximaMemoriaGB": 192,
  "suportaECC": true,
  "versaoPCIeMaxima": 5,
  "pistasPCIeLivres": 24,
  "possuiVideoIntegrado": true,
  "desbloqueadoOverclock": true,
  "indiceDesempenhoSingleThread": 92,
  "indiceDesempenhoMultithread": 88
}
```

```json
{
  "id": "ram-corsair-vengeance-rgb-2x16-ddr5-6000",
  "modelo": "Vengeance RGB",
  "fabricante": "Corsair",
  "categoria": "MEMORIA",
  "consumoEstimadoWatts": 6,
  "tipo": "DDR5",
  "capacidadePorPenteGB": 16,
  "quantidadePentes": 2,
  "frequenciaMTs": 6000,
  "frequenciaJEDECBaseMTs": 4800,
  "latenciaCAS": 30,
  "tensaoVolts": 1.35,
  "perfilEXPO": true,
  "perfilXMP": true,
  "possuiECC": false,
  "alturaMm": 44
}
```

---

## 18. Checklist de validação do cadastro

Antes de um componente entrar na base (ou antes de rodar as regras), o programa deve recusar registros que falhem nestas verificações:

| # | Verificação | Exemplo de erro |
| --- | --- | --- |
| 1 | Todo campo obrigatório preenchido | CPU sem `soquete` |
| 2 | Nenhum campo com número + texto | `potenciaTotalWatts = "750 Watts"` |
| 3 | Nenhum campo com unidade duplicada (`"750 W"` em campo de W) | `"65W"` em `tdpWatts` |
| 4 | Enums dentro do domínio da seção 12 | `soquete = "AM5 "` (com espaço) |
| 5 | Valores positivos onde só positivo faz sentido | `quantidadePentes = 0` |
| 6 | `null` usado só onde o campo não se aplica (nunca para "esqueci de preencher" um obrigatório) | SSD NVMe com `versaoPCIe = null` |
| 7 | Coerência interna mínima | `frequenciaBaseMHz > frequenciaBoostMHz`; `geracaoMinimaSlotM2 > geracaoMaximaSlotM2`; `tdpWatts > consumoMaximoWatts` |
| 8 | Identificador único | dois registros com o mesmo `id` |
| 9 | Nenhum campo derivado (seção 16) gravado | `consumoTotalWatts` presente no cadastro |
| 10 | Nenhum campo "genérico" usado como curinga | `outrosDados`, `especificacoes: "..."` |

---

## 19. Resumo: quanto cada componente precisa ter

| Componente | Campos obrigatórios | No escopo? | Papel principal na análise |
| --- | --- | --- | --- |
| CPU | 18 | ✅ | Define soquete, consumo, IMC de memória, pistas PCIe e o índice de desempenho do gargalo |
| Placa-Mãe | 26 | ✅ | Define o que encaixa (soquete, slots, VRM, conectores) e os tetos de frequência/banda |
| RAM | 8 | ✅ | Tipo, capacidade, frequência e latência — compatibilidade e gargalo de memória |
| GPU | 12 | ✅ | Interface PCIe, consumo, **conectores exigidos** e índice de desempenho |
| Armazenamento | 7 | ✅ | Interface, formato e velocidades — compatibilidade de slot e gargalo de banda |
| Fonte | 12 | ✅ | Potência, trilho 12 V e **conectores disponíveis** — sustenta o conjunto |
| Cooler | 3 (+2 condicionais) | ❌ | Dissipação declarada e dimensões — limitação térmica (fase futura) |
| Gabinete | 8 | ❌ | Limites físicos de tudo o que é grande (fase futura) |
| Monitor | 3 | ❌ | Resolução e taxa — contexto do gargalo (fase futura) |
| Perfil de Uso | 3 | ❌ | Finalidade e resolução-alvo (fase futura) |

> **Princípio final:** se uma informação não é comparada por nenhuma regra **C** ou **L**, ela é apenas informativa — e informativo **não** deve ocupar campo obrigatório, senão o cadastro fica impossível de preencher.
