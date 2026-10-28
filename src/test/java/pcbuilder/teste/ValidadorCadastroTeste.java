package pcbuilder.teste;

import java.util.List;

import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.FormatoArmazenamento;
import pcbuilder.enums.InterfaceArmazenamento;
import pcbuilder.enums.Soquete;
import pcbuilder.enums.TipoMemoria;
import pcbuilder.enums.TipoMidia;
import pcbuilder.mock.CatalogoMock;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Memoria;
import pcbuilder.modelo.PlacaMae;
import pcbuilder.modelo.PlacaVideo;
import pcbuilder.modelo.Processador;
import pcbuilder.servico.ProblemaCadastro;
import pcbuilder.servico.ResultadoValidacao;
import pcbuilder.servico.ValidadorCadastro;

/**
 * Testes da HU-001: 1 caso por verificacao do checklist, id duplicado e contagem de
 * itens por categoria.
 *
 * <p>Execucao (sem JUnit): {@code java -cp build/classes;build/classes-test pcbuilder.teste.ValidadorCadastroTeste}.</p>
 */
public final class ValidadorCadastroTeste {

    private static final ValidadorCadastro VALIDADOR = new ValidadorCadastro();

    /** Categorias que o fluxo guiado usa (secao 1.3 do modelo de dados). */
    private static final List<CategoriaComponente> CATEGORIAS_ATIVAS = List.of(
            CategoriaComponente.CPU,
            CategoriaComponente.PLACA_MAE,
            CategoriaComponente.MEMORIA,
            CategoriaComponente.PLACA_VIDEO,
            CategoriaComponente.ARMAZENAMENTO,
            CategoriaComponente.FONTE);

    private ValidadorCadastroTeste() {
        // classe utilitaria: nao deve ser instanciada
    }

    public static void main(String[] args) {
        System.out.println("Testes da HU-001 - validacao da base mockada");

        verificacao1CamposObrigatorios();
        verificacao2NumeroComTexto();
        verificacao3UnidadeDuplicada();
        verificacao4DominioDosEnums();
        verificacao5ValoresPositivos();
        verificacao6AplicabilidadeDoNull();
        verificacao7CoerenciaInterna();
        verificacao8IdUnico();
        verificacao9CampoDerivado();
        verificacao10CampoGenerico();
        contagemPorCategoria();
        paresIncompativeis();
        resumoDaCarga();
        registroInvalidoEhDescartado();

        System.exit(Assercao.concluir());
    }

    /** CPU que passa em todas as verificacoes; cada teste altera so o que precisa. */
    private static Processador.Builder cpuValida() {
        return Processador.builder()
                .id("cpu-teste")
                .modelo("CPU de teste")
                .fabricante("Teste")
                .precoMedioBRL("1000.00")
                .soquete(Soquete.AM5)
                .nucleosFisicos(8)
                .threads(16)
                .frequenciaBaseMHz(3800)
                .frequenciaBoostMHz(5500)
                .tdpWatts(65)
                .consumoMaximoWatts(140)
                .tensaoMaximaVolts(1.25)
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
                .indiceDesempenhoSingleThread(90)
                .indiceDesempenhoMultithread(85);
    }

    /** SSD NVMe valido; os testes de aplicabilidade trocam apenas os campos condicionais. */
    private static Armazenamento.Builder nvmeValido() {
        return Armazenamento.builder()
                .id("ssd-teste")
                .modelo("SSD de teste")
                .fabricante("Teste")
                .precoMedioBRL("500.00")
                .tipoMidia(TipoMidia.SSD)
                .interfaceArmazenamento(InterfaceArmazenamento.NVME)
                .formato(FormatoArmazenamento.M2_2280)
                .capacidadeGB(1000)
                .versaoPCIe(4)
                .pistasPCIe(4)
                .leituraSequencialMBs(7000)
                .escritaSequencialMBs(6000)
                .consumoMedioWatts(5.0)
                .consumoPicoWatts(8.0);
    }

    private static boolean possuiProblema(List<ProblemaCadastro> problemas, int verificacao) {
        for (ProblemaCadastro problema : problemas) {
            if (problema.getVerificacao() == verificacao) {
                return true;
            }
        }
        return false;
    }

    private static boolean possuiErro(List<ProblemaCadastro> problemas, int verificacao) {
        for (ProblemaCadastro problema : problemas) {
            if (problema.getVerificacao() == verificacao && problema.isErro()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------- casos do checklist

    /** Verificacao 1: campo obrigatorio ausente ou vazio descarta o registro. */
    private static void verificacao1CamposObrigatorios() {
        List<ProblemaCadastro> semPreco = VALIDADOR.avaliar(cpuValida().precoMedioBRL((String) null).construir());
        Assercao.verdadeiro("1: CPU sem precoMedioBRL gera ERRO 1",
                possuiErro(semPreco, ValidadorCadastro.OBRIGATORIOS));

        List<ProblemaCadastro> modeloVazio = VALIDADOR.avaliar(cpuValida().modelo("   ").construir());
        Assercao.verdadeiro("1: modelo so com espacos gera ERRO 1",
                possuiErro(modeloVazio, ValidadorCadastro.OBRIGATORIOS));

        List<ProblemaCadastro> completa = VALIDADOR.avaliar(cpuValida().construir());
        Assercao.falso("1: CPU completa nao gera ERRO 1",
                possuiErro(completa, ValidadorCadastro.OBRIGATORIOS));
    }

    /** Verificacao 2: campo numerico recebe texto ("2 pentes" em quantidadePentes). */
    private static void verificacao2NumeroComTexto() {
        List<ProblemaCadastro> problemas = VALIDADOR.avaliar(
                ComponenteFake.cpuValida().campo("threads", "16 threads").construir());
        Assercao.verdadeiro("2: '16 threads' em threads gera ERRO 2",
                possuiErro(problemas, ValidadorCadastro.NUMERO_COM_TEXTO));
    }

    /** Verificacao 3: o valor repete a unidade que ja esta no nome do campo. */
    private static void verificacao3UnidadeDuplicada() {
        List<ProblemaCadastro> problemas = VALIDADOR.avaliar(
                ComponenteFake.cpuValida().campo("tdpWatts", "65W").construir());
        Assercao.verdadeiro("3: '65W' em tdpWatts gera ERRO 3",
                possuiErro(problemas, ValidadorCadastro.UNIDADE_DUPLICADA));
    }

    /** Verificacao 4: valor fora do dominio do enum (inclusive com espaco sobrando). */
    private static void verificacao4DominioDosEnums() {
        List<ProblemaCadastro> foraDoDominio = VALIDADOR.avaliar(
                ComponenteFake.cpuValida().campo("soquete", "AM5 ").construir());
        Assercao.verdadeiro("4: soquete 'AM5 ' (com espaco) gera ERRO 4",
                possuiErro(foraDoDominio, ValidadorCadastro.DOMINIO_ENUM));

        List<ProblemaCadastro> textoExato = VALIDADOR.avaliar(
                ComponenteFake.cpuValida().campo("soquete", "AM5").construir());
        Assercao.falso("4: 'AM5' identico ao enum nao e problema de dominio",
                possuiErro(textoExato, ValidadorCadastro.DOMINIO_ENUM));
    }

    /** Verificacao 5: zero/negativo em campo que so aceita valor positivo. */
    private static void verificacao5ValoresPositivos() {
        List<ProblemaCadastro> problemas = VALIDADOR.avaliar(cpuValida().nucleosFisicos(0).construir());
        Assercao.verdadeiro("5: nucleosFisicos = 0 gera ERRO 5",
                possuiErro(problemas, ValidadorCadastro.VALORES_POSITIVOS));
    }

    /** Verificacao 6: null so onde o campo nao se aplica (NVMe x SATA). */
    private static void verificacao6AplicabilidadeDoNull() {
        List<ProblemaCadastro> nvmeIncompleto = VALIDADOR.avaliar(nvmeValido().versaoPCIe(null).construir());
        Assercao.verdadeiro("6: NVMe sem versaoPCIe gera ERRO 6",
                possuiErro(nvmeIncompleto, ValidadorCadastro.APLICABILIDADE_NULL));

        List<ProblemaCadastro> nvmeCompleto = VALIDADOR.avaliar(nvmeValido().construir());
        Assercao.falso("6: NVMe completo nao gera problema 6",
                possuiProblema(nvmeCompleto, ValidadorCadastro.APLICABILIDADE_NULL));
    }

    /** Verificacao 7: relacoes internas do registro (base > boost, trilho 12 V etc.). */
    private static void verificacao7CoerenciaInterna() {
        List<ProblemaCadastro> cpuIncoerente = VALIDADOR.avaliar(
                cpuValida().frequenciaBaseMHz(6000).frequenciaBoostMHz(3800).construir());
        Assercao.verdadeiro("7: frequencia base maior que o boost gera ERRO 7",
                possuiErro(cpuIncoerente, ValidadorCadastro.COERENCIA));

        List<ProblemaCadastro> cpuCoerente = VALIDADOR.avaliar(cpuValida().construir());
        Assercao.falso("7: CPU coerente nao gera problema 7",
                possuiProblema(cpuCoerente, ValidadorCadastro.COERENCIA));
    }

    /** Verificacao 8: dois registros com o mesmo id - o segundo e descartado. */
    private static void verificacao8IdUnico() {
        Componente primeiro = cpuValida().construir();
        Componente repetido = cpuValida().modelo("Outra CPU com o mesmo id").construir();

        ResultadoValidacao resultado = VALIDADOR.validar(List.of(primeiro, repetido));
        Assercao.verdadeiro("8: id duplicado gera ERRO 8",
                possuiErro(resultado.getProblemas(), ValidadorCadastro.ID_UNICO));
        Assercao.iguais("8: apenas o primeiro registro com o id permanece", 1, resultado.getTotalValidos());
    }

    /** Verificacao 9: campo calculado gravado no cadastro. */
    private static void verificacao9CampoDerivado() {
        List<ProblemaCadastro> problemas = VALIDADOR.avaliar(
                ComponenteFake.cpuValida().campo("consumoTotalSistemaWatts", 410).construir());
        Assercao.verdadeiro("9: consumoTotalSistemaWatts gravado gera ERRO 9",
                possuiErro(problemas, ValidadorCadastro.CAMPO_DERIVADO));
    }

    /** Verificacao 10: campo curinga escondendo varias informacoes. */
    private static void verificacao10CampoGenerico() {
        List<ProblemaCadastro> problemas = VALIDADOR.avaliar(
                ComponenteFake.cpuValida().campo("especificacoes", "8 nucleos, AM5, 65W").construir());
        Assercao.verdadeiro("10: campo 'especificacoes' gera ERRO 10",
                possuiErro(problemas, ValidadorCadastro.CAMPO_GENERICO));
    }

    // ------------------------------------------------------------ base mockada

    /** Minimo de 3 itens por categoria ativa e um catalogo grande vindo do OpenDB. */
    private static void contagemPorCategoria() {
        CatalogoMock catalogo = CatalogoMock.criar();

        Assercao.verdadeiro("contagem: base com pelo menos 1000 componentes (tem "
                + catalogo.quantidadeTotal() + ")", catalogo.quantidadeTotal() >= 1000);

        int soma = 0;
        for (CategoriaComponente categoria : CATEGORIAS_ATIVAS) {
            int quantidade = catalogo.porCategoria(categoria).size();
            soma += quantidade;
            Assercao.verdadeiro("contagem: pelo menos 3 itens em " + categoria
                    + " (tem " + quantidade + ")", quantidade >= 3);
        }
        Assercao.iguais("contagem: soma das categorias = total do catalogo",
                catalogo.quantidadeTotal(), soma);

        int processadores = catalogo.porCategoria(CategoriaComponente.CPU).size();
        Assercao.verdadeiro("contagem: catalogo com muitos processadores (tem "
                + processadores + ")", processadores >= 100);
    }

    /** A base precisa manter os pares incompativeis que exercitam o filtro (HU-003). */
    private static void paresIncompativeis() {
        CatalogoMock catalogo = CatalogoMock.criar();

        boolean cpuAm5 = catalogo.porCategoria(CategoriaComponente.CPU).stream()
                .anyMatch(c -> ((Processador) c).getSoquete() == Soquete.AM5);
        boolean placaLga1700 = catalogo.porCategoria(CategoriaComponente.PLACA_MAE).stream()
                .anyMatch(c -> ((PlacaMae) c).getSoqueteCPU() == Soquete.LGA1700);
        Assercao.verdadeiro("pares: CPU AM5 x placa LGA1700 disponiveis", cpuAm5 && placaLga1700);

        boolean placaDdr5 = catalogo.porCategoria(CategoriaComponente.PLACA_MAE).stream()
                .anyMatch(c -> ((PlacaMae) c).getTipoMemoria() == TipoMemoria.DDR5);
        boolean memoriaDdr4 = catalogo.porCategoria(CategoriaComponente.MEMORIA).stream()
                .anyMatch(c -> ((Memoria) c).getTipo() == TipoMemoria.DDR4);
        Assercao.verdadeiro("pares: placa DDR5 x memoria DDR4 disponiveis", placaDdr5 && memoriaDdr4);

        boolean gpuCom16Pin = catalogo.porCategoria(CategoriaComponente.PLACA_VIDEO).stream()
                .anyMatch(c -> ((PlacaVideo) c).getQuantidadeConectores16pin() > 0);
        boolean fonteSem16Pin = catalogo.porCategoria(CategoriaComponente.FONTE).stream()
                .anyMatch(c -> ((Fonte) c).getQuantidadeConectores16pin12VHPWR() == 0);
        Assercao.verdadeiro("pares: GPU com 16 pinos x fonte sem 16 pinos",
                gpuCom16Pin && fonteSem16Pin);
    }

    /** Mensagem exigida pela HU-001, no formato {@code Base carregada: N, X erro(s), Y aviso(s).} */
    private static void resumoDaCarga() {
        ResultadoValidacao resultado = CatalogoMock.criar().getResultadoValidacao();

        Assercao.iguais("resumo: nenhum ERRO na base", 0, resultado.getTotalErros());
        Assercao.iguais("resumo: nenhum AVISO na base", 0, resultado.getTotalAvisos());
        Assercao.iguais("resumo: todos os registros entram no catalogo",
                resultado.getTotalRegistros(), resultado.getTotalValidos());
        Assercao.iguais("resumo: linha da carga",
                "Base carregada: " + resultado.getTotalRegistros()
                        + " componentes, 0 erro(s), 0 aviso(s).",
                resultado.resumo());
    }

    /** Registro com ERRO sai da carga, mas continua contado no total avaliado. */
    private static void registroInvalidoEhDescartado() {
        Componente valido = cpuValida().construir();
        Componente invalido = cpuValida().id("cpu-invalida").threads(0).construir();

        ResultadoValidacao resultado = VALIDADOR.validar(List.of(valido, invalido));
        Assercao.iguais("descarte: so o registro limpo entra no catalogo", 1,
                resultado.getRegistrosValidos().size());
        Assercao.iguais("descarte: o total avaliado inclui o registro descartado", 2,
                resultado.getTotalRegistros());
    }
}
