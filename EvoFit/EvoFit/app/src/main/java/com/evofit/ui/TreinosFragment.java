package com.evofit.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evofit.R;
import com.evofit.dao.AlunoDAO;
import com.evofit.dao.FichaTreinoDAO;
import com.evofit.dao.UsuarioDAO;
import com.evofit.model.Aluno;
import com.evofit.model.Exercicio;
import com.evofit.model.FichaTreino;
import com.evofit.model.GrupoMuscular;
import com.evofit.model.ItemTreino;
import com.evofit.model.Usuario;
import com.evofit.util.Datas;
import com.evofit.util.SessaoUsuario;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Figura 7 - Tela Treinos (fichas A, B, C). Personal edita; aluno só visualiza o seu treino. */
public class TreinosFragment extends Fragment {

    private FichaTreinoDAO dao;
    private Usuario usuario;
    private final List<FichaTreino> fichas = new ArrayList<>();
    private FichaAdapter adapter;
    private View txtVazio;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        return inflater.inflate(R.layout.fragment_treinos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        dao = new FichaTreinoDAO(requireContext());
        usuario = new UsuarioDAO(requireContext()).buscarPorId(SessaoUsuario.idLogado(requireContext()));
        txtVazio = v.findViewById(R.id.txtVazioTreino);

        RecyclerView rv = v.findViewById(R.id.rvTreinos);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FichaAdapter();
        rv.setAdapter(adapter);

        View fab = v.findViewById(R.id.fabTreino);
        if (usuario != null && usuario.isPersonal()) {
            fab.setOnClickListener(x -> formularioFicha(null));
        } else {
            fab.setVisibility(View.GONE); // RF13: aluno não altera treinos
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        fichas.clear();
        if (usuario != null) {
            if (usuario.isPersonal()) {
                fichas.addAll(dao.listar(usuario.id));
            } else {
                Aluno a = new AlunoDAO(requireContext()).buscar(usuario.id);
                if (a != null && a.idFichaAtual > 0 && "ATIVO".equals(a.situacaoAcesso)) {
                    FichaTreino f = dao.buscar(a.idFichaAtual);
                    if (f != null) fichas.add(f);
                }
            }
        }
        adapter.notifyDataSetChanged();
        txtVazio.setVisibility(fichas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ---------------- Lista ----------------
    private class FichaAdapter extends RecyclerView.Adapter<FichaAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            ImageView icone;
            TextView titulo, foco, resumo;

            VH(View v) {
                super(v);
                icone = v.findViewById(R.id.imgIconeFicha);
                titulo = v.findViewById(R.id.txtTituloFicha);
                foco = v.findViewById(R.id.txtFocoFicha);
                resumo = v.findViewById(R.id.txtResumoFicha);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_ficha, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            FichaTreino f = fichas.get(pos);
            h.titulo.setText(f.titulo());
            h.foco.setText("Foco: " + (f.foco == null ? "-" : f.foco));
            h.resumo.setText("Resumo: " + (f.resumo == null ? "-" : f.resumo));
            boolean mobilidade = f.foco != null && f.foco.toLowerCase().contains("mobilidade");
            h.icone.setImageResource(mobilidade ? R.drawable.ic_runner : R.drawable.ic_dumbbell);
            h.itemView.setOnClickListener(x -> detalhe(f));
        }

        @Override
        public int getItemCount() {
            return fichas.size();
        }
    }

    // ---------------- Detalhe da ficha ----------------
    private void detalhe(FichaTreino f) {
        Context ctx = requireContext();
        List<ItemTreino> itens = dao.itens(f.id);
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(ctx).setTitle(f.titulo());
        if (itens.isEmpty()) {
            b.setMessage("Nenhum exercício nesta ficha ainda.");
        } else {
            String[] linhas = new String[itens.size()];
            for (int i = 0; i < linhas.length; i++) linhas[i] = descrever(itens.get(i));
            b.setItems(linhas, (d, which) -> opcoesItem(f, itens.get(which)));
        }
        b.setNegativeButton("Fechar", null);
        if (usuario.isPersonal()) {
            b.setPositiveButton("+ Exercício", (d, w) -> escolherExercicio(f));
            b.setNeutralButton("Opções", (d, w) -> FormDialog.lista(ctx, f.nome, new String[]{"Editar ficha", "Excluir ficha"},
                    (dd, which) -> {
                        if (which == 0) formularioFicha(f);
                        else FormDialog.confirmar(ctx, "Excluir ficha",
                                "Excluir \"" + f.titulo() + "\"? Alunos que usam esta ficha ficarão sem treino.",
                                () -> { dao.excluir(f.id); carregar(); });
                    }));
        }
        b.show();
    }

    private String descrever(ItemTreino i) {
        StringBuilder sb = new StringBuilder();
        sb.append(i.ordem).append(". ").append(i.exercicioNome).append("\n   ")
                .append(i.series).append(" x ").append(i.repeticoes);
        if (i.cargaKg > 0) sb.append(" · ").append(String.format(Locale.US, "%.0f", i.cargaKg)).append(" kg");
        if (i.descansoSeg > 0) sb.append(" · descanso ").append(i.descansoSeg).append("s");
        if (i.biSet) sb.append(" · bi-set");
        return sb.toString();
    }

    private void opcoesItem(FichaTreino f, ItemTreino item) {
        Context ctx = requireContext();
        List<String> ops = new ArrayList<>();
        ops.add("Ver vídeo / instruções");
        if (usuario.isPersonal()) ops.add("Remover da ficha");
        FormDialog.lista(ctx, item.exercicioNome, ops.toArray(new String[0]), (d, which) -> {
            if (which == 0) {
                FormDialog.abrirLink(ctx, item.urlVideo);
            } else {
                dao.excluirItem(item.id);
                detalhe(f);
            }
        });
    }

    // ---------------- Cadastro de ficha ----------------
    private void formularioFicha(FichaTreino existente) {
        Context ctx = requireContext();
        final boolean nova = existente == null;
        FormDialog.formulario(ctx, nova ? "Nova ficha" : "Editar ficha",
                new String[]{"Nome (ex.: Treino D)", "Foco (ex.: Costas & Bíceps)", "Resumo"},
                new int[]{FormDialog.NOME, FormDialog.NOME, FormDialog.TEXTO},
                nova ? null : new String[]{existente.nome, existente.foco, existente.resumo},
                "Salvar", v -> {
                    if (v[0].isEmpty()) { FormDialog.toast(ctx, "Informe o nome da ficha."); return false; }
                    FichaTreino f = nova ? new FichaTreino() : existente;
                    f.idPersonal = usuario.id;
                    f.nome = v[0];
                    f.foco = v[1];
                    f.resumo = v[2];
                    if (nova) dao.inserir(f); else dao.atualizar(f);
                    carregar();
                    return true;
                });
    }

    // ---------------- Adicionar exercício à ficha ----------------
    private void escolherExercicio(FichaTreino f) {
        Context ctx = requireContext();
        List<Exercicio> lista = dao.exercicios();
        String[] nomes = new String[lista.size() + 1];
        nomes[0] = "➕ Cadastrar novo exercício";
        for (int i = 0; i < lista.size(); i++) nomes[i + 1] = lista.get(i).toString();
        new MaterialAlertDialogBuilder(ctx)
                .setTitle("Escolha o exercício")
                .setItems(nomes, (d, which) -> {
                    if (which == 0) novoExercicio(f);
                    else dadosDoItem(f, lista.get(which - 1));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void novoExercicio(FichaTreino f) {
        Context ctx = requireContext();
        FormDialog.formulario(ctx, "Novo exercício", new String[]{"Nome", "Link do vídeo (opcional)"},
                new int[]{FormDialog.NOME, FormDialog.TEXTO}, null, "Próximo", v -> {
                    if (v[0].isEmpty()) { FormDialog.toast(ctx, "Informe o nome do exercício."); return false; }
                    final String nome = v[0], url = v[1];
                    List<GrupoMuscular> grupos = dao.grupos();
                    String[] nomes = new String[grupos.size()];
                    for (int i = 0; i < nomes.length; i++) nomes[i] = grupos.get(i).nome;
                    // RN008: todo exercício precisa de um grupo muscular
                    new MaterialAlertDialogBuilder(ctx)
                            .setTitle("Grupo muscular")
                            .setItems(nomes, (d, which) -> {
                                long id = dao.inserirExercicio(nome, url, grupos.get(which).id);
                                if (id == -1) { FormDialog.toast(ctx, "Erro ao salvar o exercício."); return; }
                                Exercicio e = new Exercicio();
                                e.id = id;
                                e.nome = nome;
                                dadosDoItem(f, e);
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                    return true;
                });
    }

    private void dadosDoItem(FichaTreino f, Exercicio e) {
        Context ctx = requireContext();
        FormDialog.formulario(ctx, e.nome,
                new String[]{"Séries", "Repetições (ex.: 12 ou 8-10)", "Carga (kg)", "Descanso (seg)", "Bi-set? (S/N)"},
                new int[]{FormDialog.INTEIRO, FormDialog.TEXTO, FormDialog.DECIMAL, FormDialog.INTEIRO, FormDialog.TEXTO},
                new String[]{"3", "12", "", "60", "N"}, "Adicionar", v -> {
                    int series = (int) Datas.numero(v[0]);
                    if (series <= 0 || v[1].isEmpty()) {
                        FormDialog.toast(ctx, "Informe séries (> 0) e repetições.");
                        return false;
                    }
                    ItemTreino i = new ItemTreino();
                    i.idFicha = f.id;
                    i.idExercicio = e.id;
                    i.series = series;
                    i.repeticoes = v[1];
                    i.cargaKg = Datas.numero(v[2]);
                    i.descansoSeg = (int) Datas.numero(v[3]);
                    i.biSet = v[4].toUpperCase(Locale.ROOT).startsWith("S");
                    dao.inserirItem(i);
                    FormDialog.toast(ctx, "Exercício adicionado.");
                    detalhe(f);
                    return true;
                });
    }
}
