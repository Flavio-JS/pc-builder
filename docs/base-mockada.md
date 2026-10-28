# Base de componentes (gerada do OpenDB)

A base do programa é um **arquivo JSON** com os componentes que o fluxo guiado pode
oferecer:

```
src/main/resources/base/componentes.json
```

| Item | Valor |
| --- | --- |
| Componentes | **17.230** |
| Tamanho | ~8,7 MB (uma linha por componente) |
| Fonte | `buildcores-open-db/open-db` (pastas `CPU`, `Motherboard`, `RAM`, `GPU`, `Storage`, `PSU`) — **removida** do repositório após a geração (ver seção 8) |
| Gerado por | `tools/java/pcbuilder/ferramentas/ConversorOpenDb.java` |
| Regenerar | `scripts/gerar-base.sh` (Linux/macOS/Git Bash) ou `pc-builder\scripts\gerar-base.bat` |

> **Mudança de estratégia:** a primeira versão da HU-001 usava 21 peças escritas à
> mão. Como o catálogo OpenDB já está no repositório e a HU-001 pede no mínimo 3
> itens por categoria, a base passou a ser **o catálogo completo convertido** para o
> formato do modelo de dados. Isso dá milhares de opções reais para o usuário e
> material de sobra para os filtros (HU-003) e avisos (HU-004) das próximas etapas.

## 1. Como a base é gerada

```
open-db/CPU|Motherboard|RAM|GPU|Storage|PSU  (.json por componente)
        │
        │  1. leitura com pcbuilder.json.Json (parser próprio, só JDK)
        ▼
MapeadorCpu / MapeadorPlacaMae / MapeadorMemoria /
MapeadorPlacaVideo / MapeadorArmazenamento / MapeadorFonte
        │  2. valores atômicos + heurísticas para o que o catálogo não tem
        ▼
ValidadorCadastro.validar(...)     3. as 10 verificações do checklist
        │                            (registro com ERRO é descartado)
        ▼
src/main/resources/base/componentes.json
        │  4. copiado para o classpath na compilação (scripts/compilar.*)
        ▼
LeitorBaseMock → CatalogoMock → validação na carga → `Base carregada: ...`
```

O conversor monta **objetos do próprio domínio** (`Processador`, `PlacaMae`…) e
serializa `Componente.campos()`, então o JSON usa exatamente os nomes de campo do
[`modelo-de-dados.md`](modelo-de-dados.md). Campos `null` são omitidos do arquivo.

## 2. Formato do arquivo

```json
{
  "versao": 1,
  "geradoEm": "2026-10-28",
  "origem": "../buildcores-open-db/open-db",
  "total": 17230,
  "porCategoria": { "ARMAZENAMENTO": 3322, "CPU": 265, "FONTE": 3303,
                    "MEMORIA": 4506, "PLACA_MAE": 2003, "PLACA_VIDEO": 3831 },
  "componentes": [
    { "id": "cpu-core-i9-13900t-01c380477ff9", "modelo": "Core i9 13900T",
      "fabricante": "Intel", "categoria": "CPU", "precoMedioBRL": 2529.90,
      "garantiaMeses": 36, "anoLancamento": 2023, "soquete": "LGA1700",
      "nucleosFisicos": 24, "threads": 32, "frequenciaBaseMHz": 1100, ... },
    ...
  ]
}
```

* `id` = prefixo da categoria + nome do modelo em *slug* + 12 caracteres do
  `opendb_id` (garante unicidade — verificação 8).
* `modelo` passa por uma limpeza que remove fabricante, capacidade, frequência,
  potência, cor, formato e termos de marketing (`Heuristicas.limparNome`).
* Enums são gravados pelo nome (`"LGA1700"`, `"DDR5"`, `"GOLD"`).

## 3. Contagens da última geração

| Categoria | Componentes |
| --- | --- |
| `ARMAZENAMENTO` | 3.322 |
| `CPU` | 265 |
| `FONTE` | 3.303 |
| `MEMORIA` | 4.506 |
| `PLACA_MAE` | 2.003 |
| `PLACA_VIDEO` | 3.831 |
| **Total** | **17.230** |

Todas as categorias ficam muito acima do mínimo de 3 exigido pela HU-001.

### Registros ignorados na conversão

O que não cabe no domínio do modelo de dados é descartado (e contabilizado):

| Motivo | Registros |
| --- | --- |
| `Placa-mae: soquete fora do dominio` | 1.497 |
| `CPU: soquete fora do dominio` | 525 |
| `Memoria: tipo fora do dominio` (DDR3/DDR2/DDR1) | 370 |
| `Placa-mae: chipset fora do dominio` | 161 |
| `Armazenamento: formato fora do dominio` (mSATA, PCIe, M.2-2230…) | 93 |
| `Armazenamento: interface fora do dominio` (SAS, U.2, PATA, mSATA) | 66 |
| `Placa-mae: sem slot M.2 PCIe` | 36 |
| `Placa de video: sem VRAM/TDP` | 34 |
| `Armazenamento: tipo de midia fora do dominio` (SSHD) | 24 |
| `Fonte: sem potencia` | 5 |
| `Placa-mae: sem slot PCIe x16` | 3 |
| `Placa-mae: formato fora do dominio` (SSI EEB, HPTX…) | 1 |

Total lido: 20.045 arquivos; total aprovado: 17.230.

**Soquetes aceitos:** AM4, AM5, LGA1200, LGA1700 e LGA1851 (os do modelo de dados
que aceitam DDR4/DDR5). Processadores de soquetes antigos (LGA1151, LGA2011,
FM2+…) ficam fora porque o modelo não representa DDR3 nem esses chipsets.

## 4. Regras de conversão (o que veio do catálogo e o que foi derivado)

O OpenDB **não fornece preço** e não tem vários campos que o modelo exige. Cada
valor derivado é determinístico (mesma entrada → mesma saída):

| Categoria | Do catálogo | Derivado / inventado |
| --- | --- | --- |
| Todas | `metadata.name`, `metadata.manufacturer`, `opendb_id` | `precoMedioBRL` (fórmula por categoria), `garantiaMeses`, `anoLancamento` quando ausente |
| CPU | `socket`, `cores`, `clocks`, `specifications.tdp/ppt`, `memory.*`, `eccSupport`, `integratedGraphics` | `tensaoMaximaVolts`, `frequenciaMemoriaNativaMTs`, `frequenciaMaximaSincronaMTs`, `versaoPCIeMaxima`, `pistasPCIeLivres` (por soquete); `consumoMaximoWatts = max(ppt, 1,25 × TDP)`; índices de desempenho normalizados 1–100 a partir de `boost` (single) e `boost × núcleos` (multi) |
| Placa-mãe | `socket`, `chipset`, `form_factor`, `memory.*`, `m2_slots`, `pcie_slots`, `storage_devices`, `ecc_support`, `bios_features.flashback` | VRM (fases/corrente/dissipador) e preço-base por chipset; dimensões e consumo por formato; `frequenciaMemoriaMaximaMTs` (DDR4 3200; DDR5 6400 ou 7600 nos chipsets top); perfis EXPO (AMD) / XMP; portas SATA = 4 quando o catálogo vem zerado |
| Memória | `ram_type`, `modules`, `speed`, `cas_latency`, `profile_support`, `voltage`, `ecc`, `heat_spreader` | `frequenciaJEDECBaseMTs` (4800 DDR5 / 2133 DDR4); `tensaoVolts` 1,35 V quando ausente; `alturaMm` 44 mm com dissipador, 34 mm sem; `consumoEstimadoWatts = 3 × pentes` |
| Placa de vídeo | `chipset_manufacturer`, `memory`, `memory_type`, `tdp`, `length`, `total_slot_width`, `interface` | `picoTransienteWatts = 1,6 × TDP`; `fonteRecomendadaWatts`; conectores de energia quando o catálogo traz 0 (≥250 W → 1×16 pinos, ≥150 W → 2×8 pinos, ≥75 W → 1×8 pinos); `resolucaoAlvo` por VRAM (≥16 GB 4K, ≥12 GB 1440p, resto 1080p); índice normalizado por `núcleos × boost`; `possuiRayTracing` por nome/chipset |
| Armazenamento | `capacity`, `storage_type`, `form_factor`, `interface` | velocidades, consumo e TBW típicos da classe (NVMe 5.0 ≈ 12.000 MB/s, 4.0 ≈ 7.000, 3.0 ≈ 3.400; SSD SATA ≈ 550; HD ≈ 190) |
| Fonte | `wattage`, `efficiency_rating`, `modular`, `connectors.*`, `length` | `potenciaTrilho12VWatts = wattage`; eficiência por selo (80 % / 82 / 85 / 87 / 90 / 92 / 94); `padraoATX` ATX 3.0 quando tem 12VHPWR; conectores PCIe/EPS/SATA derivados da potência quando o catálogo vem zerado |

### Extensão do domínio `Chipset`

O catálogo usa chipsets que a seção 12 do modelo de dados não listava (B550, B450,
X570, Z690, B860…). Sem eles, os 265 processadores AM4/LGA1200/LGA1700 ficariam sem
placa-mãe compatível. O enum foi ampliado para 27 chipsets — a lista está em
[`modelo-de-dados.md`](modelo-de-dados.md#12-enums-necessários-domínios-fechados).

### Quando o catálogo diverge dos exemplos dos documentos

Prevalece o catálogo. Por exemplo, [`exemplo-de-componentes.md`](exemplo-de-componentes.md)
cita o Ryzen 7 9700X com 140 W de PPT e 192 GB de memória; o OpenDB informa PPT de
88 W e 256 GB, então a base traz `consumoMaximoWatts: 88` e
`capacidadeMaximaMemoriaGB: 256`. Os exemplos dos documentos continuam válidos como
ilustração das regras (as fórmulas são as mesmas), apenas não são os valores da base.

## 5. Pares incompatíveis disponíveis

A base tem, de propósito, combinações que **não** encaixam (o filtro da HU-003 vai
escondê-las e o `[M] ver ocultos` vai explicar o motivo):

* processadores AM5 × placas LGA1700/LGA1851 (e o contrário) — `C01`;
* placas DDR5 × kits DDR4 (e placas DDR4 × kits DDR5) — `C02`;
* placas com 2 slots de memória × kits de 2 pentes acima de 64 GB — `C03`;
* placas sem slot M.2 × SSDs NVMe, e placas sem porta SATA × HDs — `C04`;
* GPUs que exigem 1×16 pinos × fontes sem 12VHPWR — `C10` (sem caminho de adaptador);
* GPUs de 250–600 W × fontes de 400 W — `C14`.

O teste `ValidadorCadastroTeste.paresIncompativeis()` garante que esses pares
continuam existindo na base.

## 6. Por que a carga termina com 0 avisos

Todos os campos condicionais foram preenchidos (tensão da CPU, I/O do SSD, pico
transiente da GPU), então o `ValidadorCadastro` não precisa emitir `AVISO`. A
mensagem da carga fica:

```
Base carregada: 17230 componentes, 0 erro(s), 0 aviso(s).
```

Os caminhos de `ERRO` e `AVISO` continuam cobertos pelos testes (verificações 1 a
10 do checklist), que montam registros inválidos com `ComponenteFake`.

## 7. Regenerar a base

**O catálogo não é necessário para rodar o programa.** O `componentes.json` é
versionado no repositório e copiado para o classpath na compilação; quem clona o
projeto compila, roda e testa sem a pasta `buildcores-open-db`. A pasta só é
necessária para **regerar** a base (e, sem ela, o conversor encerra com a mensagem
`Pasta do catalogo nao encontrada`).

```bash
cd pc-builder
./scripts/gerar-base.sh          # Linux/macOS/Git Bash
scripts\gerar-base.bat           # Windows
```

O script compila tudo, roda o conversor (`--entrada ../buildcores-open-db/open-db`,
`--saida src/main/resources/base/componentes.json`), recompila para copiar o
arquivo para o classpath e imprime o resumo de incluídos/ignorados. Se algum
registro for descartado pela validação, o conversor sai com código `2` e mostra os
primeiros problemas — sinal de que uma heurística precisa de ajuste.

## 8. Origem, licença e o que acontece se o catálogo for apagado

* **Repositório de origem:** <https://github.com/buildcores/buildcores-open-db>
  (clone: `git clone https://github.com/buildcores/buildcores-open-db.git`).
* **Licença dos dados:** Creative Commons **CC BY-NC-SA 4.0**. Cópia da licença e do
  aviso em [`licensing/`](licensing/) (`LICENSE.txt` e `NOTICE.md`), mantida aqui
  justamente para a atribuição não se perder caso o catálogo seja removido.
* Texto de atribuição deste trabalho:

  > Data from BuildCores OpenDB (https://github.com/buildcores/buildcores-open-db),
  > licensed under CC BY-NC-SA 4.0. Changes: campos derivados (preço, VRM, tensões,
  > índices de desempenho, velocidades, conectores) e recorte para as 6 categorias
  > do fluxo guiado, descritos neste documento.

### O catálogo `buildcores-open-db/` foi removido do repositório

Depois de gerar o `componentes.json` (17.230 registros), a pasta do catálogo
(~298 MB / 48.788 arquivos) foi **apagada**: nada do que roda depende dela —
`componentes.json` já tem os dados convertidos. Só é preciso baixá-la de novo para
**regenerar** a base:

```bash
# dentro de Trabalho/
git clone https://github.com/buildcores/buildcores-open-db.git
cd pc-builder && ./scripts/gerar-base.sh
```

Sem o catálogo, `gerar-base` encerra com código `1` e a mensagem
`Pasta do catalogo nao encontrada`; o `componentes.json` existente não é alterado.


