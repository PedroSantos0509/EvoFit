package com.evofit.model;

/** Exercício prescrito dentro de uma ficha. */
public class ItemTreino {
    public long id;
    public long idFicha;
    public long idExercicio;
    public String exercicioNome;
    public String urlVideo;
    public int ordem;
    public int series;
    public String repeticoes;   // "12" ou "8-10"
    public double cargaKg;      // 0 = sem carga
    public int descansoSeg;
    public boolean biSet;
}
