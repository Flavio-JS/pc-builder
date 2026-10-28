package pcbuilder.enums;

/**
 * Gravidade de um problema encontrado no cadastro de um componente
 * (secao 18 do modelo de dados).
 *
 * <ul>
 *   <li>{@link #ERRO} - o registro e descartado, nao entra no catalogo;</li>
 *   <li>{@link #AVISO} - o registro e usavel, mas com ressalva.</li>
 * </ul>
 */
public enum NivelProblema {

    ERRO,
    AVISO
}
