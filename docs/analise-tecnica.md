### 1. Placa-Mãe e Processador

**Componentes em análise:** ASUS TUF Gaming B650-Plus Wi-Fi ↔ AMD Ryzen 7 9700X

* **🔍 O que olhar:** O soquete (físico), a versão da BIOS (software) e a capacidade elétrica do VRM da placa-mãe frente ao Consumo Máximo (PPT/Turbo) do processador.
* **⚠️ O que limita o que:** Se o VRM da placa-mãe for fraco, ela limitará o processador reduzindo as frequências térmicas (*Thermal Throttling*) para proteger os componentes elétricos de derretimento.
* **🧮 O Cálculo (Capacidade do VRM vs Exigência da CPU):**
* **Exigência do Processador:** O Ryzen 7 9700X consome até 140W no modo Turbo. Sob carga pesada operando a uma tensão de aproximadamente 1.25V, a fórmula é `Corrente = Potência / Tensão`. 140W / 1.25V = **112 Amperes**.
* **Fornecimento da Placa-Mãe:** A ASUS B650 possui 12 fases de 60A dedicadas à CPU. 12 × 60A = **720 Amperes teóricos**.
* **🚀 Extração de 100%:** O processador usará apenas **~15%** da capacidade máxima do VRM (112A de 720A). A placa-mãe trabalhará completamente fria, liberando os 5.5 GHz da CPU sem nenhum estrangulamento.



---

### 2. Placa-Mãe e Memória RAM

**Componentes em análise:** ASUS TUF Gaming B650-Plus Wi-Fi ↔ Corsair Vengeance 32GB (6000 MHz)

* **🔍 O que olhar:** A tecnologia da RAM (DDR5) e a topologia elétrica da placa-mãe para suportar altas frequências sem ruído no sinal elétrico.
* **⚠️ O que limita o que:** A construção física das trilhas da placa-mãe dita o teto elétrico, limitando a frequência máxima (MHz) que os pentes de memória conseguem atingir de forma estável.
* **🧮 O Cálculo (Teto Elétrico da Placa):**
* **Limite da Placa-Mãe:** A fabricante garante suporte para até **7600+ MHz** em Overclock/EXPO.
* **Exigência da RAM:** Os pentes Corsair rodam nativamente a **6000 MHz**.
* **🚀 Extração de 100%:** Como o limite elétrico da placa (7600) é imensamente superior ao alvo da memória (6000), os pentes atingirão a velocidade total sem nenhuma resistência física ou elétrica da placa.



---

### 3. Processador e Memória RAM

**Componentes em análise:** AMD Ryzen 7 9700X ↔ Corsair Vengeance 32GB (6000 MHz / CL30)

* **🔍 O que olhar:** O suporte nativo do processador frente à frequência e à latência da memória.
* **⚠️ O que limita o que:** O Controlador Integrado de Memória (IMC), localizado fisicamente *dentro* do chip do processador, é o verdadeiro gargalo para altas velocidades de RAM.
* **🧮 O Cálculo (A Regra do *Infinity Fabric* / FCLK):**
* Para extrair potência máxima em chips AM5 sem penalidades de latência, a comunicação entre o controlador da CPU e a memória RAM deve manter uma proporção cravada de **1:1**.
* O limite máximo estável para manter essa proporção 1:1 (*sweet spot*) nas arquiteturas Ryzen atuais é exatamente **6000 MT/s**.
* **🚀 Extração de 100%:** O kit Corsair possui 6000 MHz e baixa latência (CL30). O controlador do Ryzen não precisará reduzir a própria velocidade para lidar com a RAM, garantindo a latência mais baixa e o maior fluxo de dados possível.



---

### 4. Placa-Mãe e Placa de Vídeo (GPU)

**Componentes em análise:** ASUS TUF Gaming B650-Plus Wi-Fi ↔ NVIDIA GeForce RTX 4070 Super

* **🔍 O que olhar:** A Geração do barramento PCIe e o número de pistas (*Lanes*) físicas e elétricas do slot principal cruzados com a interface da placa de vídeo.
* **⚠️ O que limita o que:** Se o slot da placa-mãe for de uma geração anterior ou tiver poucas trilhas, ele estrangulará a largura de banda dos dados visuais gerados pela GPU.
* **🧮 O Cálculo (Largura de Banda PCIe):**
* **Exigência da GPU:** A RTX 4070 Super é arquitetada para comunicação **PCIe 4.0 x16**.
* **Fornecimento da Placa-Mãe:** O slot principal entrega perfeitamente **PCIe 4.0 x16**, o que garante um limite de transferência de aproximadamente **31.5 GB/s**.
* **🚀 Extração de 100%:** O teto da placa-mãe empata com o projeto máximo da placa de vídeo. Zero estrangulamento de dados.



---

### 5. Processador e Placa de Vídeo (GPU)

**Componentes em análise:** AMD Ryzen 7 9700X ↔ NVIDIA GeForce RTX 4070 Super

* **🔍 O que olhar:** O IPC (Instruções por Clock), a frequência e os núcleos do processador frente à potência de renderização da GPU e a resolução do monitor (ex: 1440p).
* **⚠️ O que limita o que:** O processador limita a GPU (Gargalo de CPU) se não conseguir calcular a lógica do jogo rápido o bastante para alimentar a renderização. A GPU limita a CPU (cenário perfeito) quando ela atinge 100% de uso gráfico enquanto a CPU "sobra" livre.
* **🧮 O Cálculo (Equilíbrio de Carga Gráfica):**
* A métrica matemática aqui se baseia na telemetria: A RTX 4070 Super é projetada primariamente para a resolução **1440p**.
* **🚀 Extração de 100%:** Em 1440p, o Ryzen 7 9700X (processador topo de linha com IPC altíssimo) sobrará na casa dos 30% a 50% de uso, enquanto envia dados suficientes para travar a RTX 4070 Super em **95% a 99% de uso contínuo**. Todo o investimento na placa de vídeo é transformado em quadros por segundo reais.



---

### 6. Placa-Mãe e Armazenamento (SSD)

**Componentes em análise:** ASUS TUF Gaming B650-Plus Wi-Fi ↔ SSD Kingston KC3000 (PCIe 4.0)

* **🔍 O que olhar:** A Geração PCIe do slot M.2 da placa frente às velocidades nominais de leitura e escrita do SSD.
* **⚠️ O que limita o que:** O slot M.2 corta a velocidade do SSD imediatamente pela metade caso possua uma geração de barramento inferior.
* **🧮 O Cálculo (Teto de Leitura M.2):**
* **Desempenho do SSD:** O KC3000 atinge até **7.000 MB/s** em sua interface PCIe 4.0 x4.
* **Limite da Placa-Mãe:** Os slots M.2 suportam PCIe 5.0 (teto de 14.000 MB/s) e PCIe 4.0 (teto de 7.800 MB/s).
* **🚀 Extração de 100%:** Mesmo instalado no segundo slot (PCIe 4.0), o limite de banda da placa (7.800 MB/s) supera o fluxo máximo do SSD (7.000 MB/s). O SSD funcionará sem nenhuma barreira de tráfego.



---

### 7. Fonte de Alimentação (PSU) e o Conjunto Completo

**Componentes em análise:** MSI MAG A750GL (750W) ↔ (Ryzen 7 9700X + RTX 4070 Super)

* **🔍 O que olhar:** A somatória do consumo máximo em Watts (TDP/Picos) e a curva de eficiência elétrica do selo *80 Plus*.
* **⚠️ O que limita o que:** Picos de energia (*Transient Spikes*) que ultrapassem o limite de entrega de watts da fonte fazem a máquina desligar instantaneamente.
* **🧮 O Cálculo (A Curva de Eficiência *80 Plus*):**
* **Consumo da Máquina:** Ryzen (140W) + GPU (220W) + Placa-mãe/RAM/SSD/Fãs/Cooler (~50W). Consumo total máximo projetado = **~410 Watts**.
* **Fornecimento da Fonte:** A fonte dispõe de **750 Watts** e suporte nativo PCIe 5.0 (sem adaptadores que causam perda de energia).
* **🚀 Extração de 100%:** 410W representam aproximadamente **54%** da capacidade total de 750W. Fontes com selo *80 Plus Gold* alcançam sua eficiência máxima, o pico de economia de energia na conta de luz e a menor geração de calor exatamente ao operarem na faixa dos **50% de carga**. Dimensionamento perfeito.