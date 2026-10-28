package pcbuilder.ferramentas;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import pcbuilder.json.Json;

/** Atalhos de leitura sobre um registro JSON do catalogo OpenDB. */
final class Registro {

    private final Object bruto;

    Registro(Object bruto) {
        this.bruto = bruto;
    }

    Object valor(String caminho) {
        return Json.caminho(bruto, caminho);
    }

    String texto(String caminho) {
        return valor(caminho) instanceof String texto ? texto : null;
    }

    BigDecimal numero(String caminho) {
        return valor(caminho) instanceof BigDecimal numero ? numero : null;
    }

    /** Inteiro com zero como valor padrao. */
    int inteiro(String caminho) {
        BigDecimal numero = numero(caminho);
        return numero == null ? 0 : numero.intValue();
    }

    /** Inteiro que pode ser nulo (campo opcional). */
    Integer inteiroNulo(String caminho) {
        BigDecimal numero = numero(caminho);
        return numero == null ? null : Integer.valueOf(numero.intValue());
    }

    Double decimal(String caminho) {
        BigDecimal numero = numero(caminho);
        return numero == null ? null : Double.valueOf(numero.doubleValue());
    }

    Boolean booleano(String caminho) {
        return valor(caminho) instanceof Boolean valor ? valor : null;
    }

    List<Object> lista(String caminho) {
        return valor(caminho) instanceof List<?> itens ? List.copyOf(itens) : List.of();
    }

    String opendbId() {
        return texto("opendb_id");
    }

    String nome() {
        return texto("metadata.name");
    }

    String fabricante() {
        return texto("metadata.manufacturer");
    }

    String nomeLimpo() {
        return Heuristicas.limparNome(nome(), fabricante());
    }

    String id(String prefixo) {
        return Heuristicas.id(prefixo, nomeLimpo(), opendbId());
    }
}
