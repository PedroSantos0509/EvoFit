package com.evofit.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.evofit.database.ConnectionFactory;
import com.evofit.model.Exercicio;
import com.evofit.model.FichaTreino;
import com.evofit.model.GrupoMuscular;
import com.evofit.model.ItemTreino;

import java.util.ArrayList;
import java.util.List;

/** Fichas de treino, seus itens (exercícios prescritos) e a biblioteca de exercícios. */
public class FichaTreinoDAO {
    private final SQLiteDatabase db;

    public FichaTreinoDAO(Context c) {
        db = ConnectionFactory.getInstance(c).getConnection();
    }

    private FichaTreino mapear(Cursor c) {
        FichaTreino f = new FichaTreino();
        f.id = c.getLong(0);
        f.idPersonal = c.getLong(1);
        f.nome = c.getString(2);
        f.foco = c.getString(3);
        f.resumo = c.getString(4);
        return f;
    }

    public List<FichaTreino> listar(long idPersonal) {
        List<FichaTreino> l = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT id_ficha, id_personal, nome, foco, resumo FROM ficha_treino " +
                "WHERE id_personal = ? ORDER BY nome", new String[]{String.valueOf(idPersonal)})) {
            while (c.moveToNext()) l.add(mapear(c));
        }
        return l;
    }

    public FichaTreino buscar(long idFicha) {
        try (Cursor c = db.rawQuery("SELECT id_ficha, id_personal, nome, foco, resumo FROM ficha_treino " +
                "WHERE id_ficha = ?", new String[]{String.valueOf(idFicha)})) {
            return c.moveToFirst() ? mapear(c) : null;
        }
    }

    public long inserir(FichaTreino f) {
        ContentValues v = new ContentValues();
        v.put("id_personal", f.idPersonal);
        v.put("nome", f.nome);
        v.put("foco", f.foco);
        v.put("resumo", f.resumo);
        return db.insert("ficha_treino", null, v);
    }

    public void atualizar(FichaTreino f) {
        ContentValues v = new ContentValues();
        v.put("nome", f.nome);
        v.put("foco", f.foco);
        v.put("resumo", f.resumo);
        db.update("ficha_treino", v, "id_ficha = ?", new String[]{String.valueOf(f.id)});
    }

    public void excluir(long idFicha) {
        db.delete("ficha_treino", "id_ficha = ?", new String[]{String.valueOf(idFicha)});
    }

    // ---------------- Itens ----------------
    public List<ItemTreino> itens(long idFicha) {
        List<ItemTreino> l = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT i.id_item, i.id_ficha, i.id_exercicio, e.nome, e.url_video, " +
                "i.ordem, i.series, i.repeticoes, i.carga_kg, i.descanso_seg, i.bi_set " +
                "FROM item_treino i JOIN exercicio e ON e.id_exercicio = i.id_exercicio " +
                "WHERE i.id_ficha = ? ORDER BY i.ordem", new String[]{String.valueOf(idFicha)})) {
            while (c.moveToNext()) {
                ItemTreino i = new ItemTreino();
                i.id = c.getLong(0);
                i.idFicha = c.getLong(1);
                i.idExercicio = c.getLong(2);
                i.exercicioNome = c.getString(3);
                i.urlVideo = c.getString(4);
                i.ordem = c.getInt(5);
                i.series = c.getInt(6);
                i.repeticoes = c.getString(7);
                i.cargaKg = c.isNull(8) ? 0 : c.getDouble(8);
                i.descansoSeg = c.isNull(9) ? 0 : c.getInt(9);
                i.biSet = c.getInt(10) == 1;
                l.add(i);
            }
        }
        return l;
    }

    public long inserirItem(ItemTreino i) {
        int ordem = 1;
        try (Cursor c = db.rawQuery("SELECT COALESCE(MAX(ordem), 0) + 1 FROM item_treino WHERE id_ficha = ?",
                new String[]{String.valueOf(i.idFicha)})) {
            if (c.moveToFirst()) ordem = c.getInt(0);
        }
        ContentValues v = new ContentValues();
        v.put("id_ficha", i.idFicha);
        v.put("id_exercicio", i.idExercicio);
        v.put("ordem", ordem);
        v.put("series", i.series);
        v.put("repeticoes", i.repeticoes);
        v.put("carga_kg", i.cargaKg);
        v.put("descanso_seg", i.descansoSeg);
        v.put("bi_set", i.biSet ? 1 : 0);
        return db.insert("item_treino", null, v);
    }

    public void excluirItem(long idItem) {
        db.delete("item_treino", "id_item = ?", new String[]{String.valueOf(idItem)});
    }

    // ---------------- Biblioteca de exercícios ----------------
    public List<Exercicio> exercicios() {
        List<Exercicio> l = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT e.id_exercicio, e.nome, e.instrucao_execucao, e.url_video, " +
                "GROUP_CONCAT(g.nome, ', ') FROM exercicio e " +
                "LEFT JOIN exercicio_grupo eg ON eg.id_exercicio = e.id_exercicio " +
                "LEFT JOIN grupo_muscular g ON g.id_grupo = eg.id_grupo " +
                "GROUP BY e.id_exercicio ORDER BY e.nome", null)) {
            while (c.moveToNext()) {
                Exercicio e = new Exercicio();
                e.id = c.getLong(0);
                e.nome = c.getString(1);
                e.instrucao = c.getString(2);
                e.urlVideo = c.getString(3);
                e.grupos = c.getString(4);
                l.add(e);
            }
        }
        return l;
    }

    public List<GrupoMuscular> grupos() {
        List<GrupoMuscular> l = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT id_grupo, nome FROM grupo_muscular ORDER BY nome", null)) {
            while (c.moveToNext()) {
                GrupoMuscular g = new GrupoMuscular();
                g.id = c.getLong(0);
                g.nome = c.getString(1);
                l.add(g);
            }
        }
        return l;
    }

    /** RN008: todo exercício precisa de pelo menos um grupo muscular. */
    public long inserirExercicio(String nome, String urlVideo, long idGrupo) {
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("nome", nome);
            v.put("url_video", urlVideo);
            long id = db.insert("exercicio", null, v);
            if (id == -1) return -1;
            ContentValues eg = new ContentValues();
            eg.put("id_exercicio", id);
            eg.put("id_grupo", idGrupo);
            db.insert("exercicio_grupo", null, eg);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }
}
