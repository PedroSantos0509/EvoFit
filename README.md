# EvoFit — Sua evolução, nosso foco

Aplicativo móvel de **consultoria online para Personal Trainers e alunos**, que reúne em uma única plataforma a gestão de alunos, treinos, evolução corporal e mensalidades.

Projeto acadêmico desenvolvido no curso de Análise e Desenvolvimento de Sistemas da Universidade Cidade de São Paulo (UNICID).

---

## Sumário

- [Contexto e problema](#contexto-e-problema)
- [Objetivos](#objetivos)
- [Proposta](#proposta)
- [Benefícios](#benefícios)
- [Funcionalidades](#funcionalidades)
- [Perfis de usuário](#perfis-de-usuário)
- [Regras de negócio](#regras-de-negócio)
- [Requisitos não funcionais](#requisitos-não-funcionais)
- [Modelo de dados](#modelo-de-dados)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [Escopo](#escopo)
- [Metodologia: Spec-Driven Development](#metodologia-spec-driven-development)
- [Como executar o protótipo](#como-executar-o-protótipo)
- [Decisões em aberto](#decisões-em-aberto)
- [Integrantes](#integrantes)

---

## Contexto e problema

O sedentarismo é um dos principais fatores de risco para a mortalidade precoce e para doenças crônicas, como hipertensão, diabetes tipo 2 e distúrbios cardiovasculares, além de estar associado a ansiedade, estresse e depressão. A consultoria online amplia o acesso à prática orientada, mas exige ferramentas adequadas ao profissional.

Hoje, o acompanhamento costuma sofrer com quatro problemas:

1. **Baixo engajamento:** poucas ferramentas acessíveis para incentivar a prática constante.
2. **Gestão dispersa:** prescrições, periodizações e evolução corporal sem centralização.
3. **Controles manuais:** planilhas e fichas dificultam consultas e comparações entre períodos.
4. **Financeiro sem visão:** mensalidades e inadimplência sem lembretes automáticos nem visão consolidada.

## Objetivos

**Objetivo geral:** centralizar em um aplicativo móvel a gestão de alunos, treinos, evolução corporal e mensalidades do Personal Trainer, e o acompanhamento do aluno.

**Objetivos específicos:**

- Descomplicar o acompanhamento de alunos em uma plataforma móvel centralizada.
- Substituir planilhas manuais, fichas em papel e mensagens dispersas.
- Otimizar a rotina para permitir a gestão de mais alunos simultaneamente.

## Proposta

O EvoFit organiza a consultoria em quatro frentes:

| Frente | O que oferece |
|---|---|
| **Alunos** | Cadastro, dados corporais e vínculo com o personal |
| **Treinos** | Fichas A/B/C, periodização e vídeos demonstrativos |
| **Evolução** | Peso, cargas, fotos, gráficos e comparação de períodos |
| **Mensalidades** | Situação de pagamento, lembretes e restrição de acesso |

> O pagamento em si acontece fora do aplicativo. O EvoFit apenas registra e acompanha a situação das mensalidades.

## Benefícios

**Para o Personal Trainer**

- Prescrição ágil, com treinos em fichas A, B e C e periodização por ciclos.
- Clareza dos resultados, com histórico e gráficos de peso e cargas.
- Comparação de períodos (peso, cargas e fotos lado a lado).
- Controle financeiro, com dashboard de alunos ativos, atrasos e vencimentos próximos.
- Escalabilidade para atender mais alunos ao mesmo tempo.

**Para o aluno**

- Acesso ao treino vigente pelo celular, em qualquer lugar.
- Execução mais segura, com vídeos demonstrativos.
- Evolução visível, com histórico de cargas, medidas e fotos.
- Constância, com rotinas orientadas e lembretes que combatem o sedentarismo.

## Funcionalidades

O catálogo tem **26 requisitos funcionais (RF01–RF26)** em 8 áreas:

| Área | Requisitos | Descrição |
|---|---|---|
| Acesso | RF01–RF02 | Login, perfis e recuperação de senha |
| Gestão de alunos | RF03–RF04 | Cadastro e dados corporais |
| Exercícios | RF05–RF08 | Biblioteca, grupos musculares e vídeos |
| Treinos | RF09–RF14 | Fichas, periodização e duração |
| Evolução | RF15–RF18 | Histórico, gráficos e comparação |
| Perfil do aluno | RF19–RF20 | Foto de perfil e objetivo pessoal |
| Pagamentos | RF21–RF23 | Mensalidades, lembretes e bloqueio |
| Painel e gestão | RF24–RF26 | Dashboard, calendário e manutenção |

O modelo funcional descreve casos de uso por ator: **11 do Personal Trainer** e **7 do Aluno**, cada um com pré-condição, fluxo normal, exceções e pós-condição.

## Perfis de usuário

| Perfil | Responsabilidades | Restrições |
|---|---|---|
| **Personal Trainer** | Cadastra alunos, prescreve e periodiza treinos, gerencia exercícios | Único perfil que cria ou altera treinos |
| **Aluno** | Consulta o treino vigente, registra a duração e acompanha a evolução | Não altera treinos nem exclui o histórico |
| **Conta Google** (ator externo) | Autentica a identidade no login | Integração limitada à autenticação |

## Regras de negócio

| Código | Regra |
|---|---|
| RN001 | Só o Personal Trainer autenticado prescreve e altera treinos. |
| RN002 | O aluno só vê e registra execuções nos treinos vigentes da sua conta. |
| RN003 | Todo exercício novo pode receber vídeo demonstrativo. |
| RN004 | Alunos inadimplentes podem ter o acesso restrito até a regularização. |
| RN005 | Lembretes de vencimento são enviados automaticamente. |
| RN006 | O aluno não pode excluir avaliações nem relatórios de evolução. |
| RN008 | Todo exercício pertence a pelo menos um grupo muscular. |
| RN009 | Qualquer acesso exige login com credenciais válidas e ativas. |

## Requisitos não funcionais

- **Conformidade:** tratamento de dados pessoais segundo a LGPD (Lei nº 13.709/2018).
- **Segurança de acesso:** autenticação por usuário e senha, com funções por perfil.
- **Disponibilidade:** treinos e histórico acessíveis sem interrupções desnecessárias.
- **Recuperabilidade:** cópias de segurança periódicas do banco de dados.
- **Usabilidade:** interface simples, intuitiva e com menus claros.
- **Plataforma:** aplicativo para smartphones, com implantação gradual.

## Modelo de dados

Modelo relacional com **15 tabelas**, ligadas por chaves primárias e estrangeiras. Fotos e vídeos ficam no armazenamento de arquivos; o banco guarda apenas o caminho.

| Grupo | Tabelas |
|---|---|
| Usuários e perfis | `Usuario`, `PersonalTrainer`, `Aluno` |
| Treinos e exercícios | `Treino`, `FichaTreino`, `ItemTreino`, `Exercicio`, `GrupoMuscular`, `ExercicioGrupo` |
| Evolução do aluno | `SessaoTreino`, `RegistroEvolucao`, `FotoEvolucao`, `RegistroCarga` |
| Mensalidades | `Mensalidade`, `LembretePagamento` |

A relação entre exercício e grupo muscular é muitos-para-muitos, resolvida pela tabela associativa `ExercicioGrupo`.

## Arquitetura

Arquitetura **cliente-servidor**:

```
App móvel  ──►  API / Backend  ──►  Banco de dados
                    │
                    ├── Armazenamento de arquivos (fotos de evolução e mídias)
                    └── Serviço de notificações (lembretes de mensalidade)
```

A comunicação entre o aplicativo e o backend ocorre por HTTPS/TLS. As senhas são armazenadas como hash.

## Tecnologias

| Item | Tecnologia |
|---|---|
| Ambiente | Android Studio, Java e Gradle |
| Estrutura | Pacotes `model`, `dao`, `ui` e `util` |
| Dados | Scripts SQL, hash SHA-256 e sessões ativas |
| Interface | Fragments (Treinos, Perfil, Calendário) e layouts XML |
| Login | Integração preparada com a Conta Google |

**Telas:** Login, Meu Perfil, Alunos, Treinos e Calendário, com identidade escura e destaque em verde.

> A escolha definitiva de banco de dados, framework e nuvem ainda precisa ser validada. O PostgreSQL aparece na documentação apenas como referência.

## Escopo

**Incluído nesta versão**

- Login por e-mail, CPF ou conta Google
- Gestão de alunos e dados corporais
- Biblioteca de exercícios com vídeos
- Treinos em fichas A/B/C e periodização
- Evolução, gráficos e comparação
- Mensalidades, lembretes e restrição de acesso

**Fora de escopo**

- Pagamento dentro do aplicativo
- Integração com relógios e pulseiras inteligentes
- Prescrição nutricional
- Videochamadas e inteligência artificial
- Integração com academias
- Versão para computador ou navegador

## Metodologia: Spec-Driven Development

O projeto segue o **Spec-Driven Development (SDD)**, em que a especificação é a referência central para planejar, implementar, testar e revisar. O ciclo tem nove etapas:

1. Descoberta
2. Especificação
3. Critérios de aceite
4. Revisão
5. Planejamento
6. Implementação
7. Validação
8. Entrega
9. Evolução

**Qualidade e rastreabilidade:** a estratégia prevê 16 cenários de teste mínimos (TST-01 a TST-16), em 6 tipos (unitário, integração, API e autorização, sistema, regressão e não funcional), com uma matriz que liga requisitos, regras de negócio, casos de uso e testes. Cada requisito implementado exige ao menos um teste positivo, um negativo quando aplicável e evidência de autorização por perfil.

## Como executar o protótipo

1. Instale o [Android Studio](https://developer.android.com/studio).
2. Clone este repositório:
   ```bash
   git clone https://github.com/<usuario>/<repositorio>.git
   ```
3. Abra a pasta do projeto no Android Studio e aguarde a sincronização do Gradle.
4. Execute os scripts SQL do projeto no banco de dados configurado.
5. Rode o app em um emulador ou em um dispositivo Android.

> Ajuste os passos 2 e 4 conforme a estrutura real do repositório.

## Decisões em aberto

A documentação lista 12 pendências (D-01 a D-12). As principais:

- **D-01:** confirmar o escopo de RF14 e RF23, ainda não revisados.
- **D-02:** definir o fluxo técnico do login com Google.
- **D-03:** definir carência e regras de bloqueio por inadimplência.
- **D-04:** definir canal e frequência dos lembretes de pagamento.
- **D-08:** conciliar a exclusão de alunos com a retenção do histórico.
- **D-10:** registrar a decisão de linguagem, nuvem e banco de dados.

## Integrantes

- Antonny Neres Brentan
- Danilo Evangelista Nunes
- Duquesnio Daniel Bandessa
- Giovana Paixão Melo Morgado
- Gustavo Miranda Amado
- Higor Rhuan Costa Santos
- Pedro Augusto dos Santos da Silva
- Pedro Henrique Santos Peluco
- Victor Eduardo Pedro de Oliveira

---

Projeto acadêmico · Universidade Cidade de São Paulo · 2026
