package pcbuilder.teste;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import pcbuilder.analise.filtro.FiltroTipoMemoria;
import pcbuilder.analise.filtro.ListaFiltrada;
import pcbuilder.analise.limitacao.AvaliadorLimitacoes;
import pcbuilder.analise.limitacao.TextosLimitacao;
import pcbuilder.cli.TabelaComponentes;
import pcbuilder.cli.TelaEtapa;
import pcbuilder.cli.Terminal;
import pcbuilder.cli.etapas.EtapaProcessador;
import pcbuilder.cli.etapas.ResultadoEtapa;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.InterfaceArmazenamento;
import pcbuilder.enums.Resolucao;
import pcbuilder.enums.SeveridadeLimitacao;
import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.PlacaVideo;
import pcbuilder.modelo.Processador;
import pcbuilder.servico.MontagemGuiada;

/**
 * Testes da HU-004: avisos de limitacao (L01, L02, L04-L07, L09-L11).
 *
 * <p>Cobrem o caso obrigatorio (placa de 6000 MT/s x memoria de 8000 MT/s marcada,
 * selecionavel e com "~25%"), o aviso nos dois sentidos (placa escolhida depois da CPU),
 * a ausencia de aviso falso quando falta dado, a renderizacao do {@code [!]} e do bloco
 * com {@code [Enter] para continuar}, o acumulo na {@link Montagem} e o comando
 * {@code [L] Avisos}.</p>
 *
 * <p>Execucao (sem JUnit):
 * {@code java -cp build/classes;build/classes-test pcbuilder.teste.LimitacoesTeste}.</p>
 */
public final class LimitacoesTeste {

    private static final AvaliadorLimitacoes AVALIADOR = new AvaliadorLimitacoes();

    private LimitacoesTeste() {
        // classe utilitaria: nao deve ser instanciada
    }

    public static void main(String[] args) {
        System.out.println("Testes da HU-004 - avisos de limitacao");

        memoriaAcimaDoTetoDaPlaca();
        memoriaEmSingleChannel();
        vrmDentroEAcimaDoLimite();
        placaEscolhidaDepoisDaCpuForte();
        semDadosNaoGeraAvisoFalso();
        picoTransienteAcimaDoTrilho();
        eficienciaDaFonte();
        pcieDeGeracaoMenor();
        vramAbaixoDaResolucao();
        gargaloDeCpu();
        telaMarcaItensComAviso();
        acumuloEAvisosDaMontagem();
        comandoLAvisosNaEtapa1();
        tabelaComMarcador();

        System.exit(Assercao.concluir());
    }

    // ------------------------------------------------------------ L04 (memoria)

    private static void memoriaAcimaDoTetoDaPlaca() {
        Processador cpu = cpu(140, 1.25, 90);
        PlacaMae placa = placa(6000, 12, 60, true, 4, 16);
        Memoria rapida = memoria("Vengeance 8000", 8000, 38, 16, 2);
        Montagem atual = montagem(cpu, placa);

        List<Limitacao> avisos = AVALIADOR.avaliar(atual, rapida);
        Assercao.iguais("L04: caso 8000 x 6000 gera um aviso", 1, avisos.size());
        Limitacao aviso = avisos.get(0);

        Assercao.iguais("L04: regra", "L04", aviso.getRegraId());
        Assercao.iguais("L04: severidade AVISO", SeveridadeLimitacao.AVISO,
                aviso.getSeveridade());
        Assercao.iguais("L04: limitante e a placa-mae", placa, aviso.getComponenteLimitante());
        Assercao.iguais("L04: afetado e a memoria", rapida, aviso.getComponenteAfetado());
        Assercao.iguais("L04: campo comparado", "frequenciaMTs", aviso.getCampoComparado());
        Assercao.iguais("L04: valor do novo", "8000 MT/s", aviso.getValorDoNovo());
        Assercao.iguais("L04: valor suportado", "6000 MT/s", aviso.getValorSuportado());
        Assercao.iguais("L04: perda de ~25%", 25.0, aviso.getPercentualPerda());

        String texto = String.join("\n", TextosLimitacao.linhas(aviso));
        Assercao.verdadeiro("L04: aviso cita 6000 MT/s", texto.contains("6000 MT/s"));
        Assercao.verdadeiro("L04: aviso cita ~25%", texto.contains("~25%"));
        Assercao.verdadeiro("L04: aviso nomeia a placa-mae", texto.contains("ASUS B650 Teste"));
        Assercao.verdadeiro("L04: aviso mantem a escolha valida",
                texto.contains(TextosLimitacao.ESCOLHA_VALIDA));
        Assercao.verdadeiro("L04: texto usa o formato da secao 2.3",
                texto.contains("limita") && texto.contains("frequenciaMTs:"));

        Assercao.verdadeiro("HU-004: filtro nao oculta a memoria de 8000 MT/s",
                new FiltroTipoMemoria().motivoIncompatibilidade(atual, rapida).isEmpty());
    }

    private static void memoriaEmSingleChannel() {
        Processador cpu = cpu(65, 1.25, 80);
        PlacaMae placa = placa(6000, 12, 60, true, 4, 16);
        Memoria umPente = memoria("Vengeance 1x16", 6000, 30, 16, 1);
        Montagem atual = montagem(cpu, placa);

        List<Limitacao> avisos = AVALIADOR.avaliar(atual, umPente);
        Assercao.iguais("L04: 1 pente em placa de 2 canais gera aviso", 1, avisos.size());
        Limitacao aviso = avisos.get(0);
        Assercao.iguais("L04: campo do single channel", "quantidadePentes",
                aviso.getCampoComparado());
        Assercao.verdadeiro("L04: consequencia fala de single channel",
                aviso.getConsequencia().contains("single channel"));

        Memoria doisPentes = memoria("Vengeance 2x16", 6000, 30, 16, 2);
        Assercao.verdadeiro("L04: 2 pentes nao geram aviso de canal",
                AVALIADOR.avaliar(atual, doisPentes).isEmpty());
    }

    // --------------------------------------------------------------- L02 (VRM)

    private static void vrmDentroEAcimaDoLimite() {
        Processador cpu = cpu(140, 1.25, 90);

        PlacaMae folgada = placa(6000, 12, 60, true, 4, 16);
        Assercao.verdadeiro("L02: VRM de 720 A nao gera aviso",
                AVALIADOR.avaliar(montagem(cpu), folgada).isEmpty());

        PlacaMae apertada = placa(6000, 2, 60, true, 4, 16);
        List<Limitacao> avisos = AVALIADOR.avaliar(montagem(cpu), apertada);
        Assercao.iguais("L02: VRM apertado gera aviso", 1, avisos.size());
        Limitacao aviso = avisos.get(0);
        Assercao.iguais("L02: regra", "L02", aviso.getRegraId());
        Assercao.iguais("L02: severidade RISCO", SeveridadeLimitacao.RISCO,
                aviso.getSeveridade());
        Assercao.iguais("L02: limitante e a placa", apertada, aviso.getComponenteLimitante());
        Assercao.iguais("L02: afetado e a CPU", cpu, aviso.getComponenteAfetado());
        Assercao.verdadeiro("L02: consequencia fala do VRM e do calor",
                aviso.getConsequencia().contains("VRM")
                        && aviso.getConsequencia().contains("calor"));

        PlacaMae semDissipador = placa(6000, 4, 40, Boolean.FALSE, 4, 16);
        List<Limitacao> semDissipadorAvisos = AVALIADOR.avaliar(montagem(cpu), semDissipador);
        Assercao.iguais("L02: sem dissipador o limite cai para 60%", 1,
                semDissipadorAvisos.size());
        Assercao.verdadeiro("L02: aviso cita limite de 60%",
                semDissipadorAvisos.get(0).getConsequencia().contains("limite seguro: 60%"));
    }

    private static void placaEscolhidaDepoisDaCpuForte() {
        Processador cpu = cpu(140, 1.25, 90);
        PlacaMae apertada = placa(6000, 2, 60, true, 4, 16);

        List<Limitacao> avisos = AVALIADOR.avaliar(montagem(cpu), apertada);
        Assercao.iguais("L02: aviso tambem quando a placa vem depois", 1, avisos.size());
        Assercao.iguais("L02: aviso no sentido inverso aponta a placa", apertada,
                avisos.get(0).getComponenteLimitante());
        Assercao.iguais("L02: aviso no sentido inverso aponta a CPU", cpu,
                avisos.get(0).getComponenteAfetado());
    }

    // ------------------------------------------- L06 / L09 (fonte) e dados ausentes

    private static void semDadosNaoGeraAvisoFalso() {
        Processador cpu = cpu(100, 1.25, 90);
        PlacaVideo gpu = gpu(12, 4, 16, 220, null, Resolucao.QHD_1440P, 85, 1);
        Montagem atual = montagem(cpu, gpu);
        Fonte fonte = fonte(750, 744, 1, 2);

        List<Limitacao> avisos = AVALIADOR.avaliar(atual, fonte);
        Assercao.verdadeiro("L06: GPU sem pico transiente nao gera aviso falso",
                avisos.stream().noneMatch(aviso -> "L06".equals(aviso.getRegraId())));
        Assercao.verdadeiro("L06: o dado ausente aparece no detalhe",
                AVALIADOR.pendencias(atual, fonte).stream()
                        .anyMatch(pendencia -> pendencia.startsWith("L06")));
    }

    private static void picoTransienteAcimaDoTrilho() {
        Processador cpu = cpu(100, 1.25, 90);
        PlacaVideo gpu = gpu(12, 4, 16, 220, 350, Resolucao.QHD_1440P, 85, 1);
        Fonte fonte = fonte(750, 250, 1, 2);

        List<Limitacao> avisos = AVALIADOR.avaliar(montagem(cpu, gpu), fonte);
        Limitacao aviso = aviso(avisos, "L06");
        Assercao.verdadeiro("L06: pico acima do trilho gera aviso", aviso != null);
        Assercao.iguais("L06: severidade RISCO", SeveridadeLimitacao.RISCO,
                aviso.getSeveridade());
        Assercao.iguais("L06: valor do novo", "350 W", aviso.getValorDoNovo());
        Assercao.iguais("L06: valor suportado", "250 W", aviso.getValorSuportado());
        Assercao.verdadeiro("L06: consequencia fala em desligar",
                aviso.getConsequencia().contains("desligar"));
    }

    private static void eficienciaDaFonte() {
        Processador cpu = cpu(140, 1.25, 90);
        PlacaMae placa = placa(6000, 12, 60, true, 4, 16);
        Memoria ram = memoria("Vengeance 2x16", 6000, 30, 16, 2);
        PlacaVideo gpu = gpu(12, 4, 16, 220, null, Resolucao.QHD_1440P, 85, 1);
        Armazenamento ssd = ssd(4, 7000);
        Montagem atual = montagem(cpu, placa, ram, gpu, ssd);

        Fonte exagerada = fonte(1200, 1188, 1, 2);
        Limitacao aviso = aviso(AVALIADOR.avaliar(atual, exagerada), "L09");
        Assercao.verdadeiro("L09: fonte de 1200 W para 401 W gera aviso", aviso != null);
        Assercao.verdadeiro("L09: aviso cita a carga estimada",
                aviso.getConsequencia().contains("~33%"));

        Fonte equilibrada = fonte(750, 744, 1, 2);
        Assercao.verdadeiro("L09: fonte de 750 W fica na faixa eficiente",
                AVALIADOR.avaliar(atual, equilibrada).stream()
                        .noneMatch(limitacao -> "L09".equals(limitacao.getRegraId())));
    }

    // ------------------------------------------------- L05 / L07 / L01 (GPU)

    private static void pcieDeGeracaoMenor() {
        Processador cpu = cpu(140, 1.25, 90);
        PlacaMae placa = placa(6000, 12, 60, true, 3, 16);
        PlacaVideo gpu = gpu(12, 4, 16, 220, 350, Resolucao.QHD_1440P, 85, 0);

        Limitacao aviso = aviso(AVALIADOR.avaliar(montagem(cpu, placa), gpu), "L05");
        Assercao.verdadeiro("L05: PCIe 4.0 em slot 3.0 gera aviso", aviso != null);
        Assercao.iguais("L05: perda de metade da banda", 50L,
                Math.round(aviso.getPercentualPerda()));
        Assercao.verdadeiro("L05: aviso cita as duas geracoes",
                aviso.getValorDoNovo().contains("PCIe 4.0")
                        && aviso.getValorSuportado().contains("PCIe 3.0"));

        PlacaMae slotIgual = placa(6000, 12, 60, true, 4, 16);
        Assercao.verdadeiro("L05: slot de mesma geracao nao gera aviso",
                AVALIADOR.avaliar(montagem(cpu, slotIgual), gpu).stream()
                        .noneMatch(limitacao -> "L05".equals(limitacao.getRegraId())));
    }

    private static void vramAbaixoDaResolucao() {
        Processador cpu = cpu(140, 1.25, 90);
        PlacaMae placa = placa(6000, 12, 60, true, 4, 16);
        PlacaVideo gpu = gpu(8, 4, 16, 220, 350, Resolucao.QHD_1440P, 85, 0);

        Limitacao aviso = aviso(AVALIADOR.avaliar(montagem(cpu, placa), gpu), "L07");
        Assercao.verdadeiro("L07: 8 GB para 1440p gera aviso", aviso != null);
        Assercao.verdadeiro("L07: aviso cita a VRAM e o minimo",
                aviso.getConsequencia().contains("8 GB")
                        && aviso.getConsequencia().contains("12 GB"));
    }

    private static void gargaloDeCpu() {
        Processador cpu = cpu(140, 1.25, 60);
        PlacaMae placa = placa(6000, 12, 60, true, 4, 16);
        PlacaVideo gpu = gpu(12, 4, 16, 220, 350, Resolucao.QHD_1440P, 90, 0);

        Limitacao aviso = aviso(AVALIADOR.avaliar(montagem(cpu, placa), gpu), "L01");
        Assercao.verdadeiro("L01: CPU mais fraca que a GPU gera aviso", aviso != null);
        Assercao.verdadeiro("L01: consequencia fala de gargalo de CPU",
                aviso.getConsequencia().contains("gargalo de CPU"));
        Assercao.iguais("L01: perda estimada de ~33%", 33L,
                Math.round(aviso.getPercentualPerda()));

        Processador equilibrada = cpu(140, 1.25, 90);
        Assercao.verdadeiro("L01: CPU a frente da GPU nao gera aviso",
                AVALIADOR.avaliar(montagem(equilibrada, placa), gpu).stream()
                        .noneMatch(limitacao -> "L01".equals(limitacao.getRegraId())));
    }

    // ------------------------------------------------------ CLI e acumulo (HU-004)

    private static void telaMarcaItensComAviso() {
        Processador cpu = cpu(140, 1.25, 90);
        PlacaMae placa = placa(6000, 12, 60, true, 4, 16);
        Montagem atual = montagem(cpu, placa);
        Memoria lenta = memoria("Vengeance 6000", 6000, 30, 16, 2);
        Memoria rapida = memoria("Vengeance 8000", 8000, 38, 16, 2);

        List<Componente> candidatos = List.of(lenta, rapida);
        Map<Componente, List<Limitacao>> avisos = AVALIADOR.avaliarLista(atual, candidatos);
        Assercao.iguais("tela: so a memoria acima do teto tem aviso", 1, avisos.size());
        Assercao.verdadeiro("tela: a memoria de 8000 e a marcada", avisos.containsKey(rapida));

        ListaFiltrada lista = new ListaFiltrada(CategoriaComponente.MEMORIA, candidatos, List.of());
        Saida saida = new Saida();
        TelaEtapa tela = new TelaEtapa(saida.terminal);
        tela.imprimirListaFiltrada(lista, null,
                componente -> avisos.getOrDefault(componente, List.of()));

        String texto = saida.texto();
        Assercao.verdadeiro("tela: linha da memoria de 8000 comeca com [!]",
                linhaCom(texto, "Vengeance 8000").startsWith(TabelaComponentes.MARCADOR_LIMITACAO));
        Assercao.falso("tela: linha da memoria de 6000 nao tem marcador",
                linhaCom(texto, "Vengeance 6000").contains(TabelaComponentes.MARCADOR_LIMITACAO));
        Assercao.verdadeiro("tela: rodape conta os itens com aviso",
                texto.contains("1 com aviso [!]"));
        Assercao.verdadeiro("tela: rodape anuncia [L] Avisos",
                texto.contains(TelaEtapa.RODAPE_AVISOS));
        Assercao.iguais("tela: apenas uma linha marcada", 1, linhasMarcadas(texto));

        Saida selecao = new Saida();
        TelaEtapa telaSelecao = new TelaEtapa(selecao.terminal);
        telaSelecao.imprimirAvisoSelecao(avisos.get(rapida));
        String textoSelecao = selecao.texto();
        Assercao.verdadeiro("tela: bloco nomeia campo e valores",
                textoSelecao.contains("frequenciaMTs: 8000 MT/s > 6000 MT/s"));
        Assercao.verdadeiro("tela: bloco repete que a escolha vale",
                textoSelecao.contains(TextosLimitacao.ESCOLHA_VALIDA));
        Assercao.verdadeiro("tela: espera [Enter] para continuar",
                textoSelecao.contains(TelaEtapa.MENSAGEM_CONTINUAR));
    }

    private static void acumuloEAvisosDaMontagem() {
        Processador cpu = cpu(140, 1.25, 90);
        PlacaMae placa = placa(6000, 12, 60, true, 4, 16);
        Memoria rapida = memoria("Vengeance 8000", 8000, 38, 16, 2);
        Montagem atual = montagem(cpu, placa);
        List<Limitacao> avisos = AVALIADOR.avaliar(atual, rapida);

        atual.registrarLimitacoes(avisos);
        Assercao.iguais("montagem: avisos acumulados", 1, atual.getLimitacoes().size());
        atual.limparLimitacoesDe(CategoriaComponente.MEMORIA);
        Assercao.iguais("montagem: trocar a memoria descarta o aviso", 0,
                atual.getLimitacoes().size());

        atual.registrarLimitacoes(avisos);
        Saida saida = new Saida();
        new TelaEtapa(saida.terminal).imprimirAvisosDaMontagem(atual);
        Assercao.verdadeiro("tela: [L] lista os avisos acumulados",
                saida.texto().contains("Avisos acumulados (1)"));
        Assercao.verdadeiro("tela: [L] mostra o detalhe do aviso",
                saida.texto().contains("6000 MT/s"));

        Saida semAvisos = new Saida();
        new TelaEtapa(semAvisos.terminal).imprimirAvisosDaMontagem(new Montagem());
        Assercao.verdadeiro("tela: [L] sem avisos mostra a mensagem",
                semAvisos.texto().contains(TelaEtapa.MENSAGEM_SEM_AVISOS));

        atual.limpar();
        Assercao.iguais("montagem: limpar descarta os avisos", 0,
                atual.getLimitacoes().size());
    }

    private static void tabelaComMarcador() {
        String marcada = TabelaComponentes.linha(true, 1, "X", "R$ 1,00", "");
        Assercao.verdadeiro("tabela: marcador [!] antes do numero",
                marcada.startsWith("[!]   1)"));
        String normal = TabelaComponentes.linha(1, "X", "R$ 1,00", "");
        Assercao.verdadeiro("tabela: item sem aviso mantem o formato antigo",
                normal.startsWith("      1)"));
        Assercao.iguais("tabela: marcador nao desloca o numero",
                normal.indexOf("1)"), marcada.indexOf("1)"));
        Assercao.iguais("tabela: marcador publico", "[!]", TabelaComponentes.MARCADOR_LIMITACAO);
    }

    private static void comandoLAvisosNaEtapa1() {
        Processador cpu = cpu(140, 1.25, 90);
        Saida saida = new Saida("L\n1\n");
        MontagemGuiada fluxo = new MontagemGuiada();

        ResultadoEtapa resultado = new EtapaProcessador(saida.terminal, fluxo,
                List.of(cpu)).executar();

        Assercao.iguais("CLI: [L] depois escolhe a CPU", ResultadoEtapa.CONCLUIDA, resultado);
        Assercao.verdadeiro("CLI: [L] nao escolhe peca sozinho",
                saida.texto().contains(TelaEtapa.MENSAGEM_SEM_AVISOS));
        Assercao.iguais("CLI: a CPU foi guardada depois do [L]", cpu,
                fluxo.getMontagem().obter(CategoriaComponente.CPU).orElse(null));
        Assercao.verdadeiro("CLI: nenhum aviso acumulado na etapa 1",
                fluxo.getMontagem().getLimitacoes().isEmpty());
    }

    // ---------------------------------------------------------------- auxiliares

    private static Processador cpu(int consumoMaximo, double tensao, int indiceSingle) {
        return Processador.builder()
                .id("cpu-teste-" + consumoMaximo + "-" + indiceSingle)
                .modelo("Ryzen Teste " + indiceSingle)
                .fabricante("AMD")
                .soquete(Soquete.AM5)
                .nucleosFisicos(8)
                .threads(16)
                .frequenciaBaseMHz(3800)
                .frequenciaBoostMHz(5500)
                .tdpWatts(65)
                .consumoMaximoWatts(consumoMaximo)
                .tensaoMaximaVolts(tensao)
                .tiposMemoriaSuportados(TipoMemoria.DDR5)
                .canaisMemoria(2)
                .frequenciaMemoriaNativaMTs(5200)
                .frequenciaMaximaSincronaMTs(6000)
                .capacidadeMaximaMemoriaGB(192)
                .suportaECC(true)
                .versaoPCIeMaxima(5)
                .pistasPCIeLivres(24)
                .possuiVideoIntegrado(true)
                .desbloqueadoOverclock(true)
                .indiceDesempenhoSingleThread(indiceSingle)
                .indiceDesempenhoMultithread(indiceSingle)
                .construir();
    }

    private static PlacaMae placa(int frequenciaMaxima, int fases, int correntePorFase,
            Boolean dissipador, int versaoSlot, int pistasSlot) {
        return PlacaMae.builder()
                .id("placa-teste-" + frequenciaMaxima + "-" + fases + "-" + versaoSlot)
                .modelo("B650 Teste")
                .fabricante("ASUS")
                .soqueteCPU(Soquete.AM5)
                .tipoMemoria(TipoMemoria.DDR5)
                .quantidadeSlotsMemoria(4)
                .canaisMemoria(2)
                .capacidadeMaximaMemoriaGB(192)
                .frequenciaMemoriaMaximaMTs(frequenciaMaxima)
                .suportaPerfilEXPO(true)
                .suportaPerfilXMP(true)
                .vrmFasesCPU(fases)
                .vrmCorrentePorFaseA(correntePorFase)
                .vrmPossuiDissipador(dissipador)
                .quantidadeSlotsM2(2)
                .geracaoMaximaSlotM2(4)
                .geracaoMinimaSlotM2(4)
                .quantidadeSlotsPCIe16(1)
                .versaoSlotPCIe16(versaoSlot)
                .pistasSlotPCIe16(pistasSlot)
                .quantidadePortasSATA(4)
                .velocidadeSATAGbps(6)
                .quantidadeConectoresCPU8pin(1)
                .consumoEstimadoWatts(30)
                .construir();
    }

    private static Memoria memoria(String modelo, int frequencia, int latencia, int porPente,
            int pentes) {
        return Memoria.builder()
                .id("ram-teste-" + modelo.replace(' ', '-').toLowerCase())
                .modelo(modelo)
                .fabricante("CORSAIR")
                .tipo(TipoMemoria.DDR5)
                .capacidadePorPenteGB(porPente)
                .quantidadePentes(pentes)
                .frequenciaMTs(frequencia)
                .frequenciaJEDECBaseMTs(4800)
                .latenciaCAS(latencia)
                .tensaoVolts(1.35)
                .perfilEXPO(true)
                .consumoEstimadoWatts(6)
                .construir();
    }

    private static PlacaVideo gpu(int vramGB, int versaoPcie, int pistas, int tdp,
            Integer picoTransiente, Resolucao resolucao, int indice, int conectores16pin) {
        return PlacaVideo.builder()
                .id("gpu-teste-" + vramGB + "-" + versaoPcie + "-" + indice)
                .modelo("RTX Teste " + indice)
                .fabricante("NVIDIA")
                .memoriaVRAMGB(vramGB)
                .versaoPCIe(versaoPcie)
                .pistasPCIe(pistas)
                .tdpWatts(tdp)
                .picoTransienteWatts(picoTransiente)
                .resolucaoAlvo(resolucao)
                .indiceDesempenhoGPU(indice)
                .quantidadeConectores16pin(conectores16pin)
                .construir();
    }

    private static Fonte fonte(int potencia, int trilho, int conectores16pin,
            int conectores8pin) {
        return Fonte.builder()
                .id("fonte-teste-" + potencia)
                .modelo("Fonte Teste " + potencia + " W")
                .fabricante("Corsair")
                .potenciaTotalWatts(potencia)
                .potenciaTrilho12VWatts(trilho)
                .quantidadeConectores16pin12VHPWR(conectores16pin)
                .quantidadeConectoresPCIe8pin(conectores8pin)
                .quantidadeConectoresCPU8pin(1)
                .construir();
    }

    private static Armazenamento ssd(int versaoPcie, int leituraMBs) {
        return Armazenamento.builder()
                .id("ssd-teste-" + leituraMBs)
                .modelo("SSD Teste " + leituraMBs)
                .fabricante("Kingston")
                .interfaceArmazenamento(InterfaceArmazenamento.NVME)
                .capacidadeGB(2000)
                .versaoPCIe(versaoPcie)
                .pistasPCIe(4)
                .leituraSequencialMBs(leituraMBs)
                .consumoEstimadoWatts(5)
                .construir();
    }

    private static Montagem montagem(Componente... pecas) {
        Montagem montagem = new Montagem();
        for (Componente peca : pecas) {
            montagem.adicionar(peca);
        }
        return montagem;
    }

    /** Primeiro aviso da regra informada; falha explicita quando a regra nao gerou nada. */
    private static Limitacao aviso(List<Limitacao> avisos, String regraId) {
        return avisos.stream()
                .filter(aviso -> regraId.equals(aviso.getRegraId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("A regra " + regraId
                        + " nao gerou aviso. Avisos: " + avisos));
    }

    private static String linhaCom(String texto, String trecho) {
        for (String linha : texto.split("\\R")) {
            if (linha.contains(trecho)) {
                return linha;
            }
        }
        return "";
    }

    private static int linhasMarcadas(String texto) {
        int total = 0;
        for (String linha : texto.split("\\R")) {
            if (linha.startsWith(TabelaComponentes.MARCADOR_LIMITACAO)) {
                total++;
            }
        }
        return total;
    }

    /** Terminal com a entrada digitada e a saida capturada em memoria. */
    private static final class Saida {

        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final Terminal terminal;

        Saida() {
            this("");
        }

        Saida(String entrada) {
            PrintStream stream = new PrintStream(bytes, true, StandardCharsets.UTF_8);
            this.terminal = new Terminal(new StringReader(entrada), stream);
        }

        String texto() {
            return bytes.toString(StandardCharsets.UTF_8);
        }
    }
}
