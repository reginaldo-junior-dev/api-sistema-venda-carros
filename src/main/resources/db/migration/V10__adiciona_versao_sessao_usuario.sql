-- Cada token leva a versão da sessão do usuário; aumentar a versão invalida todos os tokens emitidos antes
-- (sair, trocar a senha, login Google descartar a senha não confirmada)
ALTER TABLE usuario ADD COLUMN versao_sessao INTEGER NOT NULL DEFAULT 0;
