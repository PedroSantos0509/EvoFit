package com.evofit.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.evofit.database.ConnectionFactory;
import com.evofit.model.Mensalidade;
import com.evofit.util.Datas;

import java.util.ArrayList;
import java.util.List;

/** O app só REGISTRA mensalidades; não processa pagamentos. */
public class MensalidadeDAO {
    private static final String SELECT =
            "SELECT m.id_mensalidade, m.id_aluno, u.nome, m.valor, m.data_vencimento, m.data_pagamento, " +
                    "CASE WHEN m.data_pagamento IS NOT NULL THEN 'PAGA' " +
                    "WHEN m.data_vencimento < ? THEN 'ATRASADA' ELSE 'PENDENTE' END " +
                    "FROM mensalidade m JOIN usuario u ON u.id_usuario = m.id_aluno ";

    private final SQLiteDatabase db;

    public MensalidadeDAO(Context c) {
        db = ConnectionFactory.getInstance(c).getConnection();
    }

    private List<Mensalidade> ler(String sqlResto, String... args) {
        List<Mensalidade> l = new ArrayList<>();
        String[] todos = new String[args.length + 1];
        todos[0] = Datas.hoje();
        System.arraycopy(args, 0, todos, 1, args.length);
        try (Cursor c = db.rawQuery(SELECT + sqlResto, todos)) {
            while (c.moveToNext()) {
                Mensalidade m = new Mensalidade();
                m.id = c.getLong(0);
                m.idAluno = c.getLong(1);
                m.alunoNome = c.getString(2);
                m.valor = c.getDouble(3);
                m.vencimento = c.getString(4);
                m.dataPagamento = c.getString(5);
                m.situacao = c.getString(6);
                l.add(m);
            }
        }
        return l;
    }

    public List<Mensalidade> listarPorAluno(long idAluno) {
        return ler("WHERE m.id_aluno = ? ORDER BY m.data_vencimento DESC", String.valueOf(idAluno));
    }

    /** RF22: mensalidades atrasadas ou que vencem nos próximos 5 dias (ainda não pagas). */
    public List<Mensalidade> alertas(long idPersonal) {
        return ler("JOIN aluno a ON a.id_usuario = m.id_aluno " +
                        "WHERE a.id_personal = ? AND m.data_pagamento IS NULL AND m.data_vencimento <= ? " +
                        "ORDER BY m.data_vencimento",
                String.valueOf(idPersonal), Datas.somarDias(5));
    }

    public long inserir(long idAluno, double valor, String vencimento) {
        ContentValues v = new ContentValues();
        v.put("id_aluno", idAluno);
        v.put("valor", valor);
        v.put("data_vencimento", vencimento);
        v.put("situacao", Mensalidade.PENDENTE);
        return db.insert("mensalidade", null, v);
    }

    public void marcarPaga(long idMensalidade) {
        ContentValues v = new ContentValues();
        v.put("data_pagamento", Datas.hoje());
        v.put("situacao", Mensalidade.PAGA);
        db.update("mensalidade", v, "id_mensalidade = ?", new String[]{String.valueOf(idMensalidade)});
    }
}
