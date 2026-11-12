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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evofit.R;
import com.evofit.dao.AlunoDAO;
import com.evofit.dao.FichaTreinoDAO;
import com.evofit.dao.SessaoTreinoDAO;
import com.evofit.dao.UsuarioDAO;
import com.evofit.model.Aluno;
import com.evofit.model.FichaTreino;
import com.evofit.model.SessaoTreino;
import com.evofit.model.Usuario;
import com.evofit.util.Datas;
import com.evofit.util.SessaoUsuario;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Figura 8 - Tela Calendário: treinos programados e realizados do mês. */
public class CalendarioFragment extends Fragment {

    private SessaoTreinoDAO sessaoDAO;
    private Usuario usuario;
    private Calendar mes = Calendar.getInstance();   // sempre no dia 1
    private final List<Celula> celulas = new ArrayList<>();
    private final Map<String, List<SessaoTreino>> porDia = new HashMap<>();
    private DiaAdapter adapter;
    private TextView txtMes;

    static class Celula {
        int dia;
        boolean mesAtual, hoje;
        String data;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        return inflater.inflate(R.layout.fragment_calendario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        sessaoDAO = new SessaoTreinoDAO(requireContext());
        usuario = new UsuarioDAO(requireContext()).buscarPorId(SessaoUsuario.idLogado(requireContext()));
        txtMes = v.findViewById(R.id.txtMes);
        mes.set(Calendar.DAY_OF_MONTH, 1);

        RecyclerView rv = v.findViewById(R.id.rvDias);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        adapter = new DiaAdapter();
        rv.setAdapter(adapter);

        v.findViewById(R.id.btnMesAnterior).setOnClickListener(x -> { mes.add(Calendar.MONTH, -1); montar(); });
        v.findViewById(R.id.btnMesSeguinte).setOnClickListener(x -> { mes.add(Calendar.MONTH, 1); montar(); });
    }

    @Override
    public void onResume() {
        super.onResume();
        montar();
    }

    private void montar() {
        if (usuario == null) return;
        // título
        String titulo = new SimpleDateFormat("MMMM 'de' yyyy", new Locale("pt", "BR")).format(mes.getTime());
        txtMes.setText(titulo.substring(0, 1).toUpperCase() + titulo.substring(1));

        // grade (semana começa na segunda)
        Calendar c = (Calendar) mes.clone();
        int offset = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;   // segunda=0 ... domingo=6
        int diasNoMes = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        int total = (int) Math.ceil((offset + diasNoMes) / 7.0) * 7;
        c.add(Calendar.DAY_OF_MONTH, -offset);
        String hoje = Datas.hoje();
        String primeiro = Datas.paraBanco(c);
        celulas.clear();
        for (int i = 0; i < total; i++) {
            Celula cel = new Celula();
            cel.dia = c.get(Calendar.DAY_OF_MONTH);
            cel.mesAtual = c.get(Calendar.MONTH) == mes.get(Calendar.MONTH);
            cel.data = Datas.paraBanco(c);
            cel.hoje = cel.data.equals(hoje);
            celulas.add(cel);
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        String ultimo = celulas.get(celulas.size() - 1).data;

        // sessões do período
        porDia.clear();
        long idAluno = usuario.isAluno() ? usuario.id : 0;
        for (SessaoTreino s : sessaoDAO.listarPeriodo(primeiro, ultimo, usuario.id, idAluno)) {
            List<SessaoTreino> l = porDia.get(s.data);
            if (l == null) { l = new ArrayList<>(); porDia.put(s.data, l); }
            l.add(s);
        }
        adapter.notifyDataSetChanged();
    }

    // ---------------- Grade ----------------
    private class DiaAdapter extends RecyclerView.Adapter<DiaAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            TextView dia;
            ImageView halter, corrida, check;

            VH(View v) {
                super(v);
                dia = v.findViewById(R.id.txtDia);
                halter = v.findViewById(R.id.imgHalter);
                corrida = v.findViewById(R.id.imgCorrida);
                check = v.findViewById(R.id.imgCheck);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_dia, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Celula cel = celulas.get(pos);
            h.dia.setText(String.valueOf(cel.dia));
            h.dia.setAlpha(cel.mesAtual ? 1f : 0.3f);
            h.dia.setBackgroundResource(cel.hoje ? R.drawable.bg_dia_hoje : 0);

            boolean halter = false, corrida = false, check = false;
            List<SessaoTreino> l = cel.mesAtual ? porDia.get(cel.data) : null;
            if (l != null) {
                for (SessaoTreino s : l) {
                    if (s.fichaCardio) corrida = true; else halter = true;
                    if (s.realizada()) check = true;
                }
            }
            h.halter.setVisibility(halter ? View.VISIBLE : View.GONE);
            h.corrida.setVisibility(corrida ? View.VISIBLE : View.GONE);
            h.check.setVisibility(check ? View.VISIBLE : View.GONE);
            if (cel.mesAtual) {
                h.itemView.setOnClickListener(x -> abrirDia(cel));
            } else {
                h.itemView.setOnClickListener(null);
                h.itemView.setClickable(false);
            }
        }

        @Override
        public int getItemCount() {
            return celulas.size();
        }
    }

    // ---------------- Detalhe do dia ----------------
    private void abrirDia(Celula cel) {
        Context ctx = requireContext();
        List<SessaoTreino> l = porDia.get(cel.data);
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(ctx).setTitle(Datas.bancoParaTela(cel.data));
        if (l == null || l.isEmpty()) {
            b.setMessage("Nenhum treino neste dia.");
        } else {
            String[] linhas = new String[l.size()];
            for (int i = 0; i < linhas.length; i++) {
                SessaoTreino s = l.get(i);
                linhas[i] = (usuario.isPersonal() ? s.alunoNome + " · " : "") + s.fichaNome
                        + (s.realizada() ? "  ✓ " + s.duracaoMinutos + " min" : "  (programado)");
            }
            b.setItems(linhas, (d, which) -> opcoesSessao(l.get(which)));
        }
        b.setNegativeButton("Fechar", null);
        if (usuario.isPersonal()) b.setPositiveButton("Programar treino", (d, w) -> programar(cel.data));
        b.show();
    }

    private void opcoesSessao(SessaoTreino s) {
        Context ctx = requireContext();
        List<String> ops = new ArrayList<>();
        if (!s.realizada()) ops.add("Marcar como realizado");
        if (usuario.isPersonal()) ops.add("Excluir treino");
        if (ops.isEmpty()) {
            FormDialog.aviso(ctx, s.fichaNome, "Treino realizado em " + Datas.bancoParaTela(s.data)
                    + " com duração de " + s.duracaoMinutos + " min.");
            return;
        }
        FormDialog.lista(ctx, s.fichaNome, ops.toArray(new String[0]), (d, which) -> {
            if (ops.get(which).startsWith("Marcar")) {
                // RF14: duração do treino
                FormDialog.formulario(ctx, "Duração do treino", new String[]{"Minutos"},
                        new int[]{FormDialog.INTEIRO}, new String[]{"60"}, "Salvar", v -> {
                            int min = (int) Datas.numero(v[0]);
                            if (min <= 0 || min > 600) { FormDialog.toast(ctx, "Informe a duração em minutos."); return false; }
                            sessaoDAO.concluir(s.id, min);
                            montar();
                            return true;
                        });
            } else {
                FormDialog.confirmar(ctx, "Excluir treino", "Remover este treino do calendário?",
                        () -> { sessaoDAO.excluir(s.id); montar(); });
            }
        });
    }

    private void programar(String data) {
        Context ctx = requireContext();
        List<Aluno> alunos = new AlunoDAO(ctx).listar(usuario.id, "");
        List<FichaTreino> fichas = new FichaTreinoDAO(ctx).listar(usuario.id);
        if (alunos.isEmpty() || fichas.isEmpty()) {
            FormDialog.aviso(ctx, "Programar treino", "Cadastre ao menos um aluno e uma ficha de treino antes de programar.");
            return;
        }
        String[] nomesAlunos = new String[alunos.size()];
        for (int i = 0; i < nomesAlunos.length; i++) nomesAlunos[i] = alunos.get(i).nome;
        new MaterialAlertDialogBuilder(ctx).setTitle("Aluno")
                .setItems(nomesAlunos, (d, qual) -> {
                    Aluno a = alunos.get(qual);
                    String[] nomesFichas = new String[fichas.size()];
                    for (int i = 0; i < nomesFichas.length; i++) nomesFichas[i] = fichas.get(i).titulo();
                    new MaterialAlertDialogBuilder(ctx).setTitle("Treino de " + a.nome + " em " + Datas.bancoParaTela(data))
                            .setItems(nomesFichas, (dd, qualFicha) -> {
                                sessaoDAO.programar(a.id, fichas.get(qualFicha).id, data);
                                FormDialog.toast(ctx, "Treino programado.");
                                montar();
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
