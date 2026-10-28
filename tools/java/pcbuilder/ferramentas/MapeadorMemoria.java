package pcbuilder.ferramentas;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import pcbuilder.enums.TipoMemoria;
import pcbuilder.modelo.Componente;
import pcbuilder.modelo.Memoria;

/**
 * Converte os registros de {@code open-db/RAM} para o modelo do projeto.
 *
 * <p>O catalogo traz tipo, velocidade, latencia, numero de pentes e capacidade; a
 * tensao, o valor JEDEC e a altura do pente sao derivados (1.35 V, JEDEC do tipo e
 * 44 mm com dissipador / 34 mm sem).</p>
 */
final class MapeadorMemoria {

    private MapeadorMemoria() {
        // classe utilitaria: nao deve ser instanciada
    }

    static List<Componente> converter(List<Object> brutos, Estatisticas estatisticas) {
        List<Componente> componentes = new ArrayList<>();
        for (Object bruto : brutos) {
            Componente componente = mapear(new Registro(bruto), estatisticas);
            if (componente != null) {
                componentes.add(componente);
            }
        }
        estatisticas.incluir("MEMORIA", componentes.size());
        return componentes;
    }

    private static Componente mapear(Registro registro, Estatisticas estatisticas) {
        TipoMemoria tipo = tipoDe(registro.texto("ram_type"));
        if (tipo == null) {
            estatisticas.ignorar("Memoria: tipo fora do dominio");
            return null;
        }
        int pentes = registro.inteiro("modules.quantity");
        int capacidadePorPente = registro.inteiro("modules.capacity_gb");
        int frequencia = registro.inteiro("speed");
        if (pentes <= 0 || capacidadePorPente <= 0 || frequencia <= 0) {
            estatisticas.ignorar("Memoria: sem pentes/capacidade/frequencia");
            return null;
        }

        int latencia = registro.inteiro("cas_latency");
        if (latencia <= 0) {
            latencia = tipo == TipoMemoria.DDR5 ? 30 : 16;
        }
        Double tensao = registro.decimal("voltage");
        if (tensao == null || tensao <= 0) {
            tensao = 1.35;
        }

        List<Object> perfis = registro.lista("profile_support");
        boolean expo = perfis.contains("EXPO");
        boolean xmp = perfis.contains("XMP");

        int totalGB = pentes * capacidadePorPente;
        double precoBase = tipo == TipoMemoria.DDR5
                ? 22.5 * totalGB + Math.max(0, frequencia - 4800) * 0.15
                : 12.0 * totalGB + Math.max(0, frequencia - 3200) * 0.10;

        return Memoria.builder()
                .id(registro.id("ram"))
                .modelo(registro.nomeLimpo())
                .fabricante(registro.fabricante() == null ? "Desconhecido" : registro.fabricante())
                .consumoEstimadoWatts(Math.max(3, 3 * pentes))
                .precoMedioBRL(Heuristicas.preco(precoBase))
                .garantiaMeses(120)
                .tipo(tipo)
                .capacidadePorPenteGB(capacidadePorPente)
                .quantidadePentes(pentes)
                .frequenciaMTs(frequencia)
                .frequenciaJEDECBaseMTs(Math.min(frequencia,
                        tipo == TipoMemoria.DDR5 ? 4800 : 2133))
                .latenciaCAS(latencia)
                .tensaoVolts(tensao)
                .perfilEXPO(expo)
                .perfilXMP(xmp)
                .possuiECC("ECC".equalsIgnoreCase(registro.texto("ecc")))
                .alturaMm(Boolean.TRUE.equals(registro.booleano("heat_spreader")) ? 44 : 34)
                .construir();
    }

    private static TipoMemoria tipoDe(String tipo) {
        if (tipo == null) {
            return null;
        }
        return switch (tipo.trim().toUpperCase()) {
            case "DDR4" -> TipoMemoria.DDR4;
            case "DDR5" -> TipoMemoria.DDR5;
            default -> null;
        };
    }
}
