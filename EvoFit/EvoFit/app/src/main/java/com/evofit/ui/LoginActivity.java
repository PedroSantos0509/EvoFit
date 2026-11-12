package com.evofit.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.evofit.R;
import com.evofit.dao.UsuarioDAO;
import com.evofit.model.Usuario;
import com.evofit.util.SessaoUsuario;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

/** Figura 4 - Tela de Login (e-mail/CPF + senha, recuperar senha, Google e cadastro). */
public class LoginActivity extends AppCompatActivity {

    private EditText edtLogin, edtSenha;
    private boolean senhaVisivel = false;
    private UsuarioDAO usuarioDAO;
    private GoogleSignInClient googleClient;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount conta = task.getResult(ApiException.class);
                    entrarComGoogle(conta);
                } catch (ApiException e) {
                    if (e.getStatusCode() == 12501) return; // usuário cancelou
                    FormDialog.aviso(this, "Login com Google indisponível",
                            "Código " + e.getStatusCode() + ".\n\nConfira no Google Cloud Console se existe um "
                                    + "\"ID do cliente OAuth\" do tipo Android com o pacote com.evofit e o SHA-1 "
                                    + "desta máquina (veja o LEIAME). Enquanto isso, use e-mail e senha.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usuarioDAO = new UsuarioDAO(this);

        // Já logado? Vai direto para o app.
        long id = SessaoUsuario.idLogado(this);
        if (id > 0 && usuarioDAO.buscarPorId(id) != null) {
            abrirPrincipal();
            return;
        }

        setContentView(R.layout.activity_login);
        edtLogin = findViewById(R.id.edtLogin);
        edtSenha = findViewById(R.id.edtSenha);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        ImageView btnVer = findViewById(R.id.btnVerSenha);
        btnVer.setOnClickListener(v -> {
            senhaVisivel = !senhaVisivel;
            edtSenha.setTransformationMethod(senhaVisivel
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            edtSenha.setSelection(edtSenha.getText().length());
            btnVer.setAlpha(senhaVisivel ? 1f : 0.6f);
        });
        btnVer.setAlpha(0.6f);

        findViewById(R.id.btnEntrar).setOnClickListener(v -> entrar());
        ((TextView) findViewById(R.id.txtEsqueceu)).setOnClickListener(v -> recuperarSenha());
        findViewById(R.id.txtCadastrar).setOnClickListener(v -> cadastrar());
        findViewById(R.id.btnGoogle).setOnClickListener(v -> {
            googleClient.signOut().addOnCompleteListener(t ->
                    googleLauncher.launch(googleClient.getSignInIntent())); // sempre pergunta qual conta usar
        });
    }

    private void entrar() {
        String login = edtLogin.getText().toString().trim();
        String senha = edtSenha.getText().toString();
        if (login.isEmpty() || senha.isEmpty()) {
            FormDialog.toast(this, "Informe e-mail ou CPF e a senha.");
            return;
        }
        Usuario u = usuarioDAO.autenticar(login, senha);
        if (u == null) {
            FormDialog.toast(this, "E-mail/CPF ou senha inválidos.");
            return;
        }
        SessaoUsuario.entrar(this, u.id);
        abrirPrincipal();
    }

    private void entrarComGoogle(GoogleSignInAccount conta) {
        String email = conta.getEmail();
        if (email == null) {
            FormDialog.toast(this, "A conta Google não retornou e-mail.");
            return;
        }
        Usuario u = usuarioDAO.buscarPorEmail(email);
        if (u == null) {
            // Primeiro acesso com Google: cria um perfil de Personal Trainer.
            String nome = conta.getDisplayName() != null ? conta.getDisplayName() : email;
            long id = usuarioDAO.cadastrarPersonal(nome, email, null, null, "");
            if (id == -1) {
                FormDialog.toast(this, "Não foi possível criar a conta.");
                return;
            }
            u = usuarioDAO.buscarPorId(id);
        }
        SessaoUsuario.entrar(this, u.id);
        abrirPrincipal();
    }

    private void recuperarSenha() {
        FormDialog.formulario(this, "Recuperar senha",
                new String[]{"E-mail cadastrado", "CPF", "Nova senha (mín. 6)"},
                new int[]{FormDialog.EMAIL, FormDialog.INTEIRO, FormDialog.SENHA},
                null, "Redefinir", v -> {
                    if (!FormDialog.emailValido(v[0]) || FormDialog.soDigitos(v[1]).length() != 11 || v[2].length() < 6) {
                        FormDialog.toast(this, "Confira e-mail, CPF (11 dígitos) e senha (mín. 6).");
                        return false;
                    }
                    if (usuarioDAO.redefinirSenha(v[0], v[1], v[2])) {
                        FormDialog.toast(this, "Senha redefinida! Entre com a nova senha.");
                        return true;
                    }
                    FormDialog.toast(this, "E-mail e CPF não conferem com nenhuma conta.");
                    return false;
                });
    }

    private void cadastrar() {
        FormDialog.formulario(this, "Criar conta de Personal",
                new String[]{"Nome completo", "E-mail", "CPF", "CREF", "Senha (mín. 6)"},
                new int[]{FormDialog.NOME, FormDialog.EMAIL, FormDialog.INTEIRO, FormDialog.TEXTO, FormDialog.SENHA},
                null, "Cadastrar", v -> {
                    if (v[0].length() < 3) { FormDialog.toast(this, "Informe o nome completo."); return false; }
                    if (!FormDialog.emailValido(v[1])) { FormDialog.toast(this, "E-mail inválido."); return false; }
                    if (!FormDialog.cpfValido(v[2])) { FormDialog.toast(this, "CPF inválido."); return false; }
                    if (v[4].length() < 6) { FormDialog.toast(this, "A senha precisa ter ao menos 6 caracteres."); return false; }
                    long id = usuarioDAO.cadastrarPersonal(v[0], v[1], FormDialog.soDigitos(v[2]), v[4], v[3]);
                    if (id == -1) {
                        FormDialog.toast(this, "E-mail ou CPF já cadastrado.");
                        return false;
                    }
                    SessaoUsuario.entrar(this, id);
                    abrirPrincipal();
                    return true;
                });
    }

    private void abrirPrincipal() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
