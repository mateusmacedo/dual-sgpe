package br.com.dual.sgpe.util;

import java.util.regex.Pattern;

/**
 * Utilitários de validação reutilizáveis pelos controllers.
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private ValidationUtils() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isEmailValido(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Remove tudo que não for dígito. Retorna string vazia para entrada nula.
     */
    public static String apenasDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    /**
     * Valida um CPF pelos dígitos verificadores. Aceita com ou sem pontuação;
     * rejeita tamanho diferente de 11, sequências repetidas e dígitos inválidos.
     */
    public static boolean isCpfValido(String cpf) {
        String digitos = apenasDigitos(cpf);
        if (digitos.length() != 11) {
            return false;
        }
        if (digitos.chars().distinct().count() == 1) {
            return false;
        }
        int primeiroDigito = calcularDigitoVerificador(digitos, 9, 10);
        int segundoDigito = calcularDigitoVerificador(digitos, 10, 11);
        return primeiroDigito == (digitos.charAt(9) - '0')
            && segundoDigito == (digitos.charAt(10) - '0');
    }

    private static int calcularDigitoVerificador(String digitos, int quantidade, int pesoInicial) {
        int soma = 0;
        int peso = pesoInicial;
        for (int i = 0; i < quantidade; i++) {
            soma += (digitos.charAt(i) - '0') * peso;
            peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
