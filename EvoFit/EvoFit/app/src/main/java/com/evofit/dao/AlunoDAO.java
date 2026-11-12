package com.evofit.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.evofit.database.ConnectionFactory;
import com.evofit.model.Aluno;
import com.evofit.util.Datas;
import com.evofit.util.Seguranca;

import java.util.ArrayList;
import java.util.List;

public class AlunoDAO {
    private static final String SELECT = "SELECT " + UsuarioDAO.COLUNAS +
            ", a.id_personal, a.objetivo, a.data_inicio, a.situacao_acesso, a.id_ficha_atual, f.nome " +
            "FROM aluno a JOIN usuario u ON u.id_usuario = a.id_usuario " +
            "LEFT JOIN ficha_treino f ON f.id_ficha = a.id_ficha_atual ";

    private final SQLiteDatabase db;

    public AlunoDAO(Context c) {
        db = ConnectionFactory.getInstance(c).getConnection();
    }

    private Aluno mapear(Cursor c) {
        Aluno a = new Aluno();
        UsuarioDAO.preencher(c, a);
        a.idPersonal = c.getLong(9);
        a.objetivo = c.getString(10);
        a.dataInicio = c.getString(11);
        a.situacaoAcesso = c.getString(12);
        a.idFichaAtual = c.isNull(13) ? 0 : c.getLong(13);
        a.fichaAtualNome = c.getString(14);
        return a;
    }

    public List<Aluno> listar(long idPersonal, String busca) {
        List<Aluno> lista = new ArrayList<>();
        String filtro = "%" + (busca == null ? "" : busca.trim()) + "%";
        try (Cursor c = db.rawQuery(SELECT + "WHERE a.id_personal = ? AND u.nome LIKE ? ORDER BY u.nome",
                new String[]{String.valueOf(idPersonal), filtro})) {
            while (c.moveToNext()) lista.add(mapear(c));
        }
        return lista;
    }

    public Aluno buscar(long id) {
        try (Cursor c = db.rawQuery(SELECT + "WHERE a.id_usuario = ?", new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? mapear(c) : null;
        }
    }

    /** Cadastra usuário + aluno. Retorna o id ou -1 se e-mail/CPF já estiverem em uso. */
    public long inserir(Aluno a, String senha) {
        db.beginTransaction();
        try {
            ContentValues u = new ContentValues();
            u.put("nome", a.nome);
            u.put("email", a.email.trim().toLowerCase());
            if (a.cpf != null && !a.cpf.isEmpty()) u.put("cpf", a.cpf);
            u.put("senha_hash", Seguranca.gerarHash(senha));
            u.put("tipo_usuario", "ALUNO");
            u.put("data_nascimento", a.dataNascimento);
            if (a.alturaM > 0) u.put("altura_m", a.alturaM);
            if (a.pesoKg > 0) u.put("peso_kg", a.pesoKg);
            long id = db.insert("usuario", null, u);
            if (id == -1) return -1;

            ContentValues v = new ContentValues();
            v.put("id_usuario", id);
            v.put("id_personal", a.idPersonal);
            v.put("objetivo", a.objetivo);
            v.put("data_inicio", Datas.hoje());
            v.put("situacao_acesso", "ATIVO");
            if (db.insert("aluno", null, v) == -1) return -1;
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    /** Atualiza dados do aluno (não mexe na senha). Retorna false se e-mail/CPF em conflito. */
    public boolean atualizar(Aluno a) {
        db.beginTransaction();
        try {
            ContentValues u = new ContentValues();
            u.put("nome", a.nome);
            u.put("email", a.email.trim().toLowerCase());
            if (a.cpf != null && !a.cpf.isEmpty()) u.put("cpf", a.cpf); else u.putNull("cpf");
            u.put("data_nascimento", a.dataNascimento);
            if (a.alturaM > 0) u.put("altura_m", a.alturaM); else u.putNull("altura_m");
            if (a.pesoKg > 0) u.put("peso_kg", a.pesoKg); else u.putNull("peso_kg");
            String[] id = {String.valueOf(a.id)};
            if (db.update("usuario", u, "id_usuario = ?", id) == 0) return false;
            ContentValues v = new ContentValues();
            v.put("objetivo", a.objetivo);
            v.put("situacao_acesso", a.situacaoAcesso);
            db.update("aluno", v, "id_usuario = ?", id);
            db.setTransactionSuccessful();
            return true;
        } catch (android.database.SQLException e) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public void excluir(long id) {
        db.delete("usuario", "id_usuario = ?", new String[]{String.valueOf(id)}); // cascata remove o aluno
    }

    public void atribuirFicha(long idAluno, long idFicha) {
        ContentValues v = new ContentValues();
        if (idFicha > 0) v.put("id_ficha_atual", idFicha); else v.putNull("id_ficha_atual");
        db.update("aluno", v, "id_usuario = ?", new String[]{String.valueOf(idAluno)});
    }

    public void atualizarObjetivo(long idAluno, String objetivo) {
        ContentValues v = new ContentValues();
        v.put("objetivo", objetivo);
        db.update("aluno", v, "id_usuario = ?", new String[]{String.valueOf(idAluno)});
    }

    public int total(long idPersonal) {
        return contar("SELECT COUNT(*) FROM aluno WHERE id_personal = ?", String.valueOf(idPersonal));
    }

    /** Alunos que começaram nos últimos 30 dias. */
    public int novos(long idPersonal) {
        return contar("SELECT COUNT(*) FROM aluno WHERE id_personal = ? AND data_inicio >= ?",
                String.valueOf(idPersonal), Datas.somarDias(-30));
    }

    /** Percentual de alunos com ao menos um treino realizado nos últimos 7 dias. */
    public int percentualAtivos(long idPersonal) {
        int total = total(idPersonal);
        if (total == 0) return 0;
        int ativos = contar("SELECT COUNT(DISTINCT s.id_aluno) FROM sessao_treino s " +
                        "JOIN aluno a ON a.id_usuario = s.id_aluno " +
                        "WHERE a.id_personal = ? AND s.situacao = 'REALIZADA' AND s.data_treino >= ? AND s.data_treino <= ?",
                String.valueOf(idPersonal), Datas.somarDias(-7), Datas.hoje());
        return Math.round(100f * ativos / total);
    }

    private int contar(String sql, String... args) {
        try (Cursor c = db.rawQuery(sql, args)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }
}
