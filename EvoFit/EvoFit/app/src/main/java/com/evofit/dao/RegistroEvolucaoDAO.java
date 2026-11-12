package com.evofit.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.evofit.database.ConnectionFactory;
import com.evofit.model.RegistroEvolucao;

import java.util.ArrayList;
import java.util.List;

public class RegistroEvolucaoDAO {
    private final SQLiteDatabase db;

    public RegistroEvolucaoDAO(Context c) {
        db = ConnectionFactory.getInstance(c).getConnection();
    }

    /** Histórico do aluno, do mais recente para o mais antigo. */
    public List<RegistroEvolucao> listar(long idAluno) {
        List<RegistroEvolucao> l = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT id_registro, id_aluno, data_registro, peso_kg, mudancas_corporais " +
                "FROM registro_evolucao WHERE id_aluno = ? ORDER BY data_registro DESC, id_registro DESC",
                new String[]{String.valueOf(idAluno)})) {
            while (c.moveToNext()) {
                RegistroEvolucao r = new RegistroEvolucao();
                r.id = c.getLong(0);
                r.idAluno = c.getLong(1);
                r.data = c.getString(2);
                r.pesoKg = c.isNull(3) ? 0 : c.getDouble(3);
                r.mudancas = c.getString(4);
                l.add(r);
            }
        }
        return l;
    }

    /** Registra a avaliação e atualiza o peso atual do aluno. RN006: registros não são excluídos. */
    public long inserir(long idAluno, String data, double peso, String mudancas) {
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("id_aluno", idAluno);
            v.put("data_registro", data);
            if (peso > 0) v.put("peso_kg", peso);
            v.put("mudancas_corporais", mudancas);
            long id = db.insert("registro_evolucao", null, v);
            if (id != -1 && peso > 0) {
                ContentValues u = new ContentValues();
                u.put("peso_kg", peso);
                db.update("usuario", u, "id_usuario = ?", new String[]{String.valueOf(idAluno)});
            }
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }
}
