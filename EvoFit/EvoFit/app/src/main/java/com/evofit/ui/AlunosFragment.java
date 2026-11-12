package com.evofit.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evofit.R;
import com.evofit.dao.AlunoDAO;
import com.evofit.model.Aluno;
import com.evofit.util.Imagens;
import com.evofit.util.SessaoUsuario;

import java.util.ArrayList;
import java.util.List;

/** Figura 6 - Tela Alunos (lista, busca, cadastro, edição e exclusão). */
public class AlunosFragment extends Fragment {

    private AlunoDAO alunoDAO;
    private long idPersonal;
    private final List<Aluno> alunos = new ArrayList<>();
    private AlunoAdapter adapter;
    private EditText edtBusca;
    private View txtVazio;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        return inflater.inflate(R.layout.fragment_alunos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        alunoDAO = new AlunoDAO(requireContext());
        idPersonal = SessaoUsuario.idLogado(requireContext());
        edtBusca = v.findViewById(R.id.edtBusca);
        txtVazio = v.findViewById(R.id.txtVazio);

        RecyclerView rv = v.findViewById(R.id.rvAlunos);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AlunoAdapter();
        rv.setAdapter(adapter);

        edtBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { carregar(); }
        });
        v.findViewById(R.id.fabAluno).setOnClickListener(x ->
                AlunoAcoes.formulario(requireContext(), idPersonal, null, this::carregar));
    }

    @Override
    public void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        alunos.clear();
        alunos.addAll(alunoDAO.listar(idPersonal, edtBusca.getText().toString()));
        adapter.notifyDataSetChanged();
        txtVazio.setVisibility(alunos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private class AlunoAdapter extends RecyclerView.Adapter<AlunoAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            ImageView foto;
            TextView nome, status;
            View dot;

            VH(View v) {
                super(v);
                foto = v.findViewById(R.id.imgAluno);
                nome = v.findViewById(R.id.txtNomeAluno);
                status = v.findViewById(R.id.txtStatusAluno);
                dot = v.findViewById(R.id.dotStatus);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_aluno, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Aluno a = alunos.get(pos);
            h.nome.setText(a.nome);
            Imagens.carregar(h.foto, a.fotoPerfil);
            boolean pendente = a.fichaAtualNome == null || !"ATIVO".equals(a.situacaoAcesso);
            if ("RESTRITO".equals(a.situacaoAcesso)) h.status.setText("Restrito");
            else if ("INATIVO".equals(a.situacaoAcesso)) h.status.setText("Inativo");
            else h.status.setText(a.fichaAtualNome == null ? "Pendente" : a.fichaAtualNome);
            h.dot.setBackgroundResource(pendente ? R.drawable.bg_dot_warning : R.drawable.bg_dot_green);
            h.itemView.setOnClickListener(x -> AlunoAcoes.menu(requireContext(), a, idPersonal, AlunosFragment.this::carregar));
        }

        @Override
        public int getItemCount() {
            return alunos.size();
        }
    }
}
