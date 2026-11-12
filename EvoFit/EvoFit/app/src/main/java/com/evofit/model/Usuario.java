package com.evofit.model;

/** Dados comuns de acesso (tabela usuario) + dados corporais usados no perfil. */
public class Usuario {
    public static final String PERSONAL = "PERSONAL";
    public static final String ALUNO = "ALUNO";
    public static final String EQUIPE_TI = "EQUIPE_TI";

    public long id;
    public String nome;
    public String email;
    public String cpf;
    public String fotoPerfil;      // caminho do arquivo local
    public String tipo;            // PERSONAL, ALUNO ou EQUIPE_TI
    public String dataNascimento;  // yyyy-MM-dd
    public double alturaM;         // 0 = não informado
    public double pesoKg;          // 0 = não informado

    public boolean isPersonal() { return PERSONAL.equals(tipo); }
    public boolean isAluno() { return ALUNO.equals(tipo); }
}
