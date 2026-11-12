# EvoFit – Projeto Android (Java + XML)

App de consultoria online para Personal Trainer ("Sua evolução, nosso foco").
Telas das Figuras 4 a 8 do documento: **Login, Meu Perfil, Alunos, Treinos e Calendário**.

## 1. Abrir no Android Studio
1. Extraia o ZIP.
2. Android Studio > **File > Open** > selecione a pasta `EvoFit` (a que tem o `settings.gradle`).
3. Aguarde o **Gradle Sync** (precisa de internet na primeira vez). Se pedir, aceite instalar o SDK 34.
4. Rode (▶) em um emulador (Pixel, API 30+) ou celular com depuração USB.

> Notebook fraco: use celular físico em vez de emulador, feche outros programas e, em
> *Settings > Build > Compiler*, ative "Compile independent modules in parallel".
> Se faltar memória, em `gradle.properties` deixe `org.gradle.jvmargs=-Xmx1024m`.

## 2. Acessos de teste (já criados no primeiro uso)
| Perfil   | Login                  | Senha  |
|----------|------------------------|--------|
| Personal | victor@evofit.com      | 123456 |
| Aluno    | joao@evofit.com        | 123456 |
(também entram pelo CPF: Victor 529.982.247-25 · João 111.444.777-35)

Para zerar os dados de exemplo: desinstale o app (ou *Device Explorer > Clear data*).

## 3. Estrutura de packages (`com.evofit`)
```
model     Usuario, Aluno, FichaTreino, ItemTreino, Exercicio, GrupoMuscular,
          SessaoTreino, RegistroEvolucao, Mensalidade
dao       UsuarioDAO, AlunoDAO, FichaTreinoDAO, SessaoTreinoDAO,
          RegistroEvolucaoDAO, MensalidadeDAO
database  ConnectionFactory  (cria o SQLite + dados de exemplo)
ui        LoginActivity, MainActivity, PerfilFragment, AlunosFragment,
          AlunoAcoes, TreinosFragment, CalendarioFragment, FormDialog
util      Seguranca (hash de senha), SessaoUsuario, Datas, Imagens
```
Layouts em `app/src/main/res/layout`: `activity_login`, `activity_main`, `fragment_perfil`,
`fragment_alunos` (+`item_aluno`), `fragment_treinos` (+`item_ficha`), `fragment_calendario` (+`item_dia`).

## 4. Banco de dados
* O app usa **SQLite interno** (arquivo `evofit.db` no celular) – funciona sem servidor.
* `sql/evofit_mysql.sql` é o **mesmo modelo para o MySQL Workbench** (modelagem/EER/entrega
  do trabalho). Abra no Workbench e execute. O app **não** se conecta a esse MySQL.
* Diferenças em relação ao PDF (para simplificar): `treino` foi fundido em `ficha_treino`
  (o aluno aponta para a ficha atual), `sessao_treino` guarda treino programado/realizado
  (calendário) e altura/peso/nascimento ficam em `usuario`.

## 5. O que cada tela faz
* **Login**: e-mail ou CPF + senha, olho para mostrar senha, "Esqueceu sua senha?" (e-mail + CPF + nova
  senha), "Cadastre-se" (cria conta de Personal), Entrar com Google.
* **Meu Perfil**: foto (toque para trocar), dados (altura/peso/idade editáveis), total e novos alunos,
  barra de atividade (% de alunos que treinaram nos últimos 7 dias). Ícones inferiores: editar dados,
  alterar senha, lembretes de mensalidade, sair. Aluno só altera foto e objetivo (RF19/RF20).
* **Alunos**: lista com busca, **+** cadastra; toque no aluno: editar, atribuir treino, evolução
  corporal, mensalidades, situação de acesso, excluir. "Pendente" = aluno sem treino.
* **Treinos**: fichas A/B/C; toque para ver exercícios (vídeo, remover), **+ Exercício**, editar/excluir
  ficha; **+** cria nova ficha. Exercícios novos exigem grupo muscular (RN008).
* **Calendário**: mês com halter (treino), corredor (cardio/mobilidade) e ✓ (realizado); setas mudam o
  mês; toque no dia: programar treino, marcar como realizado (registra duração) ou excluir.

## 6. Login com Google (precisa configurar uma vez)
O código já está pronto (`play-services-auth`). Para funcionar de verdade:
1. https://console.cloud.google.com > crie um projeto > **APIs e serviços > Tela de permissão OAuth**.
2. **Credenciais > Criar credenciais > ID do cliente OAuth > Android**:
   pacote `com.evofit` e o **SHA-1** da sua máquina (Android Studio: aba *Gradle* >
   `app > Tasks > android > signingReport`).
3. Rode o app de novo. Sem isso o botão mostra o aviso "Login com Google indisponível" (erro 10/12500).
O primeiro acesso com uma conta Google nova cria um perfil de Personal. O "G" do botão é um
marcador: troque pelo ícone oficial seguindo as diretrizes de marca do Google.

## 7. Observações
* Segurança: senhas guardadas com SHA-256 + salt; sem pagamentos no app (só registro de mensalidade).
* Fora do escopo (como no documento): pagamento no app, nutrição, vídeo-chamada, IA, wearables.
