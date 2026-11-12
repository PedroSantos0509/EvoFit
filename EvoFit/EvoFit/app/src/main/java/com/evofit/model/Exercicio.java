package com.evofit.model;

public class Exercicio {
    public long id;
    public String nome;
    public String instrucao;
    public String urlVideo;
    public String grupos;  // nomes dos grupos musculares separados por vírgula

    @Override
    public String toString() {
        return grupos == null || grupos.isEmpty() ? nome : nome + " (" + grupos + ")";
    }
}
