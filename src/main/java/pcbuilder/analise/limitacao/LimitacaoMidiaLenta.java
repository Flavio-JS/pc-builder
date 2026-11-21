package pcbuilder.analise.limitacao;

import java.util.List;

import pcbuilder.enums.InterfaceArmazenamento;
import pcbuilder.enums.SeveridadeLimitacao;
import pcbuilder.enums.TipoMidia;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;

/**
 * Alerta <strong>L10</strong> (informativo) - midia lenta em relacao ao SSD NVMe.
 *
 * <p>Um HD mecanico ou um SSD SATA encaixa e funciona, mas e bem mais lento que um SSD
 * NVMe. O aviso existe para o usuario leigo saber a diferenca antes de escolher - nao
 * bloqueia nada e nao altera o filtro C04.</p>
 *
 * <p>Compartilha o id {@code L10} com a perda de banda do slot; o
 * {@code campoComparado} distingue os dois ({@code tipoMidia} x
 * {@code leituraSequencialMBs}).</p>
 */
public final class LimitacaoMidiaLenta implements RegraLimitacao {

    @Override
    public String id() {
        return "L10";
    }

    @Override
    public List<Limitacao> avaliar(Montagem montagem, Componente candidato) {
        if (!(candidato instanceof Armazenamento armazenamento)) {
            return List.of();
        }
        boolean hdd = armazenamento.getTipoMidia() == TipoMidia.HDD;
        boolean sata = armazenamento.getInterfaceArmazenamento() == InterfaceArmazenamento.SATA;
        if (!hdd && !sata) {
            return List.of();
        }
        return List.of(Limitacao.builder()
                .regra(id())
                .limitante(armazenamento)
                .afetado(armazenamento)
                .campo("tipoMidia")
                .valorDoNovo(descricao(armazenamento))
                .valorSuportado("SSD NVMe")
                .consequencia(TextosLimitacao.midiaLenta(armazenamento))
                .severidade(SeveridadeLimitacao.AVISO)
                .construir());
    }

    private static String descricao(Armazenamento armazenamento) {
        String tipo = armazenamento.getTipoMidia() == TipoMidia.HDD ? "HD" : "SSD";
        String conexao = armazenamento.getInterfaceArmazenamento()
                == InterfaceArmazenamento.NVME ? "NVMe" : "SATA";
        return tipo + " " + conexao;
    }
}
