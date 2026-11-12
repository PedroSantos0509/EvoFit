package com.evofit.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.evofit.database.ConnectionFactory;
import com.evofit.model.Usuario;
import com.evofit.util.Seguranca;

public class UsuarioDAO {
    static final String COLUNAS = "u.id_usuario, u.nome, u.email, u.cpf, u.foto_perfil, u.tipo_usuario, " +
            "u.data_nascimento, u.altura_m, u.peso_kg";

    private final SQLiteDatabase db;

    public UsuarioDAO(Context c) {
        db = ConnectionFactory.getInstance(c).getConnection();
    }

    static void preencher(Cursor c, Usuario u) {
        u.id = c.getLong(0);
        u.nome = c.getString(1);
        u.email = c.getString(2);
        u.cpf = c.getString(3);
        u.fotoPerfil = c.getString(4);
        u.tipo = c.getString(5);
        u.dataNascimento = c.getString(6);
        u.alturaM = c.isNull(7) ? 0 : c.getDouble(7);
        u.pesoKg = c.isNull(8) ? 0 : c.getDouble(8);
    }

    private Usuario primeiro(String where, String... args) {
        try (Cursor c = db.rawQuery("SELECT " + COLUNAS + " FROM usuario u WHERE " + where, args)) {
            if (c.moveToFirst()) {
                Usuario u = new Usuario();
                preencher(c, u);
                return u;
            }
        }
        return null;
    }

    public Usuario buscarPorId(long id) {
        return primeiro("u.id_usuario = ?", String.valueOf(id));
    }

    public Usuario buscarPorEmail(String email) {
        return primeiro("LOWER(u.email) = ?", email.trim().toLowerCase());
    }

    /** Login por e-mail ou CPF + senha. Retorna null se inválido. */
    public Usuario autenticar(String login, String senha) {
        String l = login.trim().toLowerCase();
        String cpf = l.replaceAll("\\D", "");
        try (Cursor c = db.rawQuery(
                "SELECT " + COLUNAS + ", u.senha_hash FROM usuario u " +
                        "WHERE u.ativo = 1 AND (LOWER(u.email) = ? OR (u.cpf IS NOT NULL AND u.cpf = ?))",
                new String[]{l, cpf})) {
            if (c.moveToFirst() && Seguranca.conferir(senha, c.getString(9))) {
                Usuario u = new Usuario();
                preencher(c, u);
                return u;
            }
        }
        return null;
    }

    /** Cria um Personal Trainer. Retorna o id ou -1 se e-mail/CPF já existirem. */
    public long cadastrarPersonal(String nome, String email, String cpf, String senha, String cref) {
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("nome", nome);
            v.put("email", email.trim().toLowerCase());
            if (cpf != null && !cpf.isEmpty()) v.put("cpf", cpf);
            if (senha != null) v.put("senha_hash", Seguranca.gerarHash(senha));
            v.put("tipo_usuario", Usuario.PERSONAL);
            long id = db.insert("usuario", null, v);
            if (id == -1) return -1;
            ContentValues p = new ContentValues();
            p.put("id_usuario", id);
            p.put("cref", cref);
            if (db.insert("personal_trainer", null, p) == -1) return -1;
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public void atualizarDadosCorporais(long id, String nascimento, double altura, double peso) {
        ContentValues v = new ContentValues();
        v.put("data_nascimento", nascimento);
        if (altura > 0) v.put("altura_m", altura); else v.putNull("altura_m");
        if (peso > 0) v.put("peso_kg", peso); else v.putNull("peso_kg");
        db.update("usuario", v, "id_usuario = ?", new String[]{String.valueOf(id)});
    }

    public void atualizarFoto(long id, String caminho) {
        ContentValues v = new ContentValues();
        v.put("foto_perfil", caminho);
        db.update("usuario", v, "id_usuario = ?", new String[]{String.valueOf(id)});
    }

    public void alterarSenha(long id, String novaSenha) {
        ContentValues v = new ContentValues();
        v.put("senha_hash", Seguranca.gerarHash(novaSenha));
        db.update("usuario", v, "id_usuario = ?", new String[]{String.valueOf(id)});
    }

    public boolean confereSenha(long id, String senha) {
        try (Cursor c = db.rawQuery("SELECT senha_hash FROM usuario WHERE id_usuario = ?", new String[]{String.valueOf(id)})) {
            return c.moveToFirst() && Seguranca.conferir(senha, c.getString(0));
        }
    }

    public boolean temSenha(long id) {
        try (Cursor c = db.rawQuery("SELECT senha_hash FROM usuario WHERE id_usuario = ?", new String[]{String.valueOf(id)})) {
            return c.moveToFirst() && c.getString(0) != null;
        }
    }

    /** Recuperar senha: confere e-mail + CPF cadastrados e define a nova senha. */
    public boolean redefinirSenha(String email, String cpf, String novaSenha) {
        String cpfNum = cpf.replaceAll("\\D", "");
        ContentValues v = new ContentValues();
        v.put("senha_hash", Seguranca.gerarHash(novaSenha));
        return db.update("usuario", v, "LOWER(email) = ? AND cpf = ? AND ativo = 1",
                new String[]{email.trim().toLowerCase(), cpfNum}) > 0;
    }
}
