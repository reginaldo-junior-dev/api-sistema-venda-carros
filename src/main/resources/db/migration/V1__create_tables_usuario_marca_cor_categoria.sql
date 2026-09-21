CREATE TABLE usuario (
    id UUID PRIMARY KEY,
    nome_completo VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    senha VARCHAR(255),
    perfil VARCHAR(30) NOT NULL,
    provedor VARCHAR(30) NOT NULL,
    data_criacao TIMESTAMP NOT NULL
);

CREATE TABLE marca (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    CONSTRAINT uk_marca_nome UNIQUE (nome)
);

CREATE TABLE cor (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    CONSTRAINT uk_cor_nome UNIQUE (nome)
);

CREATE TABLE categoria (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    CONSTRAINT uk_categoria_nome UNIQUE (nome)
);
