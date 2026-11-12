package com.evofit.ui;

import android.content.Context;

import com.evofit.dao.AlunoDAO;
import com.evofit.dao.FichaTreinoDAO;
import com.evofit.dao.MensalidadeDAO;
import com.evofit.dao.RegistroEvolucaoDAO;
import com.evofit.model.Aluno;
import com.evofit.model.FichaTreino;
import com.evofit.model.Mensalidade;
import com.evofit.model.RegistroEvolucao;
import com.evofit.util.Datas;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Ações sobre um aluno: editar, treino, evolução, mensalidades, situação e exclusão. */
final class AlunoAcoes {
    private AlunoAcoes() {}

    static void menu(Context ctx, Aluno a, long idPersonal, Runnable aoMudar) {
        String[] opcoes = {"Editar dados", "Atribuir treino", "Evolução corporal",
                "Mensalidades", "Situação de acesso", "Excluir aluno"};
        FormDialog.lista(ctx, a.nome, opcoes, (d, which) -> {
            switch (which) {
                case 0: formulario(ctx, idPersonal, a, aoMudar); break;
                case 1: atribuirTreino(ctx, a, idPersonal, aoMudar); break;
                case 2: evolucao(ctx, a); break;
                case 3: mensalidades(ctx, a); break;
                case 4: situacao(ctx, a, aoMudar); break;
                default:
                    FormDialog.confirmar(ctx, "Excluir aluno",
                            "Excluir " + a.nome + " e todo o histórico (treinos, evolução e mensalidades)?",
                            () -> { new AlunoDAO(ctx).excluir(a.id); aoMudar.run(); });
            }
        });
    }

    /** Cadastro (existente == null) ou edição de aluno. */
    static void formulario(Context ctx, long idPersonal, Aluno existente, Runnable aoMudar) {
        final boolean novo = existente == null;
        String[] hints = novo
                ? new String[]{"Nome", "E-mail", "CPF", "Nascimento (dd/MM/aaaa)", "Altura (m)", "Peso (kg)", "Objetivo", "Senha inicial (mín. 6)"}
                : new String[]{"Nome", "E-mail", "CPF", "Nascimento (dd/MM/aaaa)", "Altura (m)", "Peso (kg)", "Objetivo"};
        int[] tipos = novo
                ? new int[]{FormDialog.NOME, FormDialog.EMAIL, FormDialog.INTEIRO, FormDialog.DATA, FormDialog.DECIMAL, FormDialog.DECIMAL, FormDialog.TEXTO, FormDialog.SENHA}
                : new int[]{FormDialog.NOME, FormDialog.EMAIL, FormDialog.INTEIRO, FormDialog.DATA, FormDialog.DECIMAL, FormDialog.DECIMAL, FormDialog.TEXTO};
        String[] iniciais = novo ? null : new String[]{
                existente.nome, existente.email, existente.cpf == null ? "" : existente.cpf,
                Datas.bancoParaTela(existente.dataNascimento),
                existente.alturaM > 0 ? String.valueOf(existente.alturaM) : "",
                existente.pesoKg > 0 ? String.valueOf(existente.pesoKg) : "",
                existente.objetivo == null ? "" : existente.objetivo};

        FormDialog.formulario(ctx, novo ? "Novo aluno" : "Editar aluno", hints, tipos, iniciais, "Salvar", v -> {
            if (v[0].length() < 3) { FormDialog.toast(ctx, "Informe o nome do aluno."); return false; }
            if (!FormDialog.emailValido(v[1])) { FormDialog.toast(ctx, "E-mail inválido."); return false; }
            if (!v[2].isEmpty() && !FormDialog.cpfValido(v[2])) { FormDialog.toast(ctx, "CPF inválido."); return false; }
            String nasc = null;
            if (!v[3].isEmpty()) {
                nasc = Datas.telaParaBanco(v[3]);
                if (nasc == null) { FormDialog.toast(ctx, "Data inválida. Use dd/MM/aaaa."); return false; }
            }
            double altura = Datas.numero(v[4]);
            double peso = Datas.numero(v[5]);
            if ((!v[4].isEmpty() && (altura < 0.5 || altura > 2.6)) || (!v[5].isEmpty() && (peso < 20 || peso > 400))) {
                FormDialog.toast(ctx, "Altura em metros (ex.: 1,75) e peso em kg (ex.: 70).");
                return false;
            }
            if (novo && v[7].length() < 6) { FormDialog.toast(ctx, "A senha inicial precisa ter ao menos 6 caracteres."); return false; }

            Aluno a = novo ? new Aluno() : existente;
            a.idPersonal = idPersonal;
            a.nome = v[0];
            a.email = v[1];
            a.cpf = FormDialog.soDigitos(v[2]);
            a.dataNascimento = nasc;
            a.alturaM = altura;
            a.pesoKg = peso;
            a.objetivo = v[6];
            AlunoDAO dao = new AlunoDAO(ctx);
            boolean ok;
            if (novo) ok = dao.inserir(a, v[7]) != -1;
            else ok = dao.atualizar(a);
            if (!ok) { FormDialog.toast(ctx, "E-mail ou CPF já cadastrado."); return false; }
            FormDialog.toast(ctx, novo ? "Aluno cadastrado." : "Dados atualizados.");
            aoMudar.run();
            return true;
        });
    }

    private static void atribuirTreino(Context ctx, Aluno a, long idPersonal, Runnable aoMudar) {
        List<FichaTreino> fichas = new FichaTreinoDAO(ctx).listar(idPersonal);
        String[] nomes = new String[fichas.size() + 1];
        nomes[0] = "Sem treino (pendente)";
        int marcado = 0;
        for (int i = 0; i < fichas.size(); i++) {
            nomes[i + 1] = fichas.get(i).titulo();
            if (fichas.get(i).id == a.idFichaAtual) marcado = i + 1;
        }
        new MaterialAlertDialogBuilder(ctx)
                .setTitle("Treino de " + a.nome)
                .setSingleChoiceItems(nomes, marcado, (d, which) -> {
                    new AlunoDAO(ctx).atribuirFicha(a.id, which == 0 ? 0 : fichas.get(which - 1).id);
                    d.dismiss();
                    aoMudar.run();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static void situacao(Context ctx, Aluno a, Runnable aoMudar) {
        String[] valores = {"ATIVO", "RESTRITO", "INATIVO"};
        String[] nomes = {"Ativo", "Restrito (inadimplência)", "Inativo"};
        int marcado = 0;
        for (int i = 0; i < valores.length; i++) if (valores[i].equals(a.situacaoAcesso)) marcado = i;
        new MaterialAlertDialogBuilder(ctx)
                .setTitle("Situação de acesso")
                .setSingleChoiceItems(nomes, marcado, (d, which) -> {
                    a.situacaoAcesso = valores[which];
                    new AlunoDAO(ctx).atualizar(a);
                    d.dismiss();
                    aoMudar.run();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static void evolucao(Context ctx, Aluno a) {
        RegistroEvolucaoDAO dao = new RegistroEvolucaoDAO(ctx);
        List<RegistroEvolucao> regs = dao.listar(a.id);
        StringBuilder sb = new StringBuilder();
        if (regs.isEmpty()) {
            sb.append("Nenhum registro ainda.");
        } else {
            RegistroEvolucao ultimo = regs.get(0), primeiro = regs.get(regs.size() - 1);
            if (regs.size() > 1 && ultimo.pesoKg > 0 && primeiro.pesoKg > 0) {
                double dif = ultimo.pesoKg - primeiro.pesoKg;
                sb.append(String.format(Locale.US, "Variação desde %s: %+.1f kg\n\n", Datas.bancoParaTela(primeiro.data), dif));
            }
            for (RegistroEvolucao r : regs) {
                sb.append(Datas.bancoParaTela(r.data));
                if (r.pesoKg > 0) sb.append(String.format(Locale.US, " · %.1f kg", r.pesoKg));
                if (r.mudancas != null && !r.mudancas.isEmpty()) sb.append("\n   ").append(r.mudancas);
                sb.append("\n");
            }
        }
        new MaterialAlertDialogBuilder(ctx)
                .setTitle("Evolução de " + a.nome)
                .setMessage(sb.toString().trim())
                .setPositiveButton("Novo registro", (d, w) ->
                        FormDialog.formulario(ctx, "Novo registro", new String[]{"Data (dd/MM/aaaa)", "Peso (kg)", "Mudanças corporais"},
                                new int[]{FormDialog.DATA, FormDialog.DECIMAL, FormDialog.TEXTO},
                                new String[]{Datas.bancoParaTela(Datas.hoje()), "", ""}, "Salvar", v -> {
                                    String data = Datas.telaParaBanco(v[0]);
                                    double peso = Datas.numero(v[1]);
                                    if (data == null) { FormDialog.toast(ctx, "Data inválida."); return false; }
                                    if (v[1].isEmpty() && v[2].isEmpty()) { FormDialog.toast(ctx, "Informe o peso ou uma observação."); return false; }
                                    if (!v[1].isEmpty() && (peso < 20 || peso > 400)) { FormDialog.toast(ctx, "Peso inválido."); return false; }
                                    dao.inserir(a.id, data, peso, v[2]);
                                    FormDialog.toast(ctx, "Evolução registrada.");
                                    return true;
                                }))
                .setNegativeButton("Fechar", null)
                .show();
    }

    private static void mensalidades(Context ctx, Aluno a) {
        MensalidadeDAO dao = new MensalidadeDAO(ctx);
        List<Mensalidade> lista = dao.listarPorAluno(a.id);
        NumberFormat moeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(ctx).setTitle("Mensalidades de " + a.nome);
        if (lista.isEmpty()) {
            b.setMessage("Nenhuma mensalidade registrada.");
        } else {
            String[] itens = new String[lista.size()];
            for (int i = 0; i < itens.length; i++) {
                Mensalidade m = lista.get(i);
                itens[i] = Datas.bancoParaTela(m.vencimento) + " · " + moeda.format(m.valor) + " · " + m.situacao;
            }
            b.setItems(itens, (d, which) -> {
                Mensalidade m = lista.get(which);
                if (Mensalidade.PAGA.equals(m.situacao)) {
                    FormDialog.aviso(ctx, "Mensalidade paga", "Pagamento registrado em " + Datas.bancoParaTela(m.dataPagamento) + ".");
                } else {
                    FormDialog.confirmar(ctx, "Registrar pagamento", "Marcar como paga a mensalidade de "
                            + Datas.bancoParaTela(m.vencimento) + "?", () -> {
                        dao.marcarPaga(m.id);
                        FormDialog.toast(ctx, "Pagamento registrado.");
                    });
                }
            });
        }
        b.setPositiveButton("Nova mensalidade", (d, w) ->
                FormDialog.formulario(ctx, "Nova mensalidade", new String[]{"Valor (R$)", "Vencimento (dd/MM/aaaa)"},
                        new int[]{FormDialog.DECIMAL, FormDialog.DATA},
                        new String[]{"", Datas.bancoParaTela(Datas.somarDias(30))}, "Salvar", v -> {
                            double valor = Datas.numero(v[0]);
                            String venc = Datas.telaParaBanco(v[1]);
                            if (valor <= 0) { FormDialog.toast(ctx, "Informe um valor válido."); return false; }
                            if (venc == null) { FormDialog.toast(ctx, "Data inválida."); return false; }
                            if (dao.inserir(a.id, valor, venc) == -1) {
                                FormDialog.toast(ctx, "Já existe mensalidade com esse vencimento.");
                                return false;
                            }
                            FormDialog.toast(ctx, "Mensalidade registrada.");
                            return true;
                        }));
        b.setNegativeButton("Fechar", null);
        b.show();
    }
}
