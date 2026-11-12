package com.evofit.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.evofit.R;
import com.evofit.dao.UsuarioDAO;
import com.evofit.model.Usuario;
import com.evofit.util.SessaoUsuario;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Container das telas Perfil, Alunos, Treinos e Calendário (Figuras 5 a 8). */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Usuario u = new UsuarioDAO(this).buscarPorId(SessaoUsuario.idLogado(this));
        if (u == null) {
            sairParaLogin();
            return;
        }
        setContentView(R.layout.activity_main);

        nav = findViewById(R.id.bottomNav);
        // Aluno não gerencia outros alunos.
        nav.getMenu().findItem(R.id.nav_alunos).setVisible(u.isPersonal());

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment f;
            if (id == R.id.nav_alunos) f = new AlunosFragment();
            else if (id == R.id.nav_treinos) f = new TreinosFragment();
            else if (id == R.id.nav_calendario) f = new CalendarioFragment();
            else f = new PerfilFragment();
            mostrar(f);
            return true;
        });

        if (savedInstanceState == null) nav.setSelectedItemId(R.id.nav_perfil);
    }

    private void mostrar(@NonNull Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, f)
                .commit();
    }

    private void sairParaLogin() {
        SessaoUsuario.sair(this);
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    /** Chamado pelo Perfil: encerra a sessão (inclusive a conta Google) e volta ao login. */
    public void sair() {
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this,
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
        sairParaLogin();
    }
}
