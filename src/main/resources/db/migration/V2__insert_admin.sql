-- =========================================================================
-- V2__inserir_usuario_admin.sql
-- Insere um usuário administrador padrão para testes da equipe
-- =========================================================================

INSERT INTO public.usuario (
    nome,
    email,
    senha_hash,
    telefone,
    data_nascimento,
    tipo_usuario,
    bloqueado,
    quantidade_emprestada
)
VALUES (
           'Administrador Littera',
           'admin@littera.com',
           '$2a$12$37T..v/naOvDaBwl9Sc9Me5qJS7ryCALhRIB5gW9dk/OxocwiVDI6', -- Substitua por um hash BCrypt válido da senha deles
           '51999999999',
           '1995-05-19',
           'ADMIN',
           FALSE,
           0
       );

