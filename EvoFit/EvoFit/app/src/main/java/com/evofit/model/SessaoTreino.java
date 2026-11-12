package com.evofit.model;

/** Treino programado ou realizado por um aluno em uma data. */
public class SessaoTreino {
    public static final String PROGRAMADA = "PROGRAMADA";
    public static final String REALIZADA = "REALIZADA";

    public long id;
    public long idAluno;
    public String alunoNome;
    public long idFicha;
    public String fichaNome;
    public boolean fichaCardio;
    public String data;          // yyyy-MM-dd
    public int duracaoMinutos;   // preenchido quando REALIZADA
    public String situacao;

    public boolean realizada() { return REALIZADA.equals(situacao); }
}
