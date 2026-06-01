package br.com.dual.sgpe.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidationUtilsTest {

    @Test
    void aceitaCpfValidoComEsemMascara() {
        assertTrue(ValidationUtils.isCpfValido("529.982.247-25"));
        assertTrue(ValidationUtils.isCpfValido("52998224725"));
        assertTrue(ValidationUtils.isCpfValido("111.444.777-35"));
    }

    @Test
    void rejeitaSequenciaDeDigitosRepetidos() {
        assertFalse(ValidationUtils.isCpfValido("111.111.111-11"));
        assertFalse(ValidationUtils.isCpfValido("00000000000"));
    }

    @Test
    void rejeitaDigitoVerificadorInvalido() {
        assertFalse(ValidationUtils.isCpfValido("123.456.789-00"));
        assertFalse(ValidationUtils.isCpfValido("529.982.247-20"));
    }

    @Test
    void rejeitaTamanhoOuFormatoInvalido() {
        assertFalse(ValidationUtils.isCpfValido("123"));
        assertFalse(ValidationUtils.isCpfValido("5299822472"));
        assertFalse(ValidationUtils.isCpfValido("abcdefghijk"));
        assertFalse(ValidationUtils.isCpfValido(null));
        assertFalse(ValidationUtils.isCpfValido(""));
    }

    @Test
    void apenasDigitosRemovePontuacao() {
        assertEquals("52998224725", ValidationUtils.apenasDigitos("529.982.247-25"));
        assertEquals("", ValidationUtils.apenasDigitos(null));
    }

    @Test
    void isEmailValidoContinuaFuncionando() {
        assertTrue(ValidationUtils.isEmailValido("a@b.com"));
        assertFalse(ValidationUtils.isEmailValido("invalido"));
    }
}
