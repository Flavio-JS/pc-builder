package pcbuilder.teste;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import pcbuilder.analise.CalculadoraConsumo;
import pcbuilder.analise.filtro.FiltroConectoresFonte;
import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.filtro.FiltroFonteSuficiente;
import pcbuilder.analise.filtro.FiltroMemoriaECC;
import pcbuilder.analise.filtro.FiltroSlotsMemoria;
import pcbuilder.analise.filtro.FiltroSoquete;
import pcbuilder.analise.filtro.FiltroTipoMemoria;
import pcbuilder.analise.filtro.FiltroVideoIntegrado;
import pcbuilder.analise.filtro.ItemOculto;
import pcbuilder.analise.filtro.ListaFiltrada;
import pcbuilder.cli.TabelaComponentes;
import pcbuilder.cli.TelaEtapa;
import pcbuilder.cli.Terminal;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.InterfaceArmazenamento;
import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;
import pcbuilder.mock.CatalogoMock;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.PlacaVideo;
import pcbuilder.modelo.Processador;

/**
 * Testes da HU-003: filtro de compatibilidade dura (C01 a C14).
 *
 * <p>Cobrem o encaixe por soquete, tipo/capacidade de memoria, slots de
 * armazenamento e de PCIe, conectores e potencia da fonte, a regra de ECC, a
 * mensagem de "nenhum compativel" e a renderizacao da lista filtrada na CLI.</p>
 *
 * <p>Execucao (sem JUnit):
 * {@code java -cp build/classes;build/classes-test pcbuilder.teste.FiltroEtapaTeste}.</p>
 */
public final class FiltroEtapaTeste {

    private static final CatalogoMock CATALOGO = CatalogoMock.criar();
    private static final FiltroEtapa FILTRO = new FiltroEtapa(CATALOGO);

    private static final List<Componente> CPUS = CATALOGO.porCategoria(CategoriaComponente.CPU);
    private static final List<Componente> PLACAS_MAE =
            CATALOGO.porCategoria(CategoriaComponente.PLACA_MAE);
    private static final List<Componente> MEMORIAS =
            CATALOGO.porCategoria(CategoriaComponente.MEMORIA);
    private static final List<Componente> PLACAS_VIDEO =
            CATALOGO.porCategoria(CategoriaComponente.PLACA_VIDEO);
    private static final List<Componente> ARMAZENAMENTOS =
            CATALOGO.porCategoria(CategoriaComponente.ARMAZENAMENTO);
    private static final List<Componente> FONTES =
            CATALOGO.porCategoria(CategoriaComponente.FONTE);

    private FiltroEtapaTeste() {
        // classe utilitaria: nao deve ser instanciada
    }

    public static void main(String[] args) {
        System.out.println("Testes da HU-003 - filtro de compatibilidade");

        cpuAm5SoListaPlacasAm5();
        placaDdr5SoListaMemoriasDdr5();
        frequenciaAltaNaoOcultaMemoria();
        slotsECapacidadeDeMemoria();
        armazenamentoSemSlot();
        gpuSemSlotX16();
        gpu16PinosOcultaFontesSemConector();
        fonteSemConectorCpu();
        fonteFracaEOcultada();
        memoriaEcc();
        videoIntegradoNaoOcultaGpu();
        nenhumCompativel();
        explicarFiltroRegistraOcultos();
        telaListaFiltrada();
        telaSemCompativeis();
        contagemEMotivoPredominante();
        calculoDoConsumo();

        System.exit(Assercao.concluir());
    }

    // ------------------------------------------------------------ C01 (soquete)

    private static void cpuAm5SoListaPlacasAm5() {
        Processador cpu = cpuAm5();
        Montagem montagem = montagem(cpu);

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.PLACA_MAE, montagem);
        boolean todasAm5 = true;
        for (Componente componente : lista.getCompativeis()) {
            if (((PlacaMae) componente).getSoqueteCPU() != Soquete.AM5) {
                todasAm5 = false;
            }
        }

        Assercao.verdadeiro("C01: so placas AM5 para CPU AM5", todasAm5);
        Assercao.verdadeiro("C01: existem placas compativeis", !lista.getCompativeis().isEmpty());
        Assercao.verdadeiro("C01: placas de outro soquete ficam ocultas",
                !lista.getOcultos().isEmpty());
        Assercao.iguais("C01: nada some da base", PLACAS_MAE.size(), lista.getTotalOriginal());
        Assercao.iguais("C01: total = compativeis + ocultos", lista.getTotalOriginal(),
                lista.getCompativeis().size() + lista.quantidadeOcultos());
        Assercao.verdadeiro("C01: motivo cita soquete da placa e da CPU",
                lista.getOcultos().stream()
                        .anyMatch(oculto -> oculto.getMotivo()
                                .equals("soquete LGA1700 x processador AM5")));

        PlacaMae placaAm5 = (PlacaMae) lista.getCompativeis().get(0);
        Assercao.verdadeiro("C01: mesma soquete nao e ocultada",
                new FiltroSoquete().motivoIncompatibilidade(montagem, placaAm5).isEmpty());
    }

    // -------------------------------------------------- C02/C03/C12 (memoria)

    private static void placaDdr5SoListaMemoriasDdr5() {
        Processador cpu = cpuAm5();
        PlacaMae placa = primeiraPlaca(cpu, TipoMemoria.DDR5);
        Montagem montagem = montagem(cpu, placa);

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.MEMORIA, montagem);
        boolean todasDdr5 = true;
        boolean todasSuportadasPelaCpu = true;
        for (Componente componente : lista.getCompativeis()) {
            Memoria memoria = (Memoria) componente;
            if (memoria.getTipo() != TipoMemoria.DDR5) {
                todasDdr5 = false;
            }
            if (!cpu.getTiposMemoriaSuportados().contains(memoria.getTipo())) {
                todasSuportadasPelaCpu = false;
            }
        }

        Assercao.verdadeiro("C02: placa DDR5 so lista memoria DDR5", todasDdr5);
        Assercao.verdadeiro("C02: so tipos suportados pela CPU", todasSuportadasPelaCpu);
        Assercao.verdadeiro("C02: memoria DDR4 oculta com motivo",
                lista.getOcultos().stream()
                        .anyMatch(oculto -> oculto.getMotivo().equals("DDR4 x placa-mae DDR5")));
        Assercao.iguais("C02: memorias nao somem da base", MEMORIAS.size(),
                lista.getTotalOriginal());
    }

    private static void frequenciaAltaNaoOcultaMemoria() {
        Processador cpu = cpuAm5();
        PlacaMae placa = PlacaMae.builder()
                .id("placa-teste-6000")
                .modelo("B650 Teto 6000")
                .fabricante("Teste")
                .soqueteCPU(Soquete.AM5)
                .tipoMemoria(TipoMemoria.DDR5)
                .quantidadeSlotsMemoria(4)
                .capacidadeMaximaMemoriaGB(192)
                .frequenciaMemoriaMaximaMTs(6000)
                .construir();
        Montagem montagem = montagem(cpu, placa);

        Memoria rapida = Memoria.builder()
                .id("ram-teste-8000")
                .modelo("Vengeance 8000")
                .fabricante("Teste")
                .tipo(TipoMemoria.DDR5)
                .capacidadePorPenteGB(16)
                .quantidadePentes(2)
                .frequenciaMTs(8000)
                .latenciaCAS(38)
                .construir();

        Assercao.verdadeiro("C02: 8000 MT/s nao e ocultada por frequencia",
                new FiltroTipoMemoria().motivoIncompatibilidade(montagem, rapida).isEmpty());
        Assercao.verdadeiro("C03: 8000 MT/s nao e ocultada por capacidade",
                new FiltroSlotsMemoria().motivoIncompatibilidade(montagem, rapida).isEmpty());
        Assercao.verdadeiro("C12: memoria comum nao e ocultada por ECC",
                new FiltroMemoriaECC().motivoIncompatibilidade(montagem, rapida).isEmpty());

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.MEMORIA, montagem);
        Assercao.verdadeiro("HU-003: kit acima do teto da placa continua na lista",
                lista.getCompativeis().stream()
                        .map(Memoria.class::cast)
                        .anyMatch(memoria -> memoria.getFrequenciaMTs() > 6000));
    }

    private static void slotsECapacidadeDeMemoria() {
        Processador cpu = cpuAm5();
        PlacaMae placa = PlacaMae.builder()
                .id("placa-teste-2-slots")
                .modelo("A620 Basica")
                .fabricante("Teste")
                .soqueteCPU(Soquete.AM5)
                .tipoMemoria(TipoMemoria.DDR5)
                .quantidadeSlotsMemoria(2)
                .capacidadeMaximaMemoriaGB(32)
                .construir();
        Montagem montagem = montagem(cpu, placa);

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.MEMORIA, montagem);
        boolean cabemNosSlots = true;
        for (Componente componente : lista.getCompativeis()) {
            Memoria memoria = (Memoria) componente;
            int total = memoria.getCapacidadePorPenteGB() * memoria.getQuantidadePentes();
            if (memoria.getQuantidadePentes() > 2 || total > 32) {
                cabemNosSlots = false;
            }
        }
        Assercao.verdadeiro("C03: so kits que cabem nos 2 slots e nos 32 GB", cabemNosSlots);
        Assercao.verdadeiro("C03: existe kit compativel", !lista.getCompativeis().isEmpty());

        Memoria kitQuatroPentes = Memoria.builder()
                .id("ram-teste-4-pentes")
                .modelo("Kit 4 pentes")
                .fabricante("Teste")
                .tipo(TipoMemoria.DDR5)
                .capacidadePorPenteGB(16)
                .quantidadePentes(4)
                .frequenciaMTs(6000)
                .latenciaCAS(30)
                .construir();
        Assercao.verdadeiro("C03: 4 pentes x placa de 2 slots",
                new FiltroSlotsMemoria().motivoIncompatibilidade(montagem, kitQuatroPentes)
                        .filter(motivo -> motivo.contains("4 pentes")).isPresent());

        Memoria kitGrande = Memoria.builder()
                .id("ram-teste-64-gb")
                .modelo("Kit 64 GB")
                .fabricante("Teste")
                .tipo(TipoMemoria.DDR5)
                .capacidadePorPenteGB(32)
                .quantidadePentes(2)
                .frequenciaMTs(6000)
                .latenciaCAS(30)
                .construir();
        Assercao.verdadeiro("C03: 64 GB x limite de 32 GB da placa",
                new FiltroSlotsMemoria().motivoIncompatibilidade(montagem, kitGrande)
                        .filter(motivo -> motivo.contains("limite de 32 GB (placa-mae)"))
                        .isPresent());
    }

    // ---------------------------------------------- C04 (slots de armazenamento)

    private static void armazenamentoSemSlot() {
        Processador cpu = cpuAm5();
        PlacaMae placa = PlacaMae.builder()
                .id("placa-teste-m2")
                .modelo("B650 Um M2")
                .fabricante("Teste")
                .soqueteCPU(Soquete.AM5)
                .tipoMemoria(TipoMemoria.DDR5)
                .quantidadeSlotsMemoria(4)
                .capacidadeMaximaMemoriaGB(192)
                .quantidadeSlotsM2(1)
                .quantidadePortasSATA(0)
                .construir();
        Montagem montagem = montagem(cpu, placa);

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.ARMAZENAMENTO, montagem);
        boolean todasNvme = true;
        for (Componente componente : lista.getCompativeis()) {
            if (((Armazenamento) componente).getInterfaceArmazenamento()
                    != InterfaceArmazenamento.NVME) {
                todasNvme = false;
            }
        }

        Assercao.verdadeiro("C04: placa com M.2 so lista NVMe", todasNvme);
        Assercao.verdadeiro("C04: existe NVMe compativel", !lista.getCompativeis().isEmpty());
        Assercao.verdadeiro("C04: SATA oculto com motivo",
                lista.getOcultos().stream()
                        .allMatch(oculto -> oculto.getMotivo().contains("porta SATA")));
        Assercao.iguais("C04: nada some da base", ARMAZENAMENTOS.size(), lista.getTotalOriginal());
    }

    // ---------------------------------------------------- C05 (slot x16 da GPU)

    private static void gpuSemSlotX16() {
        Processador cpu = cpuAm5();
        PlacaMae semSlot = PlacaMae.builder()
                .id("placa-teste-sem-x16")
                .modelo("B650 Sem X16")
                .fabricante("Teste")
                .soqueteCPU(Soquete.AM5)
                .tipoMemoria(TipoMemoria.DDR5)
                .quantidadeSlotsMemoria(4)
                .capacidadeMaximaMemoriaGB(192)
                .quantidadeSlotsPCIe16(0)
                .construir();

        ListaFiltrada semSlotLista = FILTRO.filtrar(CategoriaComponente.PLACA_VIDEO,
                montagem(cpu, semSlot));
        Assercao.verdadeiro("C05: sem slot x16 oculta todas as GPUs",
                semSlotLista.estaVazia());
        Assercao.verdadeiro("C05: motivo predominante cita o slot x16",
                semSlotLista.motivoPredominante().orElse("").contains("sem slot x16 livre"));
        Assercao.iguais("C05: todas as GPUs ficam ocultas", PLACAS_VIDEO.size(),
                semSlotLista.quantidadeOcultos());

        PlacaMae slotCurto = PlacaMae.builder()
                .id("placa-teste-x4")
                .modelo("B650 Slot X4")
                .fabricante("Teste")
                .soqueteCPU(Soquete.AM5)
                .tipoMemoria(TipoMemoria.DDR5)
                .quantidadeSlotsMemoria(4)
                .capacidadeMaximaMemoriaGB(192)
                .quantidadeSlotsPCIe16(1)
                .pistasSlotPCIe16(4)
                .construir();

        ListaFiltrada slotCurtoLista = FILTRO.filtrar(CategoriaComponente.PLACA_VIDEO,
                montagem(cpu, slotCurto));
        boolean cabeNasPistas = true;
        for (Componente componente : slotCurtoLista.getCompativeis()) {
            if (((PlacaVideo) componente).getPistasPCIe() > 4) {
                cabeNasPistas = false;
            }
        }
        Assercao.verdadeiro("C05: so GPUs que cabem nas 4 pistas", cabeNasPistas);
        Assercao.verdadeiro("C05: GPU de 16 pistas oculta em slot x4",
                slotCurtoLista.getOcultos().stream()
                        .anyMatch(oculto -> oculto.getMotivo().contains("pistas x slot x4")));
    }

    // ------------------------------------------------------ C10 (conectores)

    private static void gpu16PinosOcultaFontesSemConector() {
        Processador cpu = cpuAm5();
        PlacaMae placa = primeiraPlaca(cpu, TipoMemoria.DDR5);
        PlacaVideo gpu = PLACAS_VIDEO.stream()
                .map(PlacaVideo.class::cast)
                .filter(placaVideo -> placaVideo.getQuantidadeConectores16pin() == 1)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("base sem GPU de 16 pinos"));

        Montagem montagem = montagem(cpu, placa, gpu);
        int necessario = CalculadoraConsumo.consumoComMargemWatts(montagem);

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.FONTE, montagem);
        Assercao.verdadeiro("C10: so fontes com conector de 16 pinos",
                lista.getCompativeis().stream().map(Fonte.class::cast)
                        .allMatch(fonte -> fonte.getQuantidadeConectores16pin12VHPWR() >= 1));
        Assercao.verdadeiro("C14: so fontes com potencia para a configuracao",
                lista.getCompativeis().stream().map(Fonte.class::cast)
                        .allMatch(fonte -> fonte.getPotenciaTotalWatts() >= necessario));
        Assercao.verdadeiro("C10: motivo do conector de 16 pinos",
                lista.getOcultos().stream()
                        .anyMatch(oculto -> oculto.getMotivo()
                                .equals("sem conector de 16 pinos (a placa de video exige 1)")));
        Assercao.verdadeiro("C10: fonte sem o conector e ocultada",
                lista.getOcultos().stream().map(ItemOculto::getComponente).map(Fonte.class::cast)
                        .anyMatch(fonte -> fonte.getQuantidadeConectores16pin12VHPWR() == 0));
        Assercao.verdadeiro("C10: existe fonte compativel", !lista.getCompativeis().isEmpty());
    }

    private static void fonteSemConectorCpu() {
        Processador cpu = cpuAm5();
        PlacaMae placa = PlacaMae.builder()
                .id("placa-teste-cpu-8pin")
                .modelo("X670 Dual 8 pinos")
                .fabricante("Teste")
                .soqueteCPU(Soquete.AM5)
                .tipoMemoria(TipoMemoria.DDR5)
                .quantidadeSlotsMemoria(4)
                .capacidadeMaximaMemoriaGB(192)
                .quantidadeSlotsPCIe16(1)
                .pistasSlotPCIe16(16)
                .quantidadeConectoresCPU8pin(2)
                .construir();
        Montagem montagem = montagem(cpu, placa);

        Fonte umaTomada = Fonte.builder()
                .id("fonte-teste-1-cpu")
                .modelo("Fonte 1 conector CPU")
                .fabricante("Teste")
                .potenciaTotalWatts(850)
                .quantidadeConectoresCPU8pin(1)
                .construir();
        Assercao.verdadeiro("C10: fonte com 1 conector CPU e ocultada",
                new FiltroConectoresFonte().motivoIncompatibilidade(montagem, umaTomada)
                        .filter(motivo -> motivo.contains("para a CPU")).isPresent());

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.FONTE, montagem);
        Assercao.verdadeiro("C10: so fontes com 2 conectores de CPU",
                lista.getCompativeis().stream().map(Fonte.class::cast)
                        .allMatch(fonte -> fonte.getQuantidadeConectoresCPU8pin() >= 2));
        Assercao.verdadeiro("C10: fonte sem o conector de CPU fica oculta",
                lista.getOcultos().stream()
                        .anyMatch(oculto -> oculto.getMotivo().contains("para a CPU")));
        Assercao.verdadeiro("C10: existe fonte com 2 conectores",
                !lista.getCompativeis().isEmpty());
    }

    // ------------------------------------------------- C14 (potencia da fonte)

    private static void fonteFracaEOcultada() {
        Processador cpu = Processador.builder()
                .id("cpu-teste-500w")
                .modelo("CPU 500 W")
                .fabricante("Teste")
                .soquete(Soquete.AM5)
                .consumoMaximoWatts(500)
                .tiposMemoriaSuportados(TipoMemoria.DDR5)
                .construir();
        Montagem montagem = montagem(cpu);

        Fonte fraca = Fonte.builder()
                .id("fonte-teste-200w")
                .modelo("Fonte 200 W")
                .fabricante("Teste")
                .potenciaTotalWatts(200)
                .construir();

        Assercao.iguais("C14: consumo com margem de 40%", 770,
                CalculadoraConsumo.consumoComMargemWatts(montagem));
        Assercao.verdadeiro("C14: fonte de 200 W nao cobre 770 W",
                new FiltroFonteSuficiente().motivoIncompatibilidade(montagem, fraca)
                        .filter(motivo -> motivo.contains("200 W") && motivo.contains("770 W"))
                        .isPresent());

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.FONTE, montagem);
        Assercao.verdadeiro("C14: fontes compativeis entregam o necessario",
                lista.getCompativeis().stream().map(Fonte.class::cast)
                        .allMatch(fonte -> fonte.getPotenciaTotalWatts() >= 770));
        Assercao.verdadeiro("C14: fonte fraca oculta por potencia",
                lista.getOcultos().stream()
                        .anyMatch(oculto -> oculto.getMotivo().contains("com margem de 40%")));
        Assercao.verdadeiro("C14: existe fonte suficiente", !lista.getCompativeis().isEmpty());
    }

    // ------------------------------------------- C12 (ECC) e C06 (video integrado)

    private static void memoriaEcc() {
        Processador cpu = Processador.builder()
                .id("cpu-teste-sem-ecc")
                .modelo("CPU sem ECC")
                .fabricante("Teste")
                .soquete(Soquete.AM5)
                .tiposMemoriaSuportados(TipoMemoria.DDR5)
                .suportaECC(false)
                .construir();
        PlacaMae placa = PlacaMae.builder()
                .id("placa-teste-sem-ecc")
                .modelo("B650 sem ECC")
                .fabricante("Teste")
                .soqueteCPU(Soquete.AM5)
                .tipoMemoria(TipoMemoria.DDR5)
                .quantidadeSlotsMemoria(4)
                .capacidadeMaximaMemoriaGB(192)
                .suportaECC(false)
                .construir();

        Memoria ecc = Memoria.builder()
                .id("ram-teste-ecc")
                .modelo("Modulo ECC")
                .fabricante("Teste")
                .tipo(TipoMemoria.DDR5)
                .capacidadePorPenteGB(16)
                .quantidadePentes(1)
                .frequenciaMTs(4800)
                .latenciaCAS(40)
                .possuiECC(true)
                .construir();
        Memoria comum = Memoria.builder()
                .id("ram-teste-comum")
                .modelo("Modulo comum")
                .fabricante("Teste")
                .tipo(TipoMemoria.DDR5)
                .capacidadePorPenteGB(16)
                .quantidadePentes(1)
                .frequenciaMTs(4800)
                .latenciaCAS(40)
                .possuiECC(false)
                .construir();

        Montagem comPlaca = montagem(cpu, placa);
        Assercao.verdadeiro("C12: ECC sem placa com ECC e ocultada",
                new FiltroMemoriaECC().motivoIncompatibilidade(comPlaca, ecc)
                        .filter(motivo -> motivo.contains("placa-mae sem suporte a ECC")).isPresent());
        Assercao.verdadeiro("C12: memoria comum nao e afetada",
                new FiltroMemoriaECC().motivoIncompatibilidade(comPlaca, comum).isEmpty());

        Montagem soCpu = montagem(cpu);
        Assercao.verdadeiro("C12: ECC sem CPU com ECC e ocultada",
                new FiltroMemoriaECC().motivoIncompatibilidade(soCpu, ecc)
                        .filter(motivo -> motivo.contains("processador sem suporte a ECC"))
                        .isPresent());
    }

    private static void videoIntegradoNaoOcultaGpu() {
        Processador comVideo = CPUS.stream().map(Processador.class::cast)
                .filter(Processador::isPossuiVideoIntegrado).findFirst()
                .orElseThrow(() -> new IllegalStateException("base sem CPU com video integrado"));
        Processador semVideo = CPUS.stream().map(Processador.class::cast)
                .filter(cpu -> !cpu.isPossuiVideoIntegrado()).findFirst()
                .orElseThrow(() -> new IllegalStateException("base sem CPU sem video integrado"));
        PlacaVideo gpu = (PlacaVideo) PLACAS_VIDEO.get(0);

        Assercao.falso("C06: CPU com video integrado nao obriga GPU",
                FiltroVideoIntegrado.exigePlacaVideo(montagem(comVideo)));
        Assercao.verdadeiro("C06: CPU sem video integrado obriga GPU",
                FiltroVideoIntegrado.exigePlacaVideo(montagem(semVideo)));
        Assercao.verdadeiro("C06: a regra nao oculta nenhuma GPU",
                new FiltroVideoIntegrado().motivoIncompatibilidade(montagem(semVideo), gpu)
                        .isEmpty());
    }

    // ---------------------------------------- lista vazia e depuracao do filtro

    private static void nenhumCompativel() {
        Processador cpuAm3 = Processador.builder()
                .id("cpu-teste-am3")
                .modelo("Athlon AM3")
                .fabricante("Teste")
                .soquete(Soquete.AM3)
                .tiposMemoriaSuportados(TipoMemoria.DDR4)
                .construir();
        Montagem montagem = montagem(cpuAm3);

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.PLACA_MAE, montagem);
        Assercao.verdadeiro("C01: base nao tem placa AM3", lista.estaVazia());
        Assercao.iguais("C01: todas as placas ficam ocultas", PLACAS_MAE.size(),
                lista.quantidadeOcultos());
        Assercao.verdadeiro("C01: motivo predominante explica a lista vazia",
                lista.motivoPredominante().orElse("").contains("processador AM3"));
    }

    private static void explicarFiltroRegistraOcultos() {
        List<String> linhas = new ArrayList<>();
        FiltroEtapa explicativo = FiltroEtapa.explicativo(CATALOGO, linhas::add);

        ListaFiltrada lista = explicativo.filtrar(CategoriaComponente.PLACA_MAE,
                montagem(cpuAm5()));

        Assercao.iguais("explicar filtro: uma linha por item oculto",
                lista.quantidadeOcultos(), linhas.size());
        Assercao.verdadeiro("explicar filtro: linha cita o motivo do oculto",
                linhas.get(0).contains(lista.getOcultos().get(0).getMotivo()));
        Assercao.verdadeiro("explicar filtro: linha identifica o componente",
                linhas.get(0).contains(lista.getOcultos().get(0).getComponente().getModelo()));
    }

    // -------------------------------------------------- CLI: lista filtrada

    private static void telaListaFiltrada() {
        Componente primeira = CPUS.get(0);
        Componente segunda = CPUS.get(1);
        Componente oculta = CPUS.get(2);
        ListaFiltrada lista = new ListaFiltrada(CategoriaComponente.CPU,
                List.of(primeira, segunda),
                List.of(new ItemOculto(oculta, "soquete AM5 x processador LGA1700")));

        Saida saida = new Saida();
        TelaEtapa tela = new TelaEtapa(saida.terminal);
        tela.imprimirListaFiltrada(lista, componente -> "apoio " + componente.getModelo());
        String texto = saida.texto();

        Assercao.verdadeiro("tela: contagem exigida pela HU-003",
                texto.contains("2 componentes compativeis (1 oculto por incompatibilidade)"));
        Assercao.verdadeiro("tela: contagem anuncia o comando [M]",
                texto.contains("1 oculto por incompatibilidade)   [M] Ver ocultos"));
        Assercao.verdadeiro("tela: compativeis numerados",
                texto.contains(" 1)") && texto.contains(" 2)"));
        Assercao.falso("tela: item oculto nao recebe numero", texto.contains(" 3)"));
        Assercao.verdadeiro("tela: linha de apoio",
                texto.contains("apoio " + primeira.getModelo()));
        Assercao.iguais("tela: cabecalho das colunas uma vez", 1,
                contar(texto, TabelaComponentes.cabecalho()));

        tela.imprimirOcultos(lista);
        texto = saida.texto();
        Assercao.verdadeiro("tela: [M] mostra o motivo do oculto",
                texto.contains(TabelaComponentes.nomeExibicao(oculta)
                        + " - incompativel: soquete AM5 x processador LGA1700"));
        Assercao.verdadeiro("tela: [M] avisa que o item nao pode ser escolhido",
                texto.contains("nao podem ser escolhidos"));
        Assercao.iguais("tela: rodape padrao do comando", "[M] Ver ocultos",
                TelaEtapa.RODAPE_OCULTOS);
    }

    private static void telaSemCompativeis() {
        ListaFiltrada vazia = new ListaFiltrada(CategoriaComponente.PLACA_MAE, List.of(),
                List.of(new ItemOculto(CPUS.get(0), "motivo repetido"),
                        new ItemOculto(CPUS.get(1), "motivo repetido"),
                        new ItemOculto(CPUS.get(2), "motivo raro")));

        Saida saida = new Saida();
        new TelaEtapa(saida.terminal).imprimirSemCompativeis(vazia);
        String texto = saida.texto();

        Assercao.verdadeiro("tela: mensagem de nenhum compativel",
                texto.contains(TelaEtapa.MENSAGEM_SEM_COMPATIVEIS));
        Assercao.verdadeiro("tela: motivo predominante na explicacao",
                texto.contains("Motivo predominante: motivo repetido"));
        Assercao.verdadeiro("tela: oferece voltar", texto.contains("[0] Voltar"));
    }

    private static void contagemEMotivoPredominante() {
        ListaFiltrada semOcultos = new ListaFiltrada(CategoriaComponente.CPU,
                List.of(CPUS.get(0)), List.of());
        Assercao.iguais("contagem: singular e sem ocultos",
                "1 componente compativel (nenhum oculto por incompatibilidade)",
                TelaEtapa.textoContagem(semOcultos));

        ListaFiltrada tudoOculto = new ListaFiltrada(CategoriaComponente.PLACA_MAE, List.of(),
                List.of(new ItemOculto(CPUS.get(0), "motivo A"),
                        new ItemOculto(CPUS.get(1), "motivo A"),
                        new ItemOculto(CPUS.get(2), "motivo B")));
        Assercao.iguais("contagem: zero compativeis",
                "0 componentes compativeis (3 ocultos por incompatibilidade)",
                TelaEtapa.textoContagem(tudoOculto));
        Assercao.iguais("lista: motivo predominante", "motivo A",
                tudoOculto.motivoPredominante().orElse(null));
        Assercao.iguais("lista: total original", 3, tudoOculto.getTotalOriginal());
        Assercao.verdadeiro("lista: vazia em compativeis", tudoOculto.estaVazia());
        Assercao.verdadeiro("lista: sem ocultos nao tem motivo",
                semOcultos.motivoPredominante().isEmpty());

        Saida saidaSemOcultos = new Saida();
        new TelaEtapa(saidaSemOcultos.terminal).imprimirListaFiltrada(semOcultos, null);
        Assercao.falso("tela: sem ocultos nao anuncia o comando [M]",
                saidaSemOcultos.texto().contains(TelaEtapa.RODAPE_OCULTOS));
    }

    // ------------------------------------------------------------ consumo (L06)

    private static void calculoDoConsumo() {
        Montagem montagem = new Montagem();
        montagem.adicionar(cpuComConsumo(140));
        montagem.adicionar(PlacaVideo.builder()
                .id("gpu-teste").modelo("GPU 220 W").fabricante("Teste")
                .tdpWatts(220).pistasPCIe(16).versaoPCIe(4).construir());
        montagem.adicionar(PlacaMae.builder()
                .id("placa-teste").modelo("Placa 30 W").fabricante("Teste")
                .consumoEstimadoWatts(30).construir());
        montagem.adicionar(Memoria.builder()
                .id("ram-teste").modelo("RAM 6 W").fabricante("Teste")
                .tipo(TipoMemoria.DDR5).capacidadePorPenteGB(16).quantidadePentes(2)
                .consumoEstimadoWatts(6).construir());
        montagem.adicionar(Armazenamento.builder()
                .id("ssd-teste").modelo("SSD 5 W").fabricante("Teste")
                .interfaceArmazenamento(InterfaceArmazenamento.NVME)
                .consumoEstimadoWatts(5).construir());

        Assercao.iguais("consumo: soma CPU + GPU + auxiliares", 401,
                CalculadoraConsumo.consumoTotalSistemaWatts(montagem));
        Assercao.iguais("consumo: margem de 40% arredonda para cima", 562,
                CalculadoraConsumo.consumoComMargemWatts(montagem));

        Montagem soCpu = montagem(cpuComConsumo(100));
        Assercao.iguais("consumo: reserva entra uma unica vez", 150,
                CalculadoraConsumo.consumoTotalSistemaWatts(soCpu));
    }

    // ---------------------------------------------------------------- auxiliares

    private static Processador cpuAm5() {
        return CPUS.stream().map(Processador.class::cast)
                .filter(cpu -> cpu.getSoquete() == Soquete.AM5)
                .filter(cpu -> cpu.getTiposMemoriaSuportados().contains(TipoMemoria.DDR5))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("base sem CPU AM5"));
    }

    private static Processador cpuComConsumo(int consumoMaximoWatts) {
        return Processador.builder()
                .id("cpu-teste-" + consumoMaximoWatts)
                .modelo("CPU " + consumoMaximoWatts + " W")
                .fabricante("Teste")
                .soquete(Soquete.AM5)
                .consumoMaximoWatts(consumoMaximoWatts)
                .tiposMemoriaSuportados(TipoMemoria.DDR5)
                .construir();
    }

    private static PlacaMae primeiraPlaca(Processador cpu, TipoMemoria tipo) {
        return FILTRO.filtrar(CategoriaComponente.PLACA_MAE, montagem(cpu))
                .getCompativeis().stream()
                .map(PlacaMae.class::cast)
                .filter(placa -> placa.getTipoMemoria() == tipo)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("base sem placa " + tipo));
    }

    private static Montagem montagem(Componente... pecas) {
        Montagem montagem = new Montagem();
        for (Componente peca : pecas) {
            montagem.adicionar(peca);
        }
        return montagem;
    }

    /** Quantas vezes o trecho aparece no texto. */
    private static int contar(String texto, String trecho) {
        int total = 0;
        int posicao = texto.indexOf(trecho);
        while (posicao >= 0) {
            total++;
            posicao = texto.indexOf(trecho, posicao + trecho.length());
        }
        return total;
    }

    /** Terminal com a saida capturada em memoria (os testes nao digitam nada). */
    private static final class Saida {

        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final Terminal terminal;

        Saida() {
            PrintStream stream = new PrintStream(bytes, true, StandardCharsets.UTF_8);
            this.terminal = new Terminal(new StringReader(""), stream);
        }

        String texto() {
            return bytes.toString(StandardCharsets.UTF_8);
        }
    }
}
