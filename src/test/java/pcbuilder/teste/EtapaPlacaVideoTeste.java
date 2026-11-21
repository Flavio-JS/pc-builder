package pcbuilder.teste;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import pcbuilder.analise.filtro.FiltroEtapa;
import pcbuilder.analise.filtro.ListaFiltrada;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.cli.MenuPrincipal;
import pcbuilder.cli.TabelaComponentes;
import pcbuilder.cli.Terminal;
import pcbuilder.cli.etapas.EtapaPlacaVideo;
import pcbuilder.cli.etapas.ResultadoEtapa;
import pcbuilder.config.OrdemEtapas;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.Chipset;
import pcbuilder.enums.FormatoPlacaMae;
import pcbuilder.enums.Resolucao;
import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;
import pcbuilder.enums.TipoMemoriaVRAM;
import pcbuilder.mock.CatalogoMock;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.PlacaVideo;
import pcbuilder.modelo.Processador;
import pcbuilder.servico.MontagemGuiada;
import pcbuilder.servico.resumos.ResumoPlacaVideo;

/**
 * Testes da HU-007: etapa 4 (placa de video) condicional ao video integrado da CPU,
 * filtro C05, avisos L05/L07, {@code [0] Pular}/{@code [0] Voltar} e a mensagem de
 * selecao.
 *
 * <p>Execucao (sem JUnit):
 * {@code java -cp build/classes;build/classes-test pcbuilder.teste.EtapaPlacaVideoTeste}.</p>
 */
public final class EtapaPlacaVideoTeste {

    private static final CatalogoMock CATALOGO = CatalogoMock.criar();
    private static final FiltroEtapa FILTRO = new FiltroEtapa(CATALOGO);
    private static final AvaliadorLimitacoes AVALIADOR = new AvaliadorLimitacoes();

    private EtapaPlacaVideoTeste() {
        // classe utilitaria: nao deve ser instanciada
    }

    public static void main(String[] args) {
        System.out.println("Testes da HU-007 - etapa 4 (placa de video)");

        obrigatoriedadeDaEtapa();
        resumoDaLinhaDeApoio();
        etapaOpcionalPula();
        etapaObrigatoriaVolta();
        etapaSemGpuCompativel();
        marcaPcieComAviso();
        marcaVramComAviso();
        menuPulaEtapaOpcional();

        System.exit(Assercao.concluir());
    }

    // ------------------------------------------------- C06 (etapa condicional)

    private static void obrigatoriedadeDaEtapa() {
        Montagem comVideo = montagem(cpu(true));
        Montagem semVideo = montagem(cpu(false));

        Assercao.falso("C06: CPU com video integrado torna a etapa opcional",
                OrdemEtapas.obrigatoria(4, comVideo));
        Assercao.verdadeiro("C06: CPU sem video integrado torna a etapa obrigatoria",
                OrdemEtapas.obrigatoria(4, semVideo));
        Assercao.verdadeiro("C06: exige placa de video sem iGPU",
                OrdemEtapas.exigePlacaVideo(semVideo));
        Assercao.falso("C06: nao exige placa de video com iGPU",
                OrdemEtapas.exigePlacaVideo(comVideo));
        for (int etapa = 1; etapa <= OrdemEtapas.TOTAL_ETAPAS; etapa++) {
            if (etapa != 4) {
                Assercao.verdadeiro("C06: etapa " + etapa + " continua obrigatoria",
                        OrdemEtapas.obrigatoria(etapa, comVideo));
            }
        }
    }

    // ------------------------------------------------------------ linha de apoio

    private static void resumoDaLinhaDeApoio() {
        PlacaVideo gpu = PlacaVideo.builder()
                .id("gpu-teste-resumo")
                .modelo("RTX Teste")
                .fabricante("NVIDIA")
                .memoriaVRAMGB(12)
                .tipoMemoriaVRAM(TipoMemoriaVRAM.GDDR6X)
                .versaoPCIe(4)
                .pistasPCIe(16)
                .tdpWatts(220)
                .quantidadeConectores16pin(1)
                .construir();
        Assercao.iguais("resumo: linha de apoio da etapa 4",
                "12 GB GDDR6X | PCIe 4.0 x16 | 220 W | 1x 16-pinos",
                ResumoPlacaVideo.linhaDeApoio(gpu));

        PlacaVideo semConector = PlacaVideo.builder()
                .id("gpu-teste-sem-conector")
                .modelo("GPU Basica")
                .fabricante("Teste")
                .memoriaVRAMGB(4)
                .tipoMemoriaVRAM(TipoMemoriaVRAM.GDDR6)
                .versaoPCIe(3)
                .pistasPCIe(16)
                .tdpWatts(75)
                .construir();
        Assercao.verdadeiro("resumo: GPU sem conector extra",
                ResumoPlacaVideo.linhaDeApoio(semConector).contains("sem conector extra"));
        Assercao.iguais("resumo: componente que nao e GPU", "",
                ResumoPlacaVideo.linhaDeApoio(cpu(true)));
    }

    // ----------------------------------------------------- pular / voltar (tela)

    private static void etapaOpcionalPula() {
        Montagem montagem = montagem(cpu(true), placa(4, 1, 16));
        MontagemGuiada fluxo = fluxoNaEtapa4(montagem);
        Saida saida = new Saida("0\n");

        ResultadoEtapa resultado = new EtapaPlacaVideo(saida.terminal, fluxo, FILTRO)
                .executar();

        Assercao.iguais("C06: pular a etapa opcional conclui", ResultadoEtapa.CONCLUIDA,
                resultado);
        String texto = saida.texto();
        Assercao.verdadeiro("C06: cabecalho opcional",
                texto.contains("=== Etapa 4 de 6 - Placa de Video (opcional) ==="));
        Assercao.verdadeiro("C06: rodape oferece pular", texto.contains(EtapaPlacaVideo.RODAPE_PULAR));
        Assercao.verdadeiro("C06: mensagem de pulo", texto.contains(EtapaPlacaVideo.MENSAGEM_PULADA));
        Assercao.verdadeiro("C06: nenhuma GPU guardada ao pular",
                montagem.obter(CategoriaComponente.PLACA_VIDEO).isEmpty());
    }

    private static void etapaObrigatoriaVolta() {
        Montagem montagem = montagem(cpu(false), placa(4, 1, 16));
        MontagemGuiada fluxo = fluxoNaEtapa4(montagem);
        Saida saida = new Saida("0\n");

        ResultadoEtapa resultado = new EtapaPlacaVideo(saida.terminal, fluxo, FILTRO)
                .executar();

        Assercao.iguais("C06: [0] na etapa obrigatoria volta", ResultadoEtapa.VOLTAR,
                resultado);
        String texto = saida.texto();
        Assercao.verdadeiro("C06: cabecalho obrigatorio",
                texto.contains("=== Etapa 4 de 6 - Placa de Video (obrigatorio) ==="));
        Assercao.verdadeiro("C06: explica por que e obrigatoria",
                texto.contains(EtapaPlacaVideo.MENSAGEM_OBRIGATORIA));
        Assercao.verdadeiro("C06: rodape oferece voltar",
                texto.contains(EtapaPlacaVideo.RODAPE_VOLTAR));
        Assercao.falso("C06: etapa obrigatoria nao oferece pular",
                texto.contains(EtapaPlacaVideo.RODAPE_PULAR));
        Assercao.verdadeiro("C06: nenhuma GPU guardada ao voltar",
                montagem.obter(CategoriaComponente.PLACA_VIDEO).isEmpty());
    }

    private static void etapaSemGpuCompativel() {
        Montagem obrigatoria = montagem(cpu(false), placa(4, 0, 16));
        MontagemGuiada fluxoObrigatorio = fluxoNaEtapa4(obrigatoria);
        Saida saidaObrigatoria = new Saida("0\n");
        ResultadoEtapa resultadoObrigatorio = new EtapaPlacaVideo(
                saidaObrigatoria.terminal, fluxoObrigatorio, FILTRO).executar();

        Assercao.iguais("C05: sem GPU compativel a etapa obrigatoria volta",
                ResultadoEtapa.VOLTAR, resultadoObrigatorio);
        String texto = saidaObrigatoria.texto();
        Assercao.verdadeiro("C05: mensagem especifica de GPU",
                texto.contains(EtapaPlacaVideo.MENSAGEM_SEM_COMPATIVEIS));
        Assercao.verdadeiro("C05: motivo do slot x16",
                texto.contains("placa-mae sem slot x16 livre"));
        Assercao.verdadeiro("C05: rodape de voltar na etapa obrigatoria",
                texto.contains("[0] Voltar"));

        Montagem opcional = montagem(cpu(true), placa(4, 0, 16));
        MontagemGuiada fluxoOpcional = fluxoNaEtapa4(opcional);
        Saida saidaOpcional = new Saida("0\n");
        ResultadoEtapa resultadoOpcional = new EtapaPlacaVideo(
                saidaOpcional.terminal, fluxoOpcional, FILTRO).executar();

        Assercao.iguais("C05: sem GPU compativel a etapa opcional pode pular",
                ResultadoEtapa.CONCLUIDA, resultadoOpcional);
        String textoOpcional = saidaOpcional.texto();
        Assercao.verdadeiro("C05: mensagem especifica tambem na opcional",
                textoOpcional.contains(EtapaPlacaVideo.MENSAGEM_SEM_COMPATIVEIS));
        Assercao.verdadeiro("C05: rodape de pular na etapa opcional",
                textoOpcional.contains("[0] Pular"));
        Assercao.verdadeiro("C05: pulo confirmado sem GPU",
                textoOpcional.contains(EtapaPlacaVideo.MENSAGEM_PULADA));
    }

    // ------------------------------------------------------ L05 / L07 (avisos)

    private static void marcaPcieComAviso() {
        Processador cpu = cpu(false);
        PlacaMae placa = placa(3, 1, 16);
        Montagem montagem = montagem(cpu, placa);
        MontagemGuiada fluxo = fluxoNaEtapa4(montagem);

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.PLACA_VIDEO, montagem);
        int numero = 0;
        PlacaVideo alvo = null;
        for (int indice = 0; indice < lista.getCompativeis().size(); indice++) {
            PlacaVideo gpu = (PlacaVideo) lista.getCompativeis().get(indice);
            boolean l05 = AVALIADOR.avaliar(montagem, gpu).stream()
                    .anyMatch(limitacao -> "L05".equals(limitacao.getRegraId()));
            if (gpu.getVersaoPCIe() >= 4 && l05) {
                numero = indice + 1;
                alvo = gpu;
                break;
            }
        }
        Assercao.verdadeiro("L05: existe GPU de geracao maior que o slot", alvo != null);

        Saida saida = new Saida(numero + "\n\n");
        ResultadoEtapa resultado = new EtapaPlacaVideo(saida.terminal, fluxo, FILTRO)
                .executar();

        Assercao.iguais("L05: GPU com perda de banda continua selecionavel",
                ResultadoEtapa.CONCLUIDA, resultado);
        Assercao.iguais("L05: GPU marcada guardada", alvo,
                montagem.obter(CategoriaComponente.PLACA_VIDEO).orElse(null));

        String texto = saida.texto();
        Assercao.verdadeiro("L05: linha marcada com [!]",
                linhaCom(texto, ResumoPlacaVideo.linhaDeApoio(alvo))
                        .startsWith(TabelaComponentes.MARCADOR_LIMITACAO));
        Assercao.verdadeiro("L05: aviso cita a banda e o slot",
                texto.contains("a banda cai") && texto.contains("PCIe 3.0"));
        Assercao.verdadeiro("L05: aviso acumulado na montagem",
                montagem.getLimitacoes().stream()
                        .anyMatch(limitacao -> "L05".equals(limitacao.getRegraId())));
    }

    private static void marcaVramComAviso() {
        Processador cpu = cpu(false);
        PlacaMae placa = placa(4, 1, 16);
        Montagem montagem = montagem(cpu, placa);
        MontagemGuiada fluxo = fluxoNaEtapa4(montagem);

        ListaFiltrada lista = FILTRO.filtrar(CategoriaComponente.PLACA_VIDEO, montagem);
        int numero = 0;
        PlacaVideo alvo = null;
        for (int indice = 0; indice < lista.getCompativeis().size(); indice++) {
            PlacaVideo gpu = (PlacaVideo) lista.getCompativeis().get(indice);
            boolean l07 = gpu.getVersaoPCIe() <= 4
                    && AVALIADOR.avaliar(montagem, gpu).stream()
                            .anyMatch(limitacao -> "L07".equals(limitacao.getRegraId()));
            if (l07) {
                numero = indice + 1;
                alvo = gpu;
                break;
            }
        }
        Assercao.verdadeiro("L07: existe GPU com VRAM abaixo da resolucao", alvo != null);

        Saida saida = new Saida(numero + "\n\n");
        ResultadoEtapa resultado = new EtapaPlacaVideo(saida.terminal, fluxo, FILTRO)
                .executar();

        Assercao.iguais("L07: GPU com VRAM curta continua selecionavel",
                ResultadoEtapa.CONCLUIDA, resultado);
        Assercao.verdadeiro("L07: aviso fala da VRAM",
                saida.texto().contains("VRAM"));
        Assercao.verdadeiro("L07: aviso acumulado na montagem",
                montagem.getLimitacoes().stream()
                        .anyMatch(limitacao -> "L07".equals(limitacao.getRegraId())));
    }

    // ------------------------------------------------------- fluxo pelo menu

    private static void menuPulaEtapaOpcional() {
        int indiceCpu = indiceCpu(true);
        Processador cpu = cpuDoCatalogo(true);
        Componente placa = FILTRO.filtrar(CategoriaComponente.PLACA_MAE, montagem(cpu))
                .getCompativeis().get(0);
        Montagem base = montagem(cpu, placa);
        Componente memoria = FILTRO.filtrar(CategoriaComponente.MEMORIA, base)
                .getCompativeis().get(0);
        boolean avisoPlaca = !AVALIADOR.avaliar(montagem(cpu), placa).isEmpty();
        boolean avisoMemoria = !AVALIADOR.avaliar(base, memoria).isEmpty();

        String entrada = "1\n" + indiceCpu + "\n1\n" + (avisoPlaca ? "\n" : "")
                + "1\n" + (avisoMemoria ? "\n" : "") + "0\n";
        Saida saida = new Saida(entrada);
        int codigo = new MenuPrincipal(CATALOGO, saida.terminal).executar();

        Assercao.iguais("menu: execucao termina sem erro", 0, codigo);
        String texto = saida.texto();
        Assercao.verdadeiro("menu: etapa 4 aparece como opcional",
                texto.contains("=== Etapa 4 de 6 - Placa de Video (opcional) ==="));
        Assercao.verdadeiro("menu: pulo da GPU confirmado",
                texto.contains(EtapaPlacaVideo.MENSAGEM_PULADA));
        Assercao.verdadeiro("menu: avanca para a etapa 5",
                texto.contains("=== Etapa 5 de 6 - Armazenamento (obrigatorio) ==="));
    }

    // ---------------------------------------------------------------- auxiliares

    private static Processador cpu(boolean videoIntegrado) {
        return Processador.builder()
                .id("cpu-teste-video-" + videoIntegrado)
                .modelo("Ryzen Teste Video")
                .fabricante("AMD")
                .soquete(Soquete.AM5)
                .nucleosFisicos(8)
                .threads(16)
                .frequenciaBaseMHz(3800)
                .frequenciaBoostMHz(5500)
                .tdpWatts(65)
                .consumoMaximoWatts(100)
                .tensaoMaximaVolts(1.25)
                .tiposMemoriaSuportados(TipoMemoria.DDR5)
                .canaisMemoria(2)
                .frequenciaMemoriaNativaMTs(5200)
                .frequenciaMaximaSincronaMTs(6000)
                .capacidadeMaximaMemoriaGB(192)
                .pistasPCIeLivres(24)
                .possuiVideoIntegrado(videoIntegrado)
                .indiceDesempenhoSingleThread(90)
                .indiceDesempenhoMultithread(90)
                .construir();
    }

    private static PlacaMae placa(int versaoSlot, int slotsPcie, int pistasSlot) {
        return PlacaMae.builder()
                .id("placa-video-" + versaoSlot + "-" + slotsPcie + "-" + pistasSlot)
                .modelo("B650 Teste")
                .fabricante("ASUS")
                .soqueteCPU(Soquete.AM5)
                .chipset(Chipset.B650)
                .formato(FormatoPlacaMae.MATX)
                .quantidadeSlotsMemoria(4)
                .tipoMemoria(TipoMemoria.DDR5)
                .canaisMemoria(2)
                .capacidadeMaximaMemoriaGB(192)
                .frequenciaMemoriaMaximaMTs(6000)
                .suportaPerfilEXPO(true)
                .suportaPerfilXMP(true)
                .vrmFasesCPU(8)
                .vrmCorrentePorFaseA(50)
                .vrmPossuiDissipador(true)
                .quantidadeSlotsPCIe16(slotsPcie)
                .versaoSlotPCIe16(versaoSlot)
                .pistasSlotPCIe16(pistasSlot)
                .construir();
    }

    private static Montagem montagem(Componente... pecas) {
        Montagem montagem = new Montagem();
        for (Componente peca : pecas) {
            montagem.adicionar(peca);
        }
        return montagem;
    }

    private static MontagemGuiada fluxoNaEtapa4(Montagem montagem) {
        MontagemGuiada fluxo = new MontagemGuiada(montagem);
        fluxo.proximaEtapa();
        fluxo.proximaEtapa();
        fluxo.proximaEtapa();
        return fluxo;
    }

    private static Processador cpuDoCatalogo(boolean videoIntegrado) {
        return CATALOGO.porCategoria(CategoriaComponente.CPU).stream()
                .map(Processador.class::cast)
                .filter(cpu -> cpu.isPossuiVideoIntegrado() == videoIntegrado)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "base sem CPU com videoIntegrado=" + videoIntegrado));
    }

    private static int indiceCpu(boolean videoIntegrado) {
        List<Componente> cpus = CATALOGO.porCategoria(CategoriaComponente.CPU);
        for (int indice = 0; indice < cpus.size(); indice++) {
            if (((Processador) cpus.get(indice)).isPossuiVideoIntegrado() == videoIntegrado) {
                return indice + 1;
            }
        }
        throw new IllegalStateException("base sem CPU com videoIntegrado=" + videoIntegrado);
    }

    private static String linhaCom(String texto, String trecho) {
        for (String linha : texto.split("\\R")) {
            if (linha.contains(trecho)) {
                return linha;
            }
        }
        return "";
    }

    /** Terminal com a entrada digitada e a saida capturada em memoria. */
    private static final class Saida {

        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final Terminal terminal;

        Saida(String entrada) {
            PrintStream stream = new PrintStream(bytes, true, StandardCharsets.UTF_8);
            this.terminal = new Terminal(new StringReader(entrada), stream);
        }

        String texto() {
            return bytes.toString(StandardCharsets.UTF_8);
        }
    }
}
