CREATE TABLE IF NOT EXISTS usuarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome_completo TEXT NOT NULL,
    cpf TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL,
    cargo TEXT NOT NULL,
    login TEXT NOT NULL UNIQUE,
    senha TEXT NOT NULL,
    perfil TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email);

CREATE TABLE IF NOT EXISTS projetos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    descricao TEXT NOT NULL,
    data_inicio TEXT NOT NULL,
    data_termino_prevista TEXT NOT NULL,
    status TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS equipes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    descricao TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS tarefas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    titulo TEXT NOT NULL,
    descricao TEXT NOT NULL,
    projeto_id INTEGER NOT NULL,
    responsavel_id INTEGER NOT NULL,
    data_inicio TEXT NOT NULL,
    data_termino_prevista TEXT NOT NULL,
    status TEXT NOT NULL,

    FOREIGN KEY (projeto_id) REFERENCES projetos(id),
    FOREIGN KEY (responsavel_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS equipe_usuario (
    equipe_id INTEGER NOT NULL,
    usuario_id INTEGER NOT NULL,

    PRIMARY KEY (equipe_id, usuario_id),

    FOREIGN KEY (equipe_id) REFERENCES equipes(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS projeto_equipe (
    projeto_id INTEGER NOT NULL,
    equipe_id INTEGER NOT NULL,

    PRIMARY KEY (projeto_id, equipe_id),

    FOREIGN KEY (projeto_id) REFERENCES projetos(id),
    FOREIGN KEY (equipe_id) REFERENCES equipes(id)
);
