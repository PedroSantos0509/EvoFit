package com.evofit.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Guarda o id do usuário logado (mantém o login entre aberturas do app). */
public final class SessaoUsuario {
    private static final String PREFS = "evofit_sessao";
    private static final String KEY_ID = "id_usuario";

    private SessaoUsuario() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void entrar(Context c, long idUsuario) {
        prefs(c).edit().putLong(KEY_ID, idUsuario).apply();
    }

    public static long idLogado(Context c) {
        return prefs(c).getLong(KEY_ID, 0);
    }

    public static void sair(Context c) {
        prefs(c).edit().remove(KEY_ID).apply();
    }
}
