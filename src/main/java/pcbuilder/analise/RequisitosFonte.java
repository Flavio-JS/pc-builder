package pcbuilder.analise;

import java.util.ArrayList;
import java.util.List;

import pcbuilder.modelo.Fonte;
import pcbuilder.modelo.Montagem;

/**
 * Requisitos que a fonte precisa atender (HU-009): potencia minima e conectores.
 *
 * <p>E a unica fonte dos numeros exibidos no cabecalho da etapa 6 **e** usados pelo
 * filtro C10 - assim a tela e o filtro nunca divergem. A potencia vem de
 * {@link CalculadoraConsumo#consumoComMargemWatts(Montagem)}; os conectores saem de
 * {@code gpu.quantidadeConectores16pin/8pin/6pin} e
 * {@code placaMae.quantidadeConectoresCPU8pin}.</p>
 *
 * <p>Quando a etapa 4 foi pulada (CPU com video integrado), a montagem nao tem GPU e
 * so o conector de 8 pinos da placa-mae e exigido.</p>
 */
public final class RequisitosFonte {

    /** Tipos de conector de energia que a fonte pode precisar fornecer. */
    public enum TipoConector {

        PCIE_16("16-pinos", "16 pinos", "placa de video"),
        PCIE_8("8-pinos", "8 pinos", "placa de video"),
        PCIE_6("6-pinos", "6 pinos", "placa de video"),
        CPU_8("8-pinos", "8 pinos para a CPU", "placa-mae");

        private final String rotulo;
        private final String rotuloFiltro;
        private final String origem;

        TipoConector(String rotulo, String rotuloFiltro, String origem) {
            this.rotulo = rotulo;
            this.rotuloFiltro = rotuloFiltro;
            this.origem = origem;
        }

        /** Rotulo para o cabecalho ({@code 16-pinos}, {@code 8-pinos}...). */
        public String getRotulo() {
            return rotulo;
        }

        /** Rotulo usado no motivo do filtro ({@code 16 pinos}, {@code 8 pinos para a CPU}). */
        public String getRotuloFiltro() {
            return rotuloFiltro;
        }

        /** Peca que exige o conector ({@code placa de video} / {@code placa-mae}). */
        public String getOrigem() {
            return origem;
        }
    }

    /** Um conector exigido: tipo + quantidade. */
    public static final class Requisito {

        private final TipoConector tipo;
        private final int quantidade;

        public Requisito(TipoConector tipo, int quantidade) {
            this.tipo = tipo;
            this.quantidade = quantidade;
        }

        public TipoConector getTipo() {
            return tipo;
        }

        public int getQuantidade() {
            return quantidade;
        }
    }

    private RequisitosFonte() {
        // classe utilitaria: nao deve ser instanciada
    }

    /** Potencia minima da fonte, ja com a margem de seguranca. */
    public static int potenciaNecessariaWatts(Montagem montagem) {
        return CalculadoraConsumo.consumoComMargemWatts(montagem);
    }

    /**
     * Conectores exigidos pela configuracao, na ordem GPU 16/8/6 e CPU 8.
     *
     * @param montagem pecas escolhidas
     * @return lista vazia quando nao ha GPU nem placa-mae
     */
    public static List<Requisito> conectores(Montagem montagem) {
        List<Requisito> requisitos = new ArrayList<>();
        ComponentesDaMontagem.placaVideo(montagem).ifPresent(gpu -> {
            adicionar(requisitos, TipoConector.PCIE_16, gpu.getQuantidadeConectores16pin());
            adicionar(requisitos, TipoConector.PCIE_8, gpu.getQuantidadeConectores8pin());
            adicionar(requisitos, TipoConector.PCIE_6, gpu.getQuantidadeConectores6pin());
        });
        ComponentesDaMontagem.placaMae(montagem).ifPresent(placa -> adicionar(requisitos,
                TipoConector.CPU_8, placa.getQuantidadeConectoresCPU8pin()));
        return List.copyOf(requisitos);
    }

    /**
     * Quantos conectores do tipo a fonte oferece.
     *
     * @param fonte fonte candidata
     * @param tipo tipo do conector
     * @return a quantidade disponivel
     */
    public static int disponivel(Fonte fonte, TipoConector tipo) {
        switch (tipo) {
            case PCIE_16:
                return fonte.getQuantidadeConectores16pin12VHPWR();
            case PCIE_8:
                return fonte.getQuantidadeConectoresPCIe8pin();
            case PCIE_6:
                return fonte.getQuantidadeConectoresPCIe6pin();
            case CPU_8:
                return fonte.getQuantidadeConectoresCPU8pin();
            default:
                return 0;
        }
    }

    /**
     * Descricao para o cabecalho da etapa:
     * {@code 1x 16-pinos (placa de video) + 1x 8-pinos (placa-mae)}.
     *
     * @param montagem pecas escolhidas
     * @return a descricao, ou vazio quando nada e exigido
     */
    public static String descricaoConectores(Montagem montagem) {
        List<String> partes = new ArrayList<>();
        for (Requisito requisito : conectores(montagem)) {
            partes.add(requisito.getQuantidade() + "x " + requisito.getTipo().getRotulo()
                    + " (" + requisito.getTipo().getOrigem() + ")");
        }
        return String.join(" + ", partes);
    }

    private static void adicionar(List<Requisito> requisitos, TipoConector tipo, int quantidade) {
        if (quantidade > 0) {
            requisitos.add(new Requisito(tipo, quantidade));
        }
    }
}
