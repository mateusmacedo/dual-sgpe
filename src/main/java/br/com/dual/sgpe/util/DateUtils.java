package br.com.dual.sgpe.util;

import br.com.dual.sgpe.exception.ValidacaoException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Utilidades de data no formato ISO-8601 (yyyy-MM-dd). Centraliza o parse de
 * texto do formulário e a comparação de datas usada nas regras de negócio.
 */
public final class DateUtils {

    private static final DateTimeFormatter FORMATO =
        DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);

    private DateUtils() {
    }

    /**
     * Converte texto no formato brasileiro (dd-MM-yyyy) em {@link LocalDate}.
     * Lança {@link ValidacaoException} se o texto for vazio ou tiver formato
     * inválido. O armazenamento no banco permanece ISO; este formato é só de
     * apresentação/entrada.
     */
    public static LocalDate parse(String texto) {
        if (ValidationUtils.isBlank(texto)) {
            throw new ValidacaoException("Data é obrigatória.");
        }
        try {
            return LocalDate.parse(texto.trim(), FORMATO);
        } catch (DateTimeParseException excecao) {
            throw new ValidacaoException("Data inválida: use o formato DD-MM-AAAA.");
        }
    }

    /**
     * Formata uma {@link LocalDate} no padrão brasileiro (dd-MM-yyyy) para exibição.
     */
    public static String format(LocalDate data) {
        return data.format(FORMATO);
    }

    /**
     * Retorna {@code true} se {@code posterior} não for anterior a
     * {@code referencia} (ou seja, posterior &gt;= referencia).
     */
    public static boolean naoAnterior(LocalDate posterior, LocalDate referencia) {
        return !posterior.isBefore(referencia);
    }
}
