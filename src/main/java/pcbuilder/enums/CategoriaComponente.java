package pcbuilder.enums;

/**
 * Categoria do componente dentro da montagem (secao 12 do modelo de dados).
 *
 * <p>O rotulo e o texto usado na interface de linha de comando. Os rotulos ficam
 * sem acento de proposito: o console do Windows trabalha em CP850 e acentos
 * apareceriam corrompidos.</p>
 */
public enum CategoriaComponente {

    CPU("Processador"),
    PLACA_MAE("Placa-Mae"),
    MEMORIA("Memoria"),
    PLACA_VIDEO("Placa de Video"),
    ARMAZENAMENTO("Armazenamento"),
    FONTE("Fonte"),
    COOLER("Cooler"),
    GABINETE("Gabinete"),
    MONITOR("Monitor");

    private final String rotulo;

    CategoriaComponente(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
