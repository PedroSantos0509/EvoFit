package com.evofit.model;

/** Ficha de treino (A, B, C...). */
public class FichaTreino {
    public long id;
    public long idPersonal;
    public String nome;    // "Treino A"
    public String foco;    // "Inferiores & Cardio"
    public String resumo;  // texto curto exibido no cartão

    /** Título como na tela: "Treino A - Inferiores & Cardio". */
    public String titulo() {
        return (foco == null || foco.trim().isEmpty()) ? nome : nome + " - " + foco;
    }

    public boolean isCardio() {
        String t = ((foco == null ? "" : foco) + " " + (resumo == null ? "" : resumo)).toLowerCase();
        return t.contains("cardio") || t.contains("mobilidade");
    }
}
