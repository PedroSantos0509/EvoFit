package com.evofit.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.evofit.util.Datas;
import com.evofit.util.Seguranca;

/**
 * Fábrica de conexões do EvoFit (SQLite local).
 * Cria as tabelas na primeira execução e insere dados de exemplo.
 * Uso: SQLiteDatabase db = ConnectionFactory.getInstance(context).getWritableDatabase();
 */
public class ConnectionFactory extends SQLiteOpenHelper {

    private static final String DB_NAME = "evofit.db";
    private static final int DB_VERSION = 1;
    private static ConnectionFactory instance;

    public static synchronized ConnectionFactory getInstance(Context c) {
        if (instance == null) instance = new ConnectionFactory(c.getApplicationContext());
        return instance;
    }

    private ConnectionFactory(Context c) {
        super(c, DB_NAME, null, DB_VERSION);
    }

    public SQLiteDatabase getConnection() {
        return getWritableDatabase();
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE usuario (" +
                "id_usuario INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nome TEXT NOT NULL," +
                "email TEXT NOT NULL UNIQUE," +
                "cpf TEXT UNIQUE," +
                "senha_hash TEXT," +
                "foto_perfil TEXT," +
                "ativo INTEGER NOT NULL DEFAULT 1," +
                "tipo_usuario TEXT NOT NULL CHECK (tipo_usuario IN ('PERSONAL','ALUNO','EQUIPE_TI'))," +
                "data_nascimento TEXT," +
                "altura_m REAL," +
                "peso_kg REAL," +
                "criado_em TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE personal_trainer (" +
                "id_usuario INTEGER PRIMARY KEY REFERENCES usuario(id_usuario) ON DELETE CASCADE," +
                "cref TEXT)");

        db.execSQL("CREATE TABLE equipe_ti (" +
                "id_usuario INTEGER PRIMARY KEY REFERENCES usuario(id_usuario) ON DELETE CASCADE," +
                "matricula TEXT)");

        db.execSQL("CREATE TABLE ficha_treino (" +
                "id_ficha INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_personal INTEGER NOT NULL REFERENCES personal_trainer(id_usuario) ON DELETE CASCADE," +
                "nome TEXT NOT NULL," +
                "foco TEXT," +
                "resumo TEXT)");

        db.execSQL("CREATE TABLE aluno (" +
                "id_usuario INTEGER PRIMARY KEY REFERENCES usuario(id_usuario) ON DELETE CASCADE," +
                "id_personal INTEGER NOT NULL REFERENCES personal_trainer(id_usuario)," +
                "objetivo TEXT," +
                "data_inicio TEXT NOT NULL," +
                "situacao_acesso TEXT NOT NULL DEFAULT 'ATIVO' CHECK (situacao_acesso IN ('ATIVO','RESTRITO','INATIVO'))," +
                "id_ficha_atual INTEGER REFERENCES ficha_treino(id_ficha) ON DELETE SET NULL)");

        db.execSQL("CREATE TABLE grupo_muscular (" +
                "id_grupo INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nome TEXT NOT NULL UNIQUE)");

        db.execSQL("CREATE TABLE exercicio (" +
                "id_exercicio INTEGER PRIMARY KEY AUTOINCREMENT," +
                "nome TEXT NOT NULL," +
                "instrucao_execucao TEXT," +
                "url_video TEXT," +
                "url_imagem TEXT)");

        db.execSQL("CREATE TABLE exercicio_grupo (" +
                "id_exercicio INTEGER NOT NULL REFERENCES exercicio(id_exercicio) ON DELETE CASCADE," +
                "id_grupo INTEGER NOT NULL REFERENCES grupo_muscular(id_grupo)," +
                "PRIMARY KEY (id_exercicio, id_grupo))");

        db.execSQL("CREATE TABLE item_treino (" +
                "id_item INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_ficha INTEGER NOT NULL REFERENCES ficha_treino(id_ficha) ON DELETE CASCADE," +
                "id_exercicio INTEGER NOT NULL REFERENCES exercicio(id_exercicio)," +
                "ordem INTEGER NOT NULL CHECK (ordem > 0)," +
                "series INTEGER NOT NULL CHECK (series > 0)," +
                "repeticoes TEXT NOT NULL," +
                "carga_kg REAL CHECK (carga_kg IS NULL OR carga_kg >= 0)," +
                "descanso_seg INTEGER CHECK (descanso_seg IS NULL OR descanso_seg >= 0)," +
                "bi_set INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE sessao_treino (" +
                "id_sessao INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_aluno INTEGER NOT NULL REFERENCES aluno(id_usuario) ON DELETE CASCADE," +
                "id_ficha INTEGER REFERENCES ficha_treino(id_ficha) ON DELETE SET NULL," +
                "data_treino TEXT NOT NULL," +
                "duracao_minutos INTEGER," +
                "situacao TEXT NOT NULL DEFAULT 'PROGRAMADA' CHECK (situacao IN ('PROGRAMADA','REALIZADA')))");

        db.execSQL("CREATE TABLE registro_evolucao (" +
                "id_registro INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_aluno INTEGER NOT NULL REFERENCES aluno(id_usuario) ON DELETE CASCADE," +
                "data_registro TEXT NOT NULL," +
                "peso_kg REAL," +
                "mudancas_corporais TEXT)");

        db.execSQL("CREATE TABLE foto_evolucao (" +
                "id_foto INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_registro INTEGER NOT NULL REFERENCES registro_evolucao(id_registro) ON DELETE CASCADE," +
                "data_foto TEXT NOT NULL," +
                "caminho_arquivo TEXT NOT NULL)");

        db.execSQL("CREATE TABLE registro_carga (" +
                "id_registro_carga INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_aluno INTEGER NOT NULL REFERENCES aluno(id_usuario) ON DELETE CASCADE," +
                "id_exercicio INTEGER NOT NULL REFERENCES exercicio(id_exercicio)," +
                "id_sessao INTEGER REFERENCES sessao_treino(id_sessao) ON DELETE SET NULL," +
                "data_registro TEXT NOT NULL," +
                "carga_kg REAL NOT NULL CHECK (carga_kg >= 0))");

        db.execSQL("CREATE TABLE mensalidade (" +
                "id_mensalidade INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_aluno INTEGER NOT NULL REFERENCES aluno(id_usuario) ON DELETE CASCADE," +
                "valor REAL NOT NULL CHECK (valor >= 0)," +
                "data_vencimento TEXT NOT NULL," +
                "data_pagamento TEXT," +
                "situacao TEXT NOT NULL DEFAULT 'PENDENTE' CHECK (situacao IN ('PENDENTE','PAGA','ATRASADA'))," +
                "UNIQUE (id_aluno, data_vencimento))");

        db.execSQL("CREATE TABLE lembrete_pagamento (" +
                "id_lembrete INTEGER PRIMARY KEY AUTOINCREMENT," +
                "id_mensalidade INTEGER NOT NULL REFERENCES mensalidade(id_mensalidade) ON DELETE CASCADE," +
                "data_envio TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "mensagem TEXT NOT NULL)");

        db.execSQL("CREATE INDEX idx_sessao_aluno_data ON sessao_treino(id_aluno, data_treino)");
        db.execSQL("CREATE INDEX idx_evolucao_aluno_data ON registro_evolucao(id_aluno, data_registro)");
        db.execSQL("CREATE INDEX idx_mensalidade_aluno_venc ON mensalidade(id_aluno, data_vencimento)");

        popular(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Versão 1: nada a migrar ainda.
    }

    // ------------------------------------------------------------------
    // Dados iniciais
    // ------------------------------------------------------------------
    private void popular(SQLiteDatabase db) {
        // Grupos musculares
        String[] grupos = {"Peito", "Costas", "Pernas", "Ombros", "Bíceps", "Tríceps", "Abdômen", "Cardio", "Mobilidade"};
        long[] g = new long[grupos.length];
        for (int i = 0; i < grupos.length; i++) {
            ContentValues v = new ContentValues();
            v.put("nome", grupos[i]);
            g[i] = db.insert("grupo_muscular", null, v);
        }
        // Exercícios: nome, grupo (índice), termo para o vídeo
        Object[][] ex = {
                {"Agachamento livre", 2, "agachamento livre execucao"},
                {"Leg press 45°", 2, "leg press 45 execucao"},
                {"Cadeira extensora", 2, "cadeira extensora execucao"},
                {"Mesa flexora", 2, "mesa flexora execucao"},
                {"Panturrilha em pé", 2, "panturrilha em pe execucao"},
                {"Esteira (cardio)", 7, "corrida na esteira tecnica"},
                {"Bicicleta ergométrica", 7, "bicicleta ergometrica postura"},
                {"Supino reto", 0, "supino reto execucao"},
                {"Remada curvada", 1, "remada curvada execucao"},
                {"Puxada frontal", 1, "puxada frontal execucao"},
                {"Desenvolvimento com halteres", 3, "desenvolvimento ombros halteres"},
                {"Rosca direta", 4, "rosca direta execucao"},
                {"Tríceps corda", 5, "triceps corda execucao"},
                {"Abdominal infra", 6, "abdominal infra execucao"},
                {"Prancha", 6, "prancha abdominal execucao"},
                {"Alongamento global", 8, "alongamento global"},
                {"Mobilidade de quadril", 8, "mobilidade de quadril"},
                {"Agachamento com salto", 2, "agachamento com salto"}
        };
        long[] idEx = new long[ex.length];
        for (int i = 0; i < ex.length; i++) {
            ContentValues v = new ContentValues();
            v.put("nome", (String) ex[i][0]);
            v.put("url_video", "https://www.youtube.com/results?search_query=" + ((String) ex[i][2]).replace(' ', '+'));
            idEx[i] = db.insert("exercicio", null, v);
            ContentValues eg = new ContentValues();
            eg.put("id_exercicio", idEx[i]);
            eg.put("id_grupo", g[(Integer) ex[i][1]]);
            db.insert("exercicio_grupo", null, eg);
        }

        // Personal de exemplo: login victor@evofit.com / senha 123456
        long personal = inserirUsuario(db, "Victor Hugo", "victor@evofit.com", "52998224725", "123456",
                "PERSONAL", "1996-05-14", 1.82, 84);
        ContentValues p = new ContentValues();
        p.put("id_usuario", personal);
        p.put("cref", "000000-G/SP");
        db.insert("personal_trainer", null, p);

        // Fichas A, B, C
        long fA = inserirFicha(db, personal, "Treino A", "Inferiores & Cardio", "Pernas e glúteos + cardio final");
        long fB = inserirFicha(db, personal, "Treino B", "Superiores & Abdômen", "Peito, costas, ombros, braços e abdômen");
        long fC = inserirFicha(db, personal, "Treino C", "Full Body & Mobilidade", "Alongamento, mobilidade e corpo inteiro");
        inserirItem(db, fA, idEx[0], 1, 4, "10", 40, 90, 0);
        inserirItem(db, fA, idEx[1], 2, 4, "12", 120, 90, 0);
        inserirItem(db, fA, idEx[2], 3, 3, "12", 35, 60, 0);
        inserirItem(db, fA, idEx[3], 4, 3, "12", 30, 60, 0);
        inserirItem(db, fA, idEx[4], 5, 4, "15", 50, 45, 0);
        inserirItem(db, fA, idEx[5], 6, 1, "20 min", 0, 0, 0);
        inserirItem(db, fB, idEx[7], 1, 4, "8-10", 60, 90, 0);
        inserirItem(db, fB, idEx[8], 2, 4, "10", 50, 90, 0);
        inserirItem(db, fB, idEx[10], 3, 3, "12", 16, 60, 0);
        inserirItem(db, fB, idEx[11], 4, 3, "12", 20, 45, 1);
        inserirItem(db, fB, idEx[12], 5, 3, "12", 25, 45, 1);
        inserirItem(db, fB, idEx[13], 6, 3, "20", 0, 30, 0);
        inserirItem(db, fC, idEx[17], 1, 3, "15", 0, 45, 0);
        inserirItem(db, fC, idEx[14], 2, 3, "40 s", 0, 30, 0);
        inserirItem(db, fC, idEx[16], 3, 2, "10", 0, 30, 0);
        inserirItem(db, fC, idEx[15], 4, 1, "5 min", 0, 0, 0);

        // Alunos de exemplo (senha 123456)
        long joao = inserirAluno(db, personal, "João Pedro", "joao@evofit.com", "11144477735", "1998-03-10", 1.78, 80, "Hipertrofia", -90, "ATIVO", fA);
        long maria = inserirAluno(db, personal, "Maria Luiza", "maria@evofit.com", "39053344705", "1995-08-22", 1.65, 62, "Emagrecimento", -60, "ATIVO", fC);
        long carlos = inserirAluno(db, personal, "Carlos Silva", "carlos@evofit.com", "12345678909", "1990-11-02", 1.80, 88, "Condicionamento", -10, "ATIVO", fB);
        inserirAluno(db, personal, "Ana Beatriz", "ana@evofit.com", "98765432100", "2000-01-30", 1.70, 58, "Tonificação", -3, "ATIVO", 0);

        // Treinos programados/realizados em torno de hoje (aparecem no calendário)
        for (int d = -21; d <= 14; d++) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.add(java.util.Calendar.DAY_OF_MONTH, d);
            int dow = c.get(java.util.Calendar.DAY_OF_WEEK);
            boolean passado = d < 0;
            if (dow == java.util.Calendar.MONDAY || dow == java.util.Calendar.WEDNESDAY || dow == java.util.Calendar.FRIDAY) {
                inserirSessao(db, joao, fA, Datas.paraBanco(c), passado ? 50 + (d * d) % 25 : 0, passado);
            }
            if (dow == java.util.Calendar.TUESDAY || dow == java.util.Calendar.THURSDAY) {
                inserirSessao(db, maria, fC, Datas.paraBanco(c), passado ? 45 + (d * d) % 20 : 0, passado);
            }
            if (dow == java.util.Calendar.SATURDAY && d > -15) {
                inserirSessao(db, carlos, fB, Datas.paraBanco(c), passado ? 60 : 0, passado);
            }
        }

        // Mensalidades de exemplo
        inserirMensalidade(db, joao, 250, Datas.somarDias(5), null);
        inserirMensalidade(db, maria, 250, Datas.somarDias(-4), null);          // atrasada
        inserirMensalidade(db, carlos, 250, Datas.somarDias(-20), Datas.somarDias(-18)); // paga
    }

    private long inserirUsuario(SQLiteDatabase db, String nome, String email, String cpf, String senha,
                                String tipo, String nasc, double altura, double peso) {
        ContentValues v = new ContentValues();
        v.put("nome", nome);
        v.put("email", email);
        v.put("cpf", cpf);
        v.put("senha_hash", Seguranca.gerarHash(senha));
        v.put("tipo_usuario", tipo);
        v.put("data_nascimento", nasc);
        v.put("altura_m", altura);
        v.put("peso_kg", peso);
        return db.insert("usuario", null, v);
    }

    private long inserirAluno(SQLiteDatabase db, long personal, String nome, String email, String cpf, String nasc,
                              double altura, double peso, String objetivo, int diasInicio, String situacao, long ficha) {
        long id = inserirUsuario(db, nome, email, cpf, "123456", "ALUNO", nasc, altura, peso);
        ContentValues v = new ContentValues();
        v.put("id_usuario", id);
        v.put("id_personal", personal);
        v.put("objetivo", objetivo);
        v.put("data_inicio", Datas.somarDias(diasInicio));
        v.put("situacao_acesso", situacao);
        if (ficha > 0) v.put("id_ficha_atual", ficha);
        db.insert("aluno", null, v);
        return id;
    }

    private long inserirFicha(SQLiteDatabase db, long personal, String nome, String foco, String resumo) {
        ContentValues v = new ContentValues();
        v.put("id_personal", personal);
        v.put("nome", nome);
        v.put("foco", foco);
        v.put("resumo", resumo);
        return db.insert("ficha_treino", null, v);
    }

    private void inserirItem(SQLiteDatabase db, long ficha, long exercicio, int ordem, int series,
                             String reps, double carga, int descanso, int biSet) {
        ContentValues v = new ContentValues();
        v.put("id_ficha", ficha);
        v.put("id_exercicio", exercicio);
        v.put("ordem", ordem);
        v.put("series", series);
        v.put("repeticoes", reps);
        v.put("carga_kg", carga);
        v.put("descanso_seg", descanso);
        v.put("bi_set", biSet);
        db.insert("item_treino", null, v);
    }

    private void inserirSessao(SQLiteDatabase db, long aluno, long ficha, String data, int minutos, boolean realizada) {
        ContentValues v = new ContentValues();
        v.put("id_aluno", aluno);
        v.put("id_ficha", ficha);
        v.put("data_treino", data);
        v.put("situacao", realizada ? "REALIZADA" : "PROGRAMADA");
        if (realizada) v.put("duracao_minutos", minutos);
        db.insert("sessao_treino", null, v);
    }

    private void inserirMensalidade(SQLiteDatabase db, long aluno, double valor, String venc, String pago) {
        ContentValues v = new ContentValues();
        v.put("id_aluno", aluno);
        v.put("valor", valor);
        v.put("data_vencimento", venc);
        v.put("data_pagamento", pago);
        v.put("situacao", pago != null ? "PAGA" : "PENDENTE");
        db.insert("mensalidade", null, v);
    }
}
