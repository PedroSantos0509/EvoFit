-- =====================================================================
-- EvoFit | Script para MySQL Workbench (MySQL 8.0.16+)
-- Espelha o banco SQLite que o app usa (ConnectionFactory.java), com os
-- nomes de tabelas/campos do documento. Serve para modelagem, EER
-- (Database > Reverse Engineer) e para o trabalho de BD. O app NÃO se
-- conecta a este MySQL: ele usa o SQLite interno do celular.
-- Como usar: File > Open SQL Script > Executar (raio).
-- =====================================================================
DROP DATABASE IF EXISTS evofit;
CREATE DATABASE evofit CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE evofit;

CREATE TABLE usuario (
    id_usuario      BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(120) NOT NULL,
    email           VARCHAR(160) NOT NULL UNIQUE,
    cpf             CHAR(11) UNIQUE,
    senha_hash      VARCHAR(255),
    foto_perfil     TEXT,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    tipo_usuario    VARCHAR(20) NOT NULL,
    data_nascimento DATE,
    altura_m        DECIMAL(4,2),
    peso_kg         DECIMAL(6,2),
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_usuario_tipo CHECK (tipo_usuario IN ('PERSONAL','ALUNO','EQUIPE_TI')),
    CONSTRAINT ck_usuario_altura CHECK (altura_m IS NULL OR altura_m > 0),
    CONSTRAINT ck_usuario_peso CHECK (peso_kg IS NULL OR peso_kg > 0)
) ENGINE=InnoDB;

CREATE TABLE personal_trainer (
    id_usuario BIGINT PRIMARY KEY,
    cref       VARCHAR(30),
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE equipe_ti (
    id_usuario BIGINT PRIMARY KEY,
    matricula  VARCHAR(30),
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE ficha_treino (
    id_ficha    BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_personal BIGINT NOT NULL,
    nome        VARCHAR(40) NOT NULL,
    foco        VARCHAR(80),
    resumo      VARCHAR(200),
    FOREIGN KEY (id_personal) REFERENCES personal_trainer(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE aluno (
    id_usuario      BIGINT PRIMARY KEY,
    id_personal     BIGINT NOT NULL,
    objetivo        VARCHAR(120),
    data_inicio     DATE NOT NULL,
    situacao_acesso VARCHAR(15) NOT NULL DEFAULT 'ATIVO',
    id_ficha_atual  BIGINT NULL,
    CONSTRAINT ck_aluno_situacao CHECK (situacao_acesso IN ('ATIVO','RESTRITO','INATIVO')),
    FOREIGN KEY (id_usuario)     REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (id_personal)    REFERENCES personal_trainer(id_usuario),
    FOREIGN KEY (id_ficha_atual) REFERENCES ficha_treino(id_ficha) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE grupo_muscular (
    id_grupo BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome     VARCHAR(80) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE exercicio (
    id_exercicio       BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome               VARCHAR(120) NOT NULL,
    instrucao_execucao TEXT,
    url_video          TEXT,
    url_imagem         TEXT
) ENGINE=InnoDB;

CREATE TABLE exercicio_grupo (
    id_exercicio BIGINT NOT NULL,
    id_grupo     BIGINT NOT NULL,
    PRIMARY KEY (id_exercicio, id_grupo),
    FOREIGN KEY (id_exercicio) REFERENCES exercicio(id_exercicio) ON DELETE CASCADE,
    FOREIGN KEY (id_grupo)     REFERENCES grupo_muscular(id_grupo)
) ENGINE=InnoDB;

CREATE TABLE item_treino (
    id_item      BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_ficha     BIGINT NOT NULL,
    id_exercicio BIGINT NOT NULL,
    ordem        INT NOT NULL,
    series       SMALLINT NOT NULL,
    repeticoes   VARCHAR(30) NOT NULL,
    carga_kg     DECIMAL(6,2),
    descanso_seg SMALLINT,
    bi_set       BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_item_ordem CHECK (ordem > 0),
    CONSTRAINT ck_item_series CHECK (series > 0),
    CONSTRAINT ck_item_carga CHECK (carga_kg IS NULL OR carga_kg >= 0),
    CONSTRAINT ck_item_descanso CHECK (descanso_seg IS NULL OR descanso_seg >= 0),
    FOREIGN KEY (id_ficha)     REFERENCES ficha_treino(id_ficha) ON DELETE CASCADE,
    FOREIGN KEY (id_exercicio) REFERENCES exercicio(id_exercicio)
) ENGINE=InnoDB;

-- Treino programado (PROGRAMADA) ou realizado (REALIZADA) no calendário.
CREATE TABLE sessao_treino (
    id_sessao       BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_aluno        BIGINT NOT NULL,
    id_ficha        BIGINT NULL,
    data_treino     DATE NOT NULL,
    duracao_minutos INT,
    situacao        VARCHAR(12) NOT NULL DEFAULT 'PROGRAMADA',
    CONSTRAINT ck_sessao_situacao CHECK (situacao IN ('PROGRAMADA','REALIZADA')),
    CONSTRAINT ck_sessao_duracao CHECK (duracao_minutos IS NULL OR duracao_minutos > 0),
    FOREIGN KEY (id_aluno) REFERENCES aluno(id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (id_ficha) REFERENCES ficha_treino(id_ficha) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE registro_evolucao (
    id_registro        BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_aluno           BIGINT NOT NULL,
    data_registro      DATE NOT NULL,
    peso_kg            DECIMAL(6,2),
    mudancas_corporais TEXT,
    CONSTRAINT ck_evolucao_peso CHECK (peso_kg IS NULL OR peso_kg > 0),
    FOREIGN KEY (id_aluno) REFERENCES aluno(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE foto_evolucao (
    id_foto         BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_registro     BIGINT NOT NULL,
    data_foto       DATE NOT NULL,
    caminho_arquivo TEXT NOT NULL,
    FOREIGN KEY (id_registro) REFERENCES registro_evolucao(id_registro) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE registro_carga (
    id_registro_carga BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_aluno          BIGINT NOT NULL,
    id_exercicio      BIGINT NOT NULL,
    id_sessao         BIGINT NULL,
    data_registro     DATE NOT NULL,
    carga_kg          DECIMAL(7,2) NOT NULL,
    CONSTRAINT ck_carga CHECK (carga_kg >= 0),
    FOREIGN KEY (id_aluno)     REFERENCES aluno(id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (id_exercicio) REFERENCES exercicio(id_exercicio),
    FOREIGN KEY (id_sessao)    REFERENCES sessao_treino(id_sessao) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE mensalidade (
    id_mensalidade  BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_aluno        BIGINT NOT NULL,
    valor           DECIMAL(10,2) NOT NULL,
    data_vencimento DATE NOT NULL,
    data_pagamento  DATE,
    situacao        VARCHAR(15) NOT NULL DEFAULT 'PENDENTE',
    CONSTRAINT ck_mensalidade_valor CHECK (valor >= 0),
    CONSTRAINT ck_mensalidade_situacao CHECK (situacao IN ('PENDENTE','PAGA','ATRASADA')),
    UNIQUE (id_aluno, data_vencimento),
    FOREIGN KEY (id_aluno) REFERENCES aluno(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE lembrete_pagamento (
    id_lembrete    BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_mensalidade BIGINT NOT NULL,
    data_envio     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mensagem       TEXT NOT NULL,
    FOREIGN KEY (id_mensalidade) REFERENCES mensalidade(id_mensalidade) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_sessao_aluno_data     ON sessao_treino(id_aluno, data_treino);
CREATE INDEX idx_evolucao_aluno_data   ON registro_evolucao(id_aluno, data_registro);
CREATE INDEX idx_mensalidade_aluno_venc ON mensalidade(id_aluno, data_vencimento);

-- Dados iniciais
INSERT INTO grupo_muscular (nome) VALUES
 ('Peito'),('Costas'),('Pernas'),('Ombros'),('Bíceps'),('Tríceps'),('Abdômen'),('Cardio'),('Mobilidade');

INSERT INTO exercicio (nome) VALUES
 ('Agachamento livre'),('Leg press 45°'),('Cadeira extensora'),('Mesa flexora'),('Panturrilha em pé'),
 ('Esteira (cardio)'),('Bicicleta ergométrica'),('Supino reto'),('Remada curvada'),('Puxada frontal'),
 ('Desenvolvimento com halteres'),('Rosca direta'),('Tríceps corda'),('Abdominal infra'),('Prancha'),
 ('Alongamento global'),('Mobilidade de quadril'),('Agachamento com salto');
