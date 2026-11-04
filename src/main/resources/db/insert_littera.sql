-- 0. Limpa as tabelas e reinicia os contadores de ID (Sequências) do zero
TRUNCATE TABLE emprestimo, reserva, livro, revista, item_acervo, usuario RESTART IDENTITY CASCADE;

-- 1. Inserir todos os 20 itens no acervo (IDs gerados automaticamente de 1 a 20)
INSERT INTO item_acervo (titulo, ano, tipo, localizacao) VALUES
-- Revistas (IDs 1 a 10)
('Super Interessante', 2026, 'REVISTA', 'Setor A - Prateleira 1'),
('Super Interessante', 2026, 'REVISTA', 'Setor A - Prateleira 1'),
('Super Interessante', 2026, 'REVISTA', 'Setor A - Prateleira 1'),
('Recreio', 2018, 'REVISTA', 'Setor A - Prateleira 2'),
('Recreio', 2017, 'REVISTA', 'Setor A - Prateleira 2'),
('Recreio', 2017, 'REVISTA', 'Setor A - Prateleira 2'),
('Veja', 2026, 'REVISTA', 'Setor A - Prateleira 3'),
('Mundo Estranho', 2018, 'REVISTA', 'Setor A - Prateleira 4'),
('Mundo Estranho', 2014, 'REVISTA', 'Setor A - Prateleira 4'),
('Mundo Estranho', 2012, 'REVISTA', 'Setor A - Prateleira 4'),
-- Livros (IDs 11 a 20)
('Crepúsculo', 2005, 'LIVRO', 'Setor B - Prateleira 1'),
('O Código Da Vinci', 2006, 'LIVRO', 'Setor B - Prateleira 2'),
('Inferno', 2013, 'LIVRO', 'Setor B - Prateleira 2'),
('A Empregada', 2023, 'LIVRO', 'Setor B - Prateleira 3'),
('A Hospedeira', 2009, 'LIVRO', 'Setor B - Prateleira 1'),
('Extraordinário', 2013, 'LIVRO', 'Setor B - Prateleira 4'),
('Eu', 2010, 'LIVRO', 'Setor B - Prateleira 5'),
('Grey', 2015, 'LIVRO', 'Setor B - Prateleira 1'),
('Giane: Vida Arte e Luta', 2012, 'LIVRO', 'Setor B - Prateleira 5'),
('Bom Dia, Verônica', 2022, 'LIVRO', 'Setor B - Prateleira 3');

-- 2. Inserir os dados específicos das Revistas (vinculadas exatamente aos códigos de 1 a 10)
INSERT INTO revista (codigo, edicao, issn, mes_ano_publicacao) VALUES
(1, '491', '0104-1789', '09/2026'),
(2, '485', '0104-1789', '03/2026'),
(3, '490', '0104-1789', '08/2026'),
(4, '925', '1517-7467', '10/2018'),
(5, '920', '1517-7467', '10/2017'),
(6, '900', '1517-7467', '06/2017'),
(7, '3014', '0100-7122', '09/2026'),
(8, '212', '1676-9554', '08/2018'),
(9, '151', '1676-9554', '04/2014'),
(10, '130', '1676-9554', '10/2012');

-- 3. Inserir os dados específicos dos Livros (vinculados exatamente aos códigos de 11 a 20)
INSERT INTO livro (codigo, autor, isbn, genero, total_exemplares, total_disponivel, total_emprestados) VALUES
(11, 'Stephenie Meyer', '978-8598078304', 'Fantasia', 1, 1, 0),
(12, 'Dan Brown', '978-8575421130', 'Suspense', 1, 1, 0),
(13, 'Dan Brown', '978-8580411522', 'Suspense', 1, 1, 0),
(14, 'Freida McFadden', '978-6555655063', 'Suspense', 1, 1, 0),
(15, 'Stephenie Meyer', '978-8598078595', 'Suspense', 1, 1, 0),
(16, 'R. J. Palacio', '978-8580573015', 'Infanto-juvenil', 1, 1, 0),
(17, 'Ricky Martin', '978-8576655633', 'Biografia', 1, 1, 0),
(18, 'E. L. James', '978-8580577730', 'Erótico', 1, 1, 0),
(19, 'Guilherme Fiuza', '978-8575428771', 'Biografia', 1, 1, 0),
(20, 'Raphael Montes', '978-6559211043', 'Suspense', 1, 1, 0);

-- 4. Inserir os usuários
INSERT INTO usuario (nome, telefone, data_nascimento, quantidade_emprestada, email) VALUES
('Maria Silva', '51999999999', '1995-01-01', 0, 'maria@email.com'),
('João Santos', '51988888888', '1995-05-19', 0, 'joao@email.com'),
('Marta Rocha', '51789363018', '1995-07-22', 0, 'marta@email.com'),
('Juarez Costa', '48989898989', '1975-02-06', 0, 'juarez@email.com'),
('Marcela Silva', '519919382197', '1985-10-05', 0, 'marcela@email.com');

-- ADMIN

INSERT INTO usuario (nome, telefone, data_nascimento, tipo_usuario, quantidade_emprestada, email) VALUES
('Lucas Brum', '51000001111', '1995-05-19', 'ADMIN', 0, 'lucas@email.com')

-- 5. Inserir os empréstimos
INSERT INTO emprestimo (codigo_usuario, codigo_item_acervo, data_emprestimo, data_devolucao) VALUES
(1, 1, '2026-01-09', '2026-01-15'),
(2, 2, '2026-01-09', '2026-01-15'),
(3, 3, '2026-01-09', '2026-01-15'),
(4, 10, '2026-01-09', '2026-01-15'),
(5, 11, '2026-01-09', '2026-01-15');

INSERT INTO reserva (usuario_id, item_codigo, data_reserva, status) VALUES
(1, 2, '2026-10-09', 'PENDENTE'),
(3, 4, '2026-10-09', 'CONCLUIDA');