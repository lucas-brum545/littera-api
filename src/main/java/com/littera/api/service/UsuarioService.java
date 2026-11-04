package com.littera.api.service;

import com.littera.api.dto.RegistroRequest;
import com.littera.api.dto.UsuarioRequest;
import com.littera.api.dto.UsuarioResponse;
import com.littera.api.dto.UsuarioResumo;
import com.littera.api.exception.RecursoNaoEncontradoException;
import com.littera.api.model.TipoUsuario;
import com.littera.api.model.Usuario;
import com.littera.api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }
// pro caso de fazermos um sistema para o usuario acessar também
//    public void cadastrar(UsuarioCadastro usuarioCadastro){
//        if(usuarioRepository.existsByEmail(usuarioCadastro.email())) {
//            throw new IllegalArgumentException();
//        }
//
//        Usuario usuario = new Usuario();
//        usuario.setNome(usuarioCadastro.nome());
//        usuario.setEmail(usuarioCadastro.email());
//
//        usuario.setSenhaHash(passwordEncoder.encode(usuarioCadastro.senha()));
//        usuario.setTipoUsuario(TipoUsuario.LEITOR);
//
//        usuarioRepository.save(usuario);
//    }

    public List<UsuarioResumo> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResumo::fromEntity)
                .toList();
    }

    public UsuarioResumo buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com o ID: " + id));
        return UsuarioResumo.fromEntity(usuario);
    }

    public Usuario salvar(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Já existe um usuário com este e-mail.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setTelefone(request.telefone());
        usuario.setDataNascimento(request.dataNascimento()); // Garante que a data é atribuída aqui
        usuario.setTipoUsuario(request.tipoUsuario() != null ? request.tipoUsuario() : TipoUsuario.LEITOR);

        // Criptografa a senha
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));

        return usuarioRepository.save(usuario);
    }

    // UPDATE - Atualizar usuário existente de forma segura
    public UsuarioResumo atualizar(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com o ID: " + id));

        if (request.nome() != null) {
            usuario.setNome(request.nome());
        }
        if (request.email() != null) {
            usuario.setEmail(request.email());
        }
        if (request.telefone() != null) {
            usuario.setTelefone(request.telefone());
        }

        // Só atualiza a data de nascimento se ela for enviada no request
        if (request.dataNascimento() != null) {
            usuario.setDataNascimento(request.dataNascimento());
        }

        if (request.tipoUsuario() != null) {
            usuario.setTipoUsuario(request.tipoUsuario());
        }

        // Só atualiza a senha se o usuário preencheu uma nova
        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        }

        Usuario atualizado = usuarioRepository.save(usuario);
        return UsuarioResumo.fromEntity(atualizado);
    }

    public void deletar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado com o ID: " + id);
        }
        usuarioRepository.deleteById(id);
    }

    public UsuarioResponse registrarUsuario(RegistroRequest dto) {
        // 1. Cria a entidade
        Usuario user = new Usuario();
        user.setEmail(dto.email());
        user.setNome(dto.nome());

        // 2. Criptografa a senha
        user.setSenhaHash(passwordEncoder.encode(dto.senha()));

        // 3. Define valores obrigatórios do banco/entidade
        user.setTipoUsuario(TipoUsuario.ADMIN); // ou ADMIN
        user.setTelefone(dto.telefone() != null ? dto.telefone() : "00000000000"); // valor padrão se não vier no DTO
        user.setDataNascimento(dto.dataNascimento() != null ? dto.dataNascimento() : LocalDate.of(2000, 1, 1));
        user.setBloqueado(false);
        user.setQuantidadeEmprestada(0);

        // 4. Salva no banco
        Usuario savedUser = usuarioRepository.save(user);

        // 5. Retorna o DTO
        return new UsuarioResponse(savedUser.getId(), savedUser.getNome(), savedUser.getEmail());
    }
}