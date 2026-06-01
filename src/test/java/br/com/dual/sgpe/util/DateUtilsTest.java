package br.com.dual.sgpe.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.dual.sgpe.exception.ValidacaoException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateUtilsTest {

    @Test
    void parseValidoRetornaLocalDate() {
        assertEquals(LocalDate.of(2026, 6, 1), DateUtils.parse("01-06-2026"));
    }

    @Test
    void parseComEspacosNasBordasAceita() {
        assertEquals(LocalDate.of(2026, 6, 1), DateUtils.parse("  01-06-2026  "));
    }

    @Test
    void parseVazioLancaValidacao() {
        assertThrows(ValidacaoException.class, () -> DateUtils.parse("   "));
    }

    @Test
    void parseFormatoIsoLancaValidacao() {
        assertThrows(ValidacaoException.class, () -> DateUtils.parse("2026-06-01"));
    }

    @Test
    void parseFormatoInvalidoLancaValidacao() {
        assertThrows(ValidacaoException.class, () -> DateUtils.parse("01/06/2026"));
    }

    @Test
    void parseDataDeCalendarioInvalidaLancaValidacao() {
        assertThrows(ValidacaoException.class, () -> DateUtils.parse("31-02-2026"));
    }

    @Test
    void formatUsaPadraoBrasileiro() {
        assertEquals("01-06-2026", DateUtils.format(LocalDate.of(2026, 6, 1)));
    }

    @Test
    void naoAnteriorTrueQuandoIgualOuDepois() {
        assertTrue(DateUtils.naoAnterior(LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 1)));
        assertTrue(DateUtils.naoAnterior(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1)));
    }

    @Test
    void naoAnteriorFalseQuandoAntes() {
        assertFalse(DateUtils.naoAnterior(LocalDate.of(2026, 5, 31), LocalDate.of(2026, 6, 1)));
    }
}
