package pcbuilder.analise.limitacao;

import java.util.List;
import java.util.Optional;

import pcbuilder.analise.CalculadoraPcie;
import pcbuilder.analise.ComponentesDaMontagem;
import pcbuilder.enums.CategoriaComponente;
import pcbuilder.enums.InterfaceArmazenamento;
import pcbuilder.enums.SeveridadeLimitacao;
import pcbuilder.modelo.Armazenamento;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Limitacao;
import pcbuilder.modelo.Montagem;
import pcbuilder.modelo.PlacaMae;

/**
 * Regra <strong>L10</strong> - armazenamento x geracao do slot/porta.
 *
 * <p>Um SSD NVMe PCIe 4.0 em slot M.2 3.0 funciona, mas entrega cerca de metade da
 * velocidade; um SSD SATA mais rapido que a porta tambem perde. E aviso, nunca
 * bloqueio - o bloqueio de "sem slot nenhum" e a C04 (HU-003).</p>
 */
public final class LimitacaoVelocidadeArmazenamento implements RegraLimitacao {

    @Override
    public String id() {
        return "L10";
    }

    @Override
    public List<Limitacao> avaliar(Montagem montagem, Componente candidato) {
        CategoriaComponente categoria = candidato.getCategoria();
        if (categoria != CategoriaComponente.ARMAZENAMENTO
                && categoria != CategoriaComponente.PLACA_MAE) {
            return List.of();
        }
        Optional<Armazenamento> armazenamento = ComponentesDaMontagem.armazenamento(montagem);
        Optional<PlacaMae> placaMae = ComponentesDaMontagem.placaMae(montagem);
        if (armazenamento.isEmpty() || placaMae.isEmpty()) {
            return List.of();
        }
        if (armazenamento.get().getInterfaceArmazenamento() == InterfaceArmazenamento.SATA) {
            return avaliarSata(armazenamento.get(), placaMae.get());
        }
        return avaliarNvme(armazenamento.get(), placaMae.get());
    }

    private static List<Limitacao> avaliarNvme(Armazenamento armazenamento, PlacaMae placaMae) {
        if (armazenamento.getVersaoPCIe() == null || armazenamento.getVersaoPCIe() <= 0) {
            return List.of();
        }
        int geracaoSlot = placaMae.getGeracaoMaximaSlotM2();
        if (geracaoSlot <= 0 || armazenamento.getVersaoPCIe() <= geracaoSlot) {
            return List.of();
        }
        int pistas = armazenamento.getPistasPCIe() != null && armazenamento.getPistasPCIe() > 0
                ? armazenamento.getPistasPCIe()
                : 4;
        int tetoSlotMBs = CalculadoraPcie.tetoSlotM2MBs(geracaoSlot, pistas);
        int leituraMBs = armazenamento.getLeituraSequencialMBs();
        if (tetoSlotMBs <= 0 || leituraMBs <= tetoSlotMBs) {
            return List.of();
        }
        return limitacao(placaMae, armazenamento, "leituraSequencialMBs",
                leituraMBs + " MB/s", tetoSlotMBs + " MB/s", leituraMBs, tetoSlotMBs);
    }

    private static List<Limitacao> avaliarSata(Armazenamento armazenamento, PlacaMae placaMae) {
        Integer velocidadeMidia = armazenamento.getVelocidadeSATAGbps();
        int velocidadePorta = placaMae.getVelocidadeSATAGbps();
        if (velocidadeMidia == null || velocidadeMidia <= 0 || velocidadePorta <= 0
                || velocidadeMidia <= velocidadePorta) {
            return List.of();
        }
        return limitacao(placaMae, armazenamento, "velocidadeSATAGbps",
                velocidadeMidia + " Gb/s", velocidadePorta + " Gb/s",
                velocidadeMidia, velocidadePorta);
    }

    private static List<Limitacao> limitacao(PlacaMae placaMae, Armazenamento armazenamento,
            String campo, String valorDoNovo, String valorSuportado, double valorNovo,
            double valorSuportadoNumerico) {
        double perda = CalculadoraPcie.perdaPercentual(valorNovo, valorSuportadoNumerico);
        return List.of(Limitacao.builder()
                .regra("L10")
                .limitante(placaMae)
                .afetado(armazenamento)
                .campo(campo)
                .valorDoNovo(valorDoNovo)
                .valorSuportado(valorSuportado)
                .consequencia(TextosLimitacao.armazenamento(placaMae, armazenamento,
                        valorSuportado, valorDoNovo, perda))
                .severidade(SeveridadeLimitacao.AVISO)
                .percentualPerda(perda)
                .construir());
    }
}
