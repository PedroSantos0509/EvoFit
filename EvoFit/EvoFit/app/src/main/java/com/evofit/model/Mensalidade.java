package com.evofit.model;

public class Mensalidade {
    public static final String PENDENTE = "PENDENTE";
    public static final String PAGA = "PAGA";
    public static final String ATRASADA = "ATRASADA";

    public long id;
    public long idAluno;
    public String alunoNome;
    public double valor;
    public String vencimento;     // yyyy-MM-dd
    public String dataPagamento;  // yyyy-MM-dd ou null
    public String situacao;       // calculada pelo DAO (PAGA, ATRASADA ou PENDENTE)
}
