package com.evofit.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.evofit.R;
import com.evofit.dao.AlunoDAO;
import com.evofit.dao.MensalidadeDAO;
import com.evofit.dao.UsuarioDAO;
import com.evofit.model.Aluno;
import com.evofit.model.Mensalidade;
import com.evofit.model.Usuario;
import com.evofit.util.Datas;
import com.evofit.util.Imagens;
import com.evofit.util.SessaoUsuario;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Figura 5 - Tela Meu Perfil. */
public class PerfilFragment extends Fragment {

    private UsuarioDAO usuarioDAO;
    private AlunoDAO alunoDAO;
    private Usuario usuario;
    private ImageView imgFoto;

    private final ActivityResultLauncher<String> escolherFoto =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null || usuario == null) return;
                String caminho = Imagens.salvarFotoPerfil(requireContext(), uri, usuario.id);
                if (caminho == null) {
                    FormDialog.toast(requireContext(), "Não foi possível usar essa imagem.");
                    return;
                }
                usuarioDAO.atualizarFoto(usuario.id, caminho);
                atualizar();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        usuarioDAO = new UsuarioDAO(requireContext());
        alunoDAO = new AlunoDAO(requireContext());
        imgFoto = v.findViewById(R.id.imgFoto);

        imgFoto.setOnClickListener(x -> escolherFoto.launch("image/*"));
        v.findViewById(R.id.txtTrocarFoto).setOnClickListener(x -> escolherFoto.launch("image/*"));
        v.findViewById(R.id.btnEditarDados).setOnClickListener(x -> editarDados());
        v.findViewById(R.id.btnSenha).setOnClickListener(x -> alterarSenha());
        v.findViewById(R.id.btnLembretes).setOnClickListener(x -> mostrarLembretes());
        v.findViewById(R.id.btnSair).setOnClickListener(x ->
                FormDialog.confirmar(requireContext(), "Sair", "Deseja encerrar a sessão?",
                        () -> ((MainActivity) requireActivity()).sair()));
    }

    @Override
    public void onResume() {
        super.onResume();
        atualizar();
    }

    private void atualizar() {
        View v = getView();
        if (v == null) return;
        usuario = usuarioDAO.buscarPorId(SessaoUsuario.idLogado(requireContext()));
        if (usuario == null) return;

        ((TextView) v.findViewById(R.id.txtNome)).setText(usuario.nome);
        ((TextView) v.findViewById(R.id.txtCargo)).setText(usuario.isPersonal() ? "Personal Trainer" : "Aluno");
        Imagens.carregar(imgFoto, usuario.fotoPerfil);

        ((TextView) v.findViewById(R.id.txtAltura)).setText(
                usuario.alturaM > 0 ? String.format(Locale.US, "%.2fm", usuario.alturaM) : "--");
        ((TextView) v.findViewById(R.id.txtPeso)).setText(
                usuario.pesoKg > 0 ? String.format(Locale.US, "%.0fkg", usuario.pesoKg) : "--");
        int idade = Datas.idade(usuario.dataNascimento);
        ((TextView) v.findViewById(R.id.txtIdade)).setText(idade >= 0 ? String.valueOf(idade) : "--");

        View blocoAlunos = v.findViewById(R.id.blocoAlunos);
        TextView txtObjetivo = v.findViewById(R.id.txtObjetivo);
        if (usuario.isPersonal()) {
            blocoAlunos.setVisibility(View.VISIBLE);
            txtObjetivo.setVisibility(View.GONE);
            int total = alunoDAO.total(usuario.id);
            ((TextView) v.findViewById(R.id.txtTotalAlunos)).setText(total + " total");
            ((TextView) v.findViewById(R.id.txtNovosAlunos)).setText(alunoDAO.novos(usuario.id) + " novos");
            int pct = alunoDAO.percentualAtivos(usuario.id);
            ((ProgressBar) v.findViewById(R.id.progAtividade)).setProgress(pct);
            ((TextView) v.findViewById(R.id.txtAtividade)).setText(
                    pct + "% dos alunos treinaram nos últimos 7 dias");
        } else {
            blocoAlunos.setVisibility(View.GONE);
            Aluno a = alunoDAO.buscar(usuario.id);
            txtObjetivo.setVisibility(View.VISIBLE);
            txtObjetivo.setText("Objetivo: " + (a != null && a.objetivo != null && !a.objetivo.isEmpty() ? a.objetivo : "não definido"));
        }
    }

    private void editarDados() {
        if (usuario == null) return;
        if (usuario.isAluno()) {
            // RF20: o aluno só altera o objetivo (e a foto).
            Aluno a = alunoDAO.buscar(usuario.id);
            FormDialog.formulario(requireContext(), "Meu objetivo",
                    new String[]{"Objetivo (ex.: emagrecimento, hipertrofia)"},
                    new int[]{FormDialog.TEXTO},
                    new String[]{a == null ? "" : a.objetivo}, "Salvar", vals -> {
                        alunoDAO.atualizarObjetivo(usuario.id, vals[0]);
                        atualizar();
                        return true;
                    });
            return;
        }
        FormDialog.formulario(requireContext(), "Meus dados",
                new String[]{"Nascimento (dd/MM/aaaa)", "Altura (m)", "Peso (kg)"},
                new int[]{FormDialog.DATA, FormDialog.DECIMAL, FormDialog.DECIMAL},
                new String[]{Datas.bancoParaTela(usuario.dataNascimento),
                        usuario.alturaM > 0 ? String.valueOf(usuario.alturaM) : "",
                        usuario.pesoKg > 0 ? String.valueOf(usuario.pesoKg) : ""},
                "Salvar", vals -> {
                    String nasc = null;
                    if (!vals[0].isEmpty()) {
                        nasc = Datas.telaParaBanco(vals[0]);
                        if (nasc == null) { FormDialog.toast(requireContext(), "Data inválida. Use dd/MM/aaaa."); return false; }
                    }
                    double altura = Datas.numero(vals[1]);
                    double peso = Datas.numero(vals[2]);
                    if ((!vals[1].isEmpty() && (altura < 0.5 || altura > 2.6)) || (!vals[2].isEmpty() && (peso < 20 || peso > 400))) {
                        FormDialog.toast(requireContext(), "Altura em metros (ex.: 1,82) e peso em kg (ex.: 84).");
                        return false;
                    }
                    usuarioDAO.atualizarDadosCorporais(usuario.id, nasc, altura, peso);
                    atualizar();
                    return true;
                });
    }

    private void alterarSenha() {
        if (usuario == null) return;
        final boolean temSenha = usuarioDAO.temSenha(usuario.id);
        String[] hints = temSenha
                ? new String[]{"Senha atual", "Nova senha (mín. 6)", "Confirmar nova senha"}
                : new String[]{"Nova senha (mín. 6)", "Confirmar nova senha"};
        int[] tipos = temSenha
                ? new int[]{FormDialog.SENHA, FormDialog.SENHA, FormDialog.SENHA}
                : new int[]{FormDialog.SENHA, FormDialog.SENHA};
        FormDialog.formulario(requireContext(), "Alterar senha", hints, tipos, null, "Salvar", vals -> {
            int o = temSenha ? 1 : 0;
            if (temSenha && !usuarioDAO.confereSenha(usuario.id, vals[0])) {
                FormDialog.toast(requireContext(), "Senha atual incorreta.");
                return false;
            }
            if (vals[o].length() < 6) { FormDialog.toast(requireContext(), "A nova senha precisa ter ao menos 6 caracteres."); return false; }
            if (!vals[o].equals(vals[o + 1])) { FormDialog.toast(requireContext(), "As senhas não conferem."); return false; }
            usuarioDAO.alterarSenha(usuario.id, vals[o]);
            FormDialog.toast(requireContext(), "Senha alterada.");
            return true;
        });
    }

    /** RF22: lembretes de mensalidades atrasadas ou próximas do vencimento. */
    private void mostrarLembretes() {
        if (usuario == null) return;
        MensalidadeDAO dao = new MensalidadeDAO(requireContext());
        List<Mensalidade> todas;
        if (usuario.isPersonal()) {
            todas = dao.alertas(usuario.id);
        } else {
            todas = new java.util.ArrayList<>();
            for (Mensalidade m : dao.listarPorAluno(usuario.id)) {
                if (!Mensalidade.PAGA.equals(m.situacao)) todas.add(m);
            }
        }
        if (todas.isEmpty()) {
            FormDialog.aviso(requireContext(), "Lembretes", "Nenhuma mensalidade atrasada ou a vencer nos próximos 5 dias.");
            return;
        }
        NumberFormat moeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        String[] itens = new String[todas.size()];
        for (int i = 0; i < itens.length; i++) {
            Mensalidade m = todas.get(i);
            itens[i] = m.alunoNome + " · " + moeda.format(m.valor) + "\nvence " + Datas.bancoParaTela(m.vencimento)
                    + (Mensalidade.ATRASADA.equals(m.situacao) ? " (ATRASADA)" : " (a vencer)");
        }
        final List<Mensalidade> lista = todas;
        FormDialog.lista(requireContext(), "Lembretes de pagamento", itens, (d, which) -> {
            if (!usuario.isPersonal()) return;
            Mensalidade m = lista.get(which);
            FormDialog.confirmar(requireContext(), "Registrar pagamento",
                    "Marcar a mensalidade de " + m.alunoNome + " como paga?",
                    () -> { dao.marcarPaga(m.id); FormDialog.toast(requireContext(), "Pagamento registrado."); });
        });
    }
}
