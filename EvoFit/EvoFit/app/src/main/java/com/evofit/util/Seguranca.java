package com.evofit.util;

import java.security.MessageDigest;
import java.security.SecureRandom;

/** Hash de senha SHA-256 com salt (formato "salt:hash"). */
public final class Seguranca {
    private Seguranca() {}

    public static String gerarHash(String senha) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String s = hex(salt);
        return s + ":" + sha(s + senha);
    }

    public static boolean conferir(String senha, String armazenado) {
        if (armazenado == null || !armazenado.contains(":")) return false;
        String[] p = armazenado.split(":", 2);
        return sha(p[0] + senha).equals(p[1]);
    }

    private static String sha(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return hex(md.digest(texto.getBytes("UTF-8")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
