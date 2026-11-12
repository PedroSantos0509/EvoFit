package com.evofit.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Datas são salvas como yyyy-MM-dd e exibidas como dd/MM/yyyy. */
public final class Datas {
    private Datas() {}

    private static SimpleDateFormat banco() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US); }
    private static SimpleDateFormat tela() { return new SimpleDateFormat("dd/MM/yyyy", Locale.US); }

    public static String hoje() { return banco().format(new Date()); }

    public static String somarDias(int dias) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, dias);
        return banco().format(c.getTime());
    }

    public static String paraBanco(Calendar c) { return banco().format(c.getTime()); }

    /** "25/12/1998" -> "1998-12-25"; retorna null se inválida. */
    public static String telaParaBanco(String texto) {
        if (texto == null || texto.trim().isEmpty()) return null;
        try {
            SimpleDateFormat f = tela();
            f.setLenient(false);
            return banco().format(f.parse(texto.trim()));
        } catch (ParseException e) {
            return null;
        }
    }

    /** "1998-12-25" -> "25/12/1998". */
    public static String bancoParaTela(String texto) {
        if (texto == null || texto.isEmpty()) return "";
        try {
            return tela().format(banco().parse(texto));
        } catch (ParseException e) {
            return texto;
        }
    }

    public static int idade(String nascimentoBanco) {
        if (nascimentoBanco == null || nascimentoBanco.isEmpty()) return -1;
        try {
            Calendar nasc = Calendar.getInstance();
            nasc.setTime(banco().parse(nascimentoBanco));
            Calendar hoje = Calendar.getInstance();
            int idade = hoje.get(Calendar.YEAR) - nasc.get(Calendar.YEAR);
            if (hoje.get(Calendar.DAY_OF_YEAR) < nasc.get(Calendar.DAY_OF_YEAR)) idade--;
            return idade;
        } catch (ParseException e) {
            return -1;
        }
    }

    /** Converte "1,82" ou "1.82" em double; retorna 0 se vazio/ inválido. */
    public static double numero(String texto) {
        if (texto == null) return 0;
        try {
            return Double.parseDouble(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
