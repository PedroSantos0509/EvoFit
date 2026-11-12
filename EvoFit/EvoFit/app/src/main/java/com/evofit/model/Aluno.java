package com.evofit.model;

/** Aluno = Usuario + vínculo com o Personal e treino atual. */
public class Aluno extends Usuario {
    public long idPersonal;
    public String objetivo;
    public String dataInicio;      // yyyy-MM-dd
    public String situacaoAcesso;  // ATIVO, RESTRITO, INATIVO
    public long idFichaAtual;      // 0 = sem treino
    public String fichaAtualNome;  // ex.: "Treino A"
}
