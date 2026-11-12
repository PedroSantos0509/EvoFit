package com.evofit.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Helpers para montar diálogos de formulário, confirmação e lista sem precisar de um XML para cada um. */
public final class FormDialog {
    private FormDialog() {}

    public static final int TEXTO = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_SENTENCES;
    public static final int NOME = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS;
    public static final int EMAIL = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
    public static final int SENHA = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
    public static final int DECIMAL = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
    public static final int INTEIRO = InputType.TYPE_CLASS_NUMBER;
    public static final int DATA = InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE;

    /** Retorna true para fechar o diálogo; false para mantê-lo aberto (ex.: erro de validação). */
    public interface Resultado {
        boolean aoConfirmar(String[] valores);
    }

    public static void formulario(Context ctx, String titulo, String[] hints, int[] tipos,
                                  String[] iniciais, String textoOk, Resultado cb) {
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(ctx).setTitle(titulo);
        Context tc = b.getContext();
        int pad = (int) (20 * tc.getResources().getDisplayMetrics().density);

        LinearLayout box = new LinearLayout(tc);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);
        final TextInputEditText[] campos = new TextInputEditText[hints.length];
        for (int i = 0; i < hints.length; i++) {
            TextInputLayout til = new TextInputLayout(tc);
            til.setHint(hints[i]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = pad / 4;
            TextInputEditText et = new TextInputEditText(til.getContext());
            et.setInputType(tipos[i]);
            et.setSingleLine(true);
            if (iniciais != null && iniciais[i] != null) et.setText(iniciais[i]);
            til.addView(et);
            box.addView(til, lp);
            campos[i] = et;
        }
        ScrollView sv = new ScrollView(tc);
        sv.addView(box);
        b.setView(sv);
        b.setNegativeButton("Cancelar", null);
        b.setPositiveButton(textoOk, null);
        final AlertDialog d = b.create();
        d.show();
        d.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String[] valores = new String[campos.length];
            for (int i = 0; i < campos.length; i++) {
                valores[i] = campos[i].getText() == null ? "" : campos[i].getText().toString().trim();
            }
            if (cb.aoConfirmar(valores)) d.dismiss();
        });
    }

    public static void confirmar(Context ctx, String titulo, String mensagem, Runnable aoConfirmar) {
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Confirmar", (d, w) -> aoConfirmar.run())
                .show();
    }

    public static void lista(Context ctx, String titulo, String[] itens, DialogInterface.OnClickListener aoEscolher) {
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(titulo)
                .setItems(itens, aoEscolher)
                .setNegativeButton("Fechar", null)
                .show();
    }

    public static void aviso(Context ctx, String titulo, String mensagem) {
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setPositiveButton("OK", null)
                .show();
    }

    public static void toast(Context ctx, String texto) {
        Toast.makeText(ctx, texto, Toast.LENGTH_SHORT).show();
    }

    public static void abrirLink(Context ctx, String url) {
        if (url == null || url.isEmpty()) {
            toast(ctx, "Este exercício não tem vídeo cadastrado.");
            return;
        }
        try {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            toast(ctx, "Não foi possível abrir o link.");
        }
    }

    public static boolean emailValido(String s) {
        return s != null && android.util.Patterns.EMAIL_ADDRESS.matcher(s).matches();
    }

    public static String soDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    /** Validação do CPF (dígitos verificadores). */
    public static boolean cpfValido(String cpf) {
        String c = soDigitos(cpf);
        if (c.length() != 11 || c.matches("(\\d)\\1{10}")) return false;
        for (int t = 9; t < 11; t++) {
            int soma = 0;
            for (int i = 0; i < t; i++) soma += (c.charAt(i) - '0') * (t + 1 - i);
            int dv = (soma * 10) % 11 % 10;
            if (dv != c.charAt(t) - '0') return false;
        }
        return true;
    }

    static void esconder(View v) { v.setVisibility(View.GONE); }
}
