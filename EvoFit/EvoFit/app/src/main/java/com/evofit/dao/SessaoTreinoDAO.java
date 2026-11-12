package com.evofit.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.evofit.database.ConnectionFactory;
import com.evofit.model.SessaoTreino;

import java.util.ArrayList;
import java.util.List;

/** Treinos programados e realizados (alimenta o calendário). */
public class SessaoTreinoDAO {
    private final SQLiteDatabase db;

    public SessaoTreinoDAO(Context c) {
        db = ConnectionFactory.getInstance(c).getConnection();
    }

    /**
     * Sessões entre duas datas (yyyy-MM-dd, inclusive).
     * idAluno > 0 filtra um aluno; senão filtra todos os alunos do Personal.
     */
    public List<SessaoTreino> listarPeriodo(String ini, String fim, long idPersonal, long idAluno) {
        List<SessaoTreino> l = new ArrayList<>();
        String filtro = idAluno > 0 ? "s.id_aluno = ?" : "a.id_personal = ?";
        String arg = String.valueOf(idAluno > 0 ? idAluno : idPersonal);
        try (Cursor c = db.rawQuery("SELECT s.id_sessao, s.id_aluno, u.nome, s.id_ficha, f.nome, f.foco, f.resumo, " +
                "s.data_treino, s.duracao_minutos, s.situacao " +
                "FROM sessao_treino s " +
                "JOIN aluno a ON a.id_usuario = s.id_aluno " +
                "JOIN usuario u ON u.id_usuario = s.id_aluno " +
                "LEFT JOIN ficha_treino f ON f.id_ficha = s.id_ficha " +
                "WHERE " + filtro + " AND s.data_treino BETWEEN ? AND ? " +
                "ORDER BY s.data_treino, u.nome", new String[]{arg, ini, fim})) {
            while (c.moveToNext()) {
                SessaoTreino s = new SessaoTreino();
                s.id = c.getLong(0);
                s.idAluno = c.getLong(1);
                s.alunoNome = c.getString(2);
                s.idFicha = c.isNull(3) ? 0 : c.getLong(3);
                s.fichaNome = c.getString(4) == null ? "Treino" : c.getString(4);
                String foco = c.getString(5) == null ? "" : c.getString(5);
                String resumo = c.getString(6) == null ? "" : c.getString(6);
                String t = (foco + " " + resumo).toLowerCase();
                s.fichaCardio = t.contains("cardio") || t.contains("mobilidade");
                s.data = c.getString(7);
                s.duracaoMinutos = c.isNull(8) ? 0 : c.getInt(8);
                s.situacao = c.getString(9);
                l.add(s);
            }
        }
        return l;
    }

    public long programar(long idAluno, long idFicha, String data) {
        ContentValues v = new ContentValues();
        v.put("id_aluno", idAluno);
        v.put("id_ficha", idFicha);
        v.put("data_treino", data);
        v.put("situacao", SessaoTreino.PROGRAMADA);
        return db.insert("sessao_treino", null, v);
    }

    /** RF14: registra a duração do treino realizado. */
    public void concluir(long idSessao, int minutos) {
        ContentValues v = new ContentValues();
        v.put("duracao_minutos", minutos);
        v.put("situacao", SessaoTreino.REALIZADA);
        db.update("sessao_treino", v, "id_sessao = ?", new String[]{String.valueOf(idSessao)});
    }

    public void excluir(long idSessao) {
        db.delete("sessao_treino", "id_sessao = ?", new String[]{String.valueOf(idSessao)});
    }
}
